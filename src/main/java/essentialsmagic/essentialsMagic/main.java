package essentialsmagic.EssentialsMagic;

import essentialsmagic.EssentialsMagic.MagicFire.MagicFire;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class main extends JavaPlugin {

    @Override
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage(String.valueOf(Component.text("[EssentialsMagic] plugin has been enabled.").color(NamedTextColor.LIGHT_PURPLE)));
        getServer().getPluginManager().registerEvents(new MagicFire(), this);
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(String.valueOf(Component.text("[EssentialsMagic] plugin has been disabled.").color(NamedTextColor.LIGHT_PURPLE)));
    }
}