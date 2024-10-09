package essentialsmagic.EssentialsMagic.PsGod;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import essentialsmagic.EssentialsMagic.wg.WorldGuardManager;
import essentialsmagic.EssentialsMagic.Utilities;

import java.util.List;

// -=-=-=-=-=-=-=-=-=-=-=-=-
//   Não funciona re fazer
// -=-=-=-=-=-=-=-=-=-=-=-=-


public class PG_commands extends JavaPlugin implements CommandExecutor {

    private PG_MySQL database;
    private String prefix;
    private String broadcastMessage;
    private String playerMessage;
    private String errorMessage;
    private String regionMessage;
    private String noPermissionMessage;
    private String adminMessage;

    @Override
    public void onEnable() {
        this.database = new PG_MySQL(this);
        this.database.initializeDatabase();
        this.prefix = Utilities.colorize(getConfig().getString("psgod.prefix"));
        this.broadcastMessage = Utilities.colorize(getConfig().getString("psgod.mensages.broadcast"));
        this.playerMessage = Utilities.colorize(getConfig().getString("psgod.mensages.player"));
        this.errorMessage = Utilities.colorize(getConfig().getString("psgod.mensages.error"));
        this.regionMessage = Utilities.colorize(getConfig().getString("psgod.mensages.region"));
        this.noPermissionMessage = Utilities.colorize(getConfig().getString("psgod.mensages.not_permission"));
        this.adminMessage = Utilities.colorize(getConfig().getString("psgod.mensages.admin"));
        getCommand("psgod").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        Player player = (Player) sender;
        String playerIP = player.getAddress().getAddress().toString();
        String playerUID = player.getUniqueId().toString();

        if (cmd.getName().equalsIgnoreCase("psgod")) {
            if (!player.hasPermission("EssentialsMagic.PsGod")) {
                player.sendMessage(prefix + noPermissionMessage);
                return true;
            }

            if (args.length == 0) {
                player.sendMessage(prefix + ChatColor.RED + "Por favor, forneça um pedido.");
                return true;
            }

            if (getConfig().getBoolean("psgod.use_region")) {
                if (!WorldGuardManager.isRegionFlagAllowed(player, WorldGuardManager.PSGOD_FLAG)) {
                    player.sendMessage(prefix + regionMessage);
                    return true;
                }
            }

            if (database.playerExceedsOrderLimit(playerUID)) {
                player.sendMessage(errorMessage);
                return true;
            }

            String message = String.join(" ", args);
            database.saveOrder(playerUID, playerIP, message);
            Bukkit.broadcastMessage(broadcastMessage.replace("{player}", player.getName()));
            player.sendMessage(playerMessage);
            return true;
        }

        if (args[0].equalsIgnoreCase("random")) {
            if (!player.hasPermission("EssentialsMagic.PsGod.Read")) {
                player.sendMessage(prefix + noPermissionMessage);
                return true;
            }

            int numberOfUsers = 1;
            if (args.length > 1) {
                try {
                    numberOfUsers = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(prefix + "Número inválido.");
                    return true;
                }
            }

            List<String> orders = database.getRandomOrders(numberOfUsers);
            if (orders.isEmpty()) {
                player.sendMessage(prefix + "Nenhum pedido válido encontrado.");
                return true;
            }

            for (String order : orders) {
                player.sendMessage(adminMessage.replace("{order}", order));
            }
        }

        return true;
    }
}