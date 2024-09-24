package essentialsmagic.EssentialsMagic;

import essentialsmagic.EssentialsMagic.MagicFire.MF_MySQL;
import essentialsmagic.EssentialsMagic.MagicFire.MagicFire;
import essentialsmagic.EssentialsMagic.MagicFire.mf_commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class main extends JavaPlugin {
    private MF_MySQL mfMySQL;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            FileConfiguration config = this.getConfig();
            mfMySQL = new MF_MySQL(this, config);
            new mf_commands(this, mfMySQL);
            String message = LegacyComponentSerializer.legacySection().serialize(Component.text("[EssentialsMagic] plugin has been enabled.").color(NamedTextColor.LIGHT_PURPLE));
            Bukkit.getConsoleSender().sendMessage(message);
            getServer().getPluginManager().registerEvents(new MagicFire(this, mfMySQL), this);
        } catch (Exception e) {
            getLogger().severe("An error occurred during plugin enable: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        try {
            if (mfMySQL != null) {
                mfMySQL.closeConnection();
            }
            String message = LegacyComponentSerializer.legacySection().serialize(Component.text("[EssentialsMagic] plugin has been disabled.").color(NamedTextColor.LIGHT_PURPLE));
            Bukkit.getConsoleSender().sendMessage(message);
        } catch (Exception e) {
            getLogger().severe("An error occurred during plugin disable: " + e.getMessage());
            e.printStackTrace();
        }
    }
}