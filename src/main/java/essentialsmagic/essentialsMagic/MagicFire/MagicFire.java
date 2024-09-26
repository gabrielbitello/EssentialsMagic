package essentialsmagic.EssentialsMagic.MagicFire;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import essentialsmagic.EssentialsMagic.MagicFire.guis.tp_menu;
import essentialsmagic.EssentialsMagic.wg.WorldGuardManager;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureBreakEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurniturePlaceEvent;
import io.th0rgal.oraxen.api.OraxenFurniture;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
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
    private final WorldGuardManager worldGuardManager;
    private final Map<Player, Location> pendingPortals = new HashMap<>();
    private final Map<UUID, Boolean> awaitingPortalName = new HashMap<>();
    private final Map<UUID, Location> playerLocations = new HashMap<>();

    public MagicFire(JavaPlugin plugin, MF_MySQL mfMySQL, WorldGuardManager worldGuardManager) {
        this.plugin = plugin;
        this.mfMySQL = mfMySQL;
        this.tpMenu = new tp_menu(plugin, mfMySQL);
        this.worldGuardManager = worldGuardManager;
    }

    @EventHandler
    public void onFurniturePlace(OraxenFurniturePlaceEvent event) {
        List<String> portalIds = plugin.getConfig().getStringList("magicfire.portal_ids");
        event.setCancelled(true); // Cancelar a colocação padrão

        String itemId = event.getMechanic().getItemID();
        if (portalIds.contains(itemId)) {
            Player player = event.getPlayer();
            Location location = event.getBlock().getLocation();
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            ApplicableRegionSet set = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().getApplicableRegions(localPlayer.getLocation());

            if (!set.testState(localPlayer, WorldGuardManager.MAGIC_FIRE_FLAG) && !hasPermission(player)) {
                player.sendMessage("§cVocê não tem permissão para colocar um portal aqui.");
                return;
            }

            if (mfMySQL.isPortalNearby(location, 2)) {
                player.sendMessage("§cNão é possível criar um portal tão próximo de outro!");
                return;
            }

            pendingPortals.put(player, location);
            awaitingPortalName.put(player.getUniqueId(), true);
            playerLocations.put(player.getUniqueId(), player.getLocation());
            player.sendMessage("§aPor favor, digite o nome do portal no chat. Você tem 30 segundos para responder.");
            new PortalNameTimeoutTask(this, player).runTaskLater(plugin, 600L);

            // Recolocar a mobília com o yaw correto
            placeFurniture(itemId, location, player);

            // Remover uma unidade do item da mão do jogador
            updatePlayerInventory(player);
        }
    }

    private boolean hasPermission(Player player) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        if (luckPerms == null) {
            return player.isOp();
        } else {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            return user != null && user.getCachedData().getPermissionData().checkPermission("EssentialsMagic.MagicFire.ByPass").asBoolean();
        }
    }

    private void placeFurniture(String itemId, Location location, Player player) {
        float yaw = player.getLocation().getYaw();
        Bukkit.getScheduler().runTask(plugin, () -> {
            OraxenFurniture.place(itemId, location, yaw, BlockFace.UP);
        });
    }

    private void updatePlayerInventory(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand != null) {
            if (itemInHand.getAmount() > 1) {
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
                pendingPortals.remove(player);
                player.sendMessage("§cVocê se moveu. A criação do portal foi cancelada.");
            }
        }
    }

    @EventHandler
    public void onFurnitureBreak(OraxenFurnitureBreakEvent event) {
        List<String> portalIds = plugin.getConfig().getStringList("magicfire.portal_ids");
        if (portalIds.contains(event.getMechanic().getItemID())) {
            Location location = event.getBlock().getLocation();
            mfMySQL.deleteNearbyPortal(location, 3);
            event.getPlayer().sendMessage("§cPortal removido da rede!");
        }
    }

    @EventHandler
    public void onFurnitureInteract(OraxenFurnitureInteractEvent event) {
        Player player = event.getPlayer();
        String mechanicItemId = event.getMechanic().getItemID();
        List<String> portalIds = plugin.getConfig().getStringList("magicfire.portal_ids");

        if (portalIds.contains(mechanicItemId)) {
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            String portalKeyId = plugin.getConfig().getString("magicfire.portal_key_id", "po_dos_sonhos");

            if (itemInHand != null && portalKeyId.equals(OraxenItems.getIdByItem(itemInHand))) {
                Location fireLocation = event.getBlock().getLocation();
                tpMenu.openMenu(player, fireLocation);
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
            if (magicFire.awaitingPortalName.getOrDefault(player.getUniqueId(), false)) {
                magicFire.awaitingPortalName.remove(player.getUniqueId());
                magicFire.playerLocations.remove(player.getUniqueId());
                magicFire.getPendingPortals().remove(player);
                player.sendMessage("§cTempo esgotado para nomear o portal.");
            }
        }
    }
}
