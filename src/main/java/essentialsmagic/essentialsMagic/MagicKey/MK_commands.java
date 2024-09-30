package essentialsmagic.EssentialsMagic.MagicKey;

import essentialsmagic.EssentialsMagic.main;
import essentialsmagic.EssentialsMagic.wg.WorldGuardManager;
import essentialsmagic.EssentialsMagic.MagicKey.guis.home_menu;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class MK_commands implements CommandExecutor, TabCompleter {

    private final main plugin;
    private final MK_MySQL mkMySQL;
    private final LuckPerms luckPerms;
    private final Map<UUID, Long> homeCooldowns = new HashMap<>();
    private final home_menu homeMenu; // Adiciona a instância de home_menu


    public MK_commands(main plugin, MK_MySQL mkMySQL) {
        this.plugin = plugin;
        this.mkMySQL = mkMySQL;
        this.luckPerms = Bukkit.getServicesManager().load(LuckPerms.class);
        this.homeMenu = new home_menu(plugin, mkMySQL); // Inicializa a instância de home_menu
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        if (!plugin.getConfig().getBoolean("magickey.home", true)) {
            player.sendMessage("§cO comando de home está desativado.");
            return true;
        }

        if (args.length == 0) {
            // Teleportar para a home
            teleportToHome(player, playerId);
        } else if (args.length == 1 && args[0].equalsIgnoreCase("set")) {
            // Definir a home
            setHome(player, playerId);
        } else if (args.length == 1 && args[0].equalsIgnoreCase("menu")) {
            // Abrir o menu
            homeMenu.openHomeMenu(player);
        } else {
            player.sendMessage("§cUso incorreto do comando. Use /home para teleportar, /home set para definir a home ou /home menu para abrir o menu.");
        }

        return true;
    }

    private void teleportToHome(Player player, UUID playerId) {
        if (luckPerms != null) {
            User user = luckPerms.getUserManager().getUser(playerId);
            if (user != null && !user.getNodes().contains(Node.builder("EssentialsMagic.MagicKey.home").build())) {
                player.sendMessage("§cVocê não tem permissão para usar este comando.");
                return;
            }
        }

        long cooldown = plugin.getConfig().getInt("magickey.home_cooldown", 5) * 1000L;
        long lastUsed = homeCooldowns.getOrDefault(playerId, 0L);
        long timeSinceLastUse = System.currentTimeMillis() - lastUsed;

        if (timeSinceLastUse < cooldown && !player.hasPermission("EssentialsMagic.MagicKey.home.byPass")) {
            player.sendMessage("§cVocê deve esperar antes de usar este comando novamente.");
            return;
        }

        if (!canTeleportHome(player)) {
            player.sendMessage("§cVocê não pode teleportar para este mundo.");
            return;
        }

        Location homeLocation = mkMySQL.getHome(playerId);
        if (homeLocation != null) {
            String worldName = homeLocation.getWorld().getName();
            List<String> teleportBlacklist = plugin.getConfig().getStringList("magickey.world_teleport_blacklist");
            if (teleportBlacklist.contains(worldName) && !player.hasPermission("EssentialsMagic.MagicKey.teleport.byPass")) {
                player.sendMessage("§cVocê não pode teleportar para este mundo.");
                return;
            }

            player.teleport(homeLocation);
            player.sendMessage("§aVocê foi teleportado para sua home.");
            homeCooldowns.put(playerId, System.currentTimeMillis());
        } else {
            player.sendMessage("§cVocê ainda não definiu uma home.");
        }
    }

    private void setHome(Player player, UUID playerId) {
        if (luckPerms != null) {
            User user = luckPerms.getUserManager().getUser(playerId);
            if (user != null && !user.getNodes().contains(Node.builder("EssentialsMagic.MagicKey.home").build())) {
                player.sendMessage("§cVocê não tem permissão para definir uma home.");
                return;
            }
        }

        if (!canCreateHome(player)) {
            player.sendMessage("§cVocê não pode definir uma home neste mundo.");
            return;
        }

        Location location = player.getLocation();
        String world = location.getWorld().getName();
        List<String> createBlacklist = plugin.getConfig().getStringList("magickey.world_create_blacklist");
        if (createBlacklist.contains(world) && !player.hasPermission("EssentialsMagic.MagicKey.Create.byPass")) {
            player.sendMessage("§cVocê não pode definir uma home neste mundo.");
            return;
        }

        double x = Math.round(location.getX());
        double y = Math.round(location.getY());
        double z = Math.round(location.getZ());
        float yaw = Math.round(location.getYaw());

        mkMySQL.setHome(playerId, world, x, y, z, yaw);
        player.sendMessage("§aSua home foi definida com sucesso.");
    }

    private boolean canCreateHome(Player player) {
        return WorldGuardManager.isRegionFlagAllowed(player, WorldGuardManager.HOME_CREATE_FLAG);
    }

    private boolean canTeleportHome(Player player) {
        return WorldGuardManager.isRegionFlagAllowed(player, WorldGuardManager.HOME_TELEPORT_FLAG);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("set", "menu");
        }
        return new ArrayList<>();
    }
}