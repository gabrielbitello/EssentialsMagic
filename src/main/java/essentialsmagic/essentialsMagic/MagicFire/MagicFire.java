package essentialsmagic.EssentialsMagic.MagicFire;

import essentialsmagic.EssentialsMagic.MagicFire.guis.tp_menu;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureBreakEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurniturePlaceEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class MagicFire implements Listener {
    private final JavaPlugin plugin;
    private final MF_MySQL mfMySQL;
    private final tp_menu tpMenu;
    private final Map<Player, Location> pendingPortals = new HashMap<>();
    private Location fireLocation;

    public MagicFire(JavaPlugin plugin, MF_MySQL mfMySQL) {
        this.plugin = plugin;
        this.mfMySQL = mfMySQL;
        this.tpMenu = new tp_menu(plugin, mfMySQL);
    }

    @EventHandler
    public void onFurniturePlace(OraxenFurniturePlaceEvent event) {
        String portalId = plugin.getConfig().getString("magicfire.portal_id", "chama_dos_sonhos");
        if (event.getMechanic().getItemID().equals(portalId)) {
            Player player = event.getPlayer();
            Location location = event.getBlock().getLocation();
            if (mfMySQL.isPortalNearby(location, 2)) {
                player.sendMessage("§cNão é possível criar um portal tão próximo de outro!");
                event.setCancelled(true);
                return;
            }

            pendingPortals.put(player, location);
            player.sendMessage("§aPor favor, digite o nome do portal no chat. Você tem 30 segundos para responder.");
            plugin.getConfig().set("aguardando_nome_portal." + player.getUniqueId(), true);
            plugin.saveConfig();
            new PortalNameTimeoutTask(this, player).runTaskLater(plugin, 600L);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfig().getBoolean("aguardando_nome_portal." + player.getUniqueId())) {
            String portalName = event.getMessage();
            plugin.getConfig().set("aguardando_nome_portal." + player.getUniqueId(), null);
            plugin.saveConfig();
            if (mfMySQL.isPortalNameExists(portalName)) {
                player.sendMessage("§cO nome do portal já existe. Ação cancelada.");
                pendingPortals.remove(player);
                event.setCancelled(true);
                return;
            }

            Location location = pendingPortals.remove(player);
            handlePortalCreation(player, location, portalName);
            player.sendMessage("§aPortal criado com sucesso!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFurnitureBreak(OraxenFurnitureBreakEvent event) {
        String portalId = plugin.getConfig().getString("magicfire.portal_id", "chama_dos_sonhos");
        if (event.getMechanic().getItemID().equals(portalId)) {
            Location location = event.getBlock().getLocation();
            mfMySQL.deleteNearbyPortal(location, 3);
            event.getPlayer().sendMessage("§cPortal removido da rede!");
        }
    }

    public void handlePortalCreation(Player player, Location location, String portalName) {
        String playerUUID = player.getUniqueId().toString();
        String playerName = player.getName();
        String description = "Descrição do portal";
        String category = "Outros";
        String icon = "stone";
        String world = player.getWorld().getName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        int status = 1;
        String bannedPlayers = "";
        int visits = 0;
        mfMySQL.verifyAndInsertPortal(player, playerUUID, portalName, playerName, description, category, icon, world, x, y, z, status, bannedPlayers, visits);
    }

    @EventHandler
    public void onFurnitureInteract(OraxenFurnitureInteractEvent event) {
        String portalId = plugin.getConfig().getString("magicfire.portal_id", "chama_dos_sonhos");
        String portalKeyId = plugin.getConfig().getString("magicfire.portal_key_id", "po_dos_sonhos");
        if (event.getMechanic().getItemID().equals(portalId)) {
            Player player = event.getPlayer();
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand != null) {
                String itemId = OraxenItems.getIdByItem(itemInHand);
                if (portalKeyId.equals(itemId)) {
                    fireLocation = event.getBlock().getLocation();
                    tpMenu.openMenu(player); // Abre o menu inicial
                } else {
                    player.sendMessage("§cVocê precisa da chave correta para interagir com este portal.");
                }
            } else {
                player.sendMessage("§cVocê precisa da chave correta para interagir com este portal.");
            }
        }
    }

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
            if (magicFire.plugin.getConfig().getBoolean("aguardando_nome_portal." + player.getUniqueId())) {
                magicFire.plugin.getConfig().set("aguardando_nome_portal." + player.getUniqueId(), null);
                magicFire.plugin.saveConfig();
                magicFire.getPendingPortals().remove(player);
                player.sendMessage("§cTempo esgotado para nomear o portal.");
            }
        }
    }
}