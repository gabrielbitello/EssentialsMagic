package essentialsmagic.essentialsMagic;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import essentialsmagic.essentialsMagic.MagicFire.MagicFire;

public final class EssentialsMagic extends JavaPlugin {

    @Override
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage(Component.text("[EssentialsMagic] plugin has been enabled.").color(NamedTextColor.LIGHT_PURPLE));
        getServer().getPluginManager().registerEvents(new MagicFire(), this);
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(Component.text("[EssentialsMagic] plugin has been disabled.").color(NamedTextColor.LIGHT_PURPLE));
    }
}