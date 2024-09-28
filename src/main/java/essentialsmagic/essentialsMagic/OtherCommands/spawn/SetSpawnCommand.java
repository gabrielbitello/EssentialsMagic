package essentialsmagic.EssentialsMagic.OtherCommands.spawn;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SetSpawnCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public SetSpawnCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando só pode ser executado por jogadores.");
            return true;
        }

        Player player = (Player) sender;

        if (!config.getBoolean("tp_commands.spawn", true)) {
            player.sendMessage("O comando de spawn está desativado.");
            return true;
        }

        if (config.getBoolean("use_luckperms", false)) {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null || !user.getNodes().contains(Node.builder("EssentialsMagic.Spawn.byPass").build())) {
                if (!player.isOp()) {
                    long cooldownTime = config.getInt("tp_commands.spawn_cooldown", 5) * 1000L;
                    long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
                    long currentTime = System.currentTimeMillis();

                    if (currentTime - lastUsed < cooldownTime) {
                        player.sendMessage("Você deve esperar antes de usar este comando novamente.");
                        return true;
                    }

                    cooldowns.put(player.getUniqueId(), currentTime);
                }
            }
        }

        // Coletar o mundo e as coordenadas do jogador
        World world = player.getWorld();
        Location location = player.getLocation();
        double x = Math.round(location.getX());
        double y = Math.round(location.getY());
        double z = Math.round(location.getZ());
        float yaw = Math.round(location.getYaw());
        float pitch = Math.round(location.getPitch());

        // Salvar as informações no arquivo de configuração
        config.set("tp_commands.spawn_cords.world", world.getName());
        config.set("tp_commands.spawn_cords.x", x);
        config.set("tp_commands.spawn_cords.y", y);
        config.set("tp_commands.spawn_cords.z", z);
        config.set("tp_commands.spawn_cords.yaw", yaw);
        config.set("tp_commands.spawn_cords.pitch", pitch);
        plugin.saveConfig();

        player.sendMessage("Localização de spawn definida para: " + world.getName() + " " + x + " " + y + " " + z + " " + yaw + " " + pitch);
        return true;
    }
}