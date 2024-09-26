package essentialsmagic.EssentialsMagic.MagicKey.guis;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class home_menu implements Listener {
    private final JavaPlugin plugin;

    public home_menu(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Lógica para a interface gráfica
}