package essentialsmagic.EssentialsMagic.MagicKey;

import essentialsmagic.EssentialsMagic.main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List; // Importando a classe List
import java.util.ArrayList; // Importando a classe ArrayList

public class MK_commands implements CommandExecutor, TabCompleter {
    private final main plugin;
    private final MK_MySQL mkMySQL;

    public MK_commands(main plugin, MK_MySQL mkMySQL) {
        this.plugin = plugin;
        this.mkMySQL = mkMySQL;
        plugin.getCommand("magickey").setExecutor(this);
        plugin.getCommand("magickey").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Lógica do comando
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Lógica de auto-completar
        return new ArrayList<>(); // Retornando uma lista vazia por enquanto
    }
}