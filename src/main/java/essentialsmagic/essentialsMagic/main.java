package essentialsmagic.EssentialsMagic;

import essentialsmagic.EssentialsMagic.MagicFire.MF_MySQL;
import essentialsmagic.EssentialsMagic.MagicFire.MagicFire;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class main extends JavaPlugin {
    private MF_MySQL mfMySQL;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        mfMySQL = new MF_MySQL(this);
        String message = LegacyComponentSerializer.legacySection().serialize(Component.text("[EssentialsMagic] plugin has been enabled.").color(NamedTextColor.LIGHT_PURPLE));
        Bukkit.getConsoleSender().sendMessage(message);
        getServer().getPluginManager().registerEvents(new MagicFire(this, mfMySQL), this);
    }

    @Override
    public void onDisable() {
        if (mfMySQL != null) {
            mfMySQL.closeConnection();
        }
        String message = LegacyComponentSerializer.legacySection().serialize(Component.text("[EssentialsMagic] plugin has been disabled.").color(NamedTextColor.LIGHT_PURPLE));
        Bukkit.getConsoleSender().sendMessage(message);
    }
}