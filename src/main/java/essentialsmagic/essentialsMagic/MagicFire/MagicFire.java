package essentialsmagic.EssentialsMagic.MagicFire;

import essentialsmagic.EssentialsMagic.MagicFire.guis.tp_menu;


import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureBreakEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurniturePlaceEvent;
import io.th0rgal.oraxen.api.OraxenFurniture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MagicFire implements Listener {
    private final JavaPlugin plugin;
    private final MF_MySQL mfMySQL;
    private final tp_menu tpMenu;
    private final Map<Player, Location> pendingPortals = new HashMap<>();
    private final Map<UUID, Boolean> awaitingPortalName = new HashMap<>();
    private final Map<UUID, Location> playerLocations = new HashMap<>();
    private Location fireLocation;

    public MagicFire(JavaPlugin plugin, MF_MySQL mfMySQL) {
        this.plugin = plugin;
        this.mfMySQL = mfMySQL;
        this.tpMenu = new tp_menu(plugin, mfMySQL);
    }

    @EventHandler
    public void onFurniturePlace(OraxenFurniturePlaceEvent event) {
        List<String> portalIds = plugin.getConfig().getStringList("magicfire.portal_ids");

        event.setCancelled(true);

        if (portalIds.contains(event.getMechanic().getItemID())) {
            Player player = event.getPlayer();
            Location location = event.getBlock().getLocation();
            String portalType = event.getMechanic().getItemID(); // Capture o ID do item aqui

            if (mfMySQL.isPortalNearby(location, 2)) {
                player.sendMessage("§cNão é possível criar um portal tão próximo de outro!");
                return;
            }

            pendingPortals.put(player, location);
            awaitingPortalName.put(player.getUniqueId(), true);
            playerLocations.put(player.getUniqueId(), player.getLocation());
            player.sendMessage("§aPor favor, digite o nome do portal no chat. Você tem 30 segundos para responder.");
            new PortalNameTimeoutTask(this, player).runTaskLater(plugin, 600L);

            player.setMetadata("portalType", new FixedMetadataValue(plugin, portalType));

            // Recolocar a mobília com o yaw correto
            float yaw = player.getLocation().getYaw();
            Bukkit.getScheduler().runTask(plugin, () -> {
                OraxenFurniture.place(portalType, location, yaw, BlockFace.UP);
            });

            // Remover uma unidade do item da mão do jogador
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand != null && itemInHand.getAmount() > 1) {
                itemInHand.setAmount(itemInHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (awaitingPortalName.getOrDefault(player.getUniqueId(), false)) {
            event.setCancelled(true);
            String portalName = event.getMessage().trim();
            Location location = pendingPortals.get(player);
            String portalType = player.getMetadata("portalType").get(0).asString();

            awaitingPortalName.remove(player.getUniqueId());
            pendingPortals.remove(player);
            playerLocations.remove(player.getUniqueId());

            if (mfMySQL.verifyAndInsertPortal(player, player.getUniqueId().toString(), portalName, player.getName(), "Um portal mágico criado por " + player.getName(), "Outros", "stone", location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), 1, "", 0, portalType, player.getLocation().getYaw())) {
                player.sendMessage("§aPortal criado com sucesso!");
            } else {
                player.sendMessage("§cVocê já possui o número máximo de portais.");
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (awaitingPortalName.getOrDefault(player.getUniqueId(), false)) {
            Location initialLocation = playerLocations.get(player.getUniqueId());
            if (initialLocation != null && !initialLocation.equals(player.getLocation())) {
                awaitingPortalName.remove(player.getUniqueId());
                playerLocations.remove(player.getUniqueId());
                Location location = pendingPortals.remove(player);
                player.sendMessage("§cVocê se moveu. A criação do portal foi cancelada.");
            }
        }
    }

    @EventHandler
    public void onFurnitureBreak(OraxenFurnitureBreakEvent event) {
        List<String> portalIds = plugin.getConfig().getStringList("magicfire.portal_ids");
        String portalId = plugin.getConfig().getString("magicfire.portal_id", "chama_dos_sonhos");

        if (portalIds.contains(event.getMechanic().getItemID())) {
            Location location = event.getBlock().getLocation();
            mfMySQL.deleteNearbyPortal(location, 3);
            event.getPlayer().sendMessage("§cPortal removido da rede!");
        }
    }

    public void handlePortalCreation(Player player, Location location, String portalName, String portalType) {
        String playerUUID = player.getUniqueId().toString();
        String playerName = player.getName();
        String description = "Um portal mágico criado por " + playerName;
        String category = "Outros";
        String icon = "stone";
        String world = player.getWorld().getName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        int status = 1;
        String bannedPlayers = "";
        int visits = 0;
        float yaw = player.getLocation().getYaw(); // Capturar o yaw do jogador

        // Salvar o portal e o yaw no banco de dados
        mfMySQL.verifyAndInsertPortal(player, playerUUID, portalName, playerName, description, category, icon, world, x, y, z, status, bannedPlayers, visits, portalType, yaw);
        plugin.getLogger().info("Portal created: " + portalName + " at location: " + x + ", " + y + ", " + z + " with type: " + portalType + " and yaw: " + yaw);
    }

    @EventHandler
    public void onFurnitureInteract(OraxenFurnitureInteractEvent event) {
        try {
            List<String> portalIds = plugin.getConfig().getStringList("magicfire.portal_ids");
            boolean animationEnabled = plugin.getConfig().getBoolean("magicfire.animation", false);
            Map<String, String> portalAnimations = new HashMap<>();

            // Preencher o mapa com as animações dos portais
            for (String entry : plugin.getConfig().getStringList("magicfire.portal_ids_animation")) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    portalAnimations.put(parts[0], parts[1]);
                }
            }

            String mechanicItemId = event.getMechanic().getItemID();

            if (portalIds.contains(mechanicItemId)) {
                Player player = event.getPlayer();
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                String portalKeyId = plugin.getConfig().getString("magicfire.portal_key_id", "po_dos_sonhos");

                if (itemInHand != null && portalKeyId.equals(OraxenItems.getIdByItem(itemInHand))) {
                    if (event.getBlock() == null) {
                        player.sendMessage("§cO bloco do portal não pôde ser encontrado.");
                        return;
                    }
                    Location fireLocation = event.getBlock().getLocation();
                    float yaw = player.getLocation().getYaw(); // Capturar o yaw do jogador

                    if (animationEnabled) {
                        String portalIdAnimation = portalAnimations.get(mechanicItemId);
                        if (portalIdAnimation != null) {
                            OraxenFurniture.remove(fireLocation, null);
                            OraxenFurniture.place(portalIdAnimation, fireLocation, yaw, BlockFace.UP);

                            // Restaurar a mobília original após 4 segundos (80 ticks)
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                OraxenFurniture.remove(fireLocation, null);
                                OraxenFurniture.place(mechanicItemId, fireLocation, yaw, BlockFace.UP);
                            }, 80L);
                        } else {
                            player.sendMessage("§cAnimação não encontrada para o portal: " + mechanicItemId);
                        }
                    }

                    tpMenu.openMenu(player, fireLocation);
                } else {
                    player.sendMessage("§cVocê precisa da chave correta para interagir com este portal.");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("An error occurred while interacting with the portal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Adicione este método na classe MagicFire
    public Map<Player, Location> getPendingPortals() {
        return pendingPortals;
    }

    public static class PortalNameTimeoutTask extends BukkitRunnable {
        private final MagicFire magicFire;
        private final Player player;

        public PortalNameTimeoutTask(MagicFire magicFire, Player player) {
            this.magicFire = magicFire;
            this.player = player;
        }

        @Override
        public void run() {
            if (magicFire.awaitingPortalName.getOrDefault(player.getUniqueId(), false)) {
                magicFire.awaitingPortalName.remove(player.getUniqueId());
                magicFire.playerLocations.remove(player.getUniqueId());
                magicFire.getPendingPortals().remove(player);
                player.sendMessage("§cTempo esgotado para nomear o portal.");
            }
        }
    }
}