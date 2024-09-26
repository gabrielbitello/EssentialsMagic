package essentialsmagic.EssentialsMagic;

import essentialsmagic.EssentialsMagic.MagicFire.MF_MySQL;
import essentialsmagic.EssentialsMagic.MagicFire.MagicFire;
import essentialsmagic.EssentialsMagic.MagicFire.mf_commands;
import essentialsmagic.EssentialsMagic.MagicKey.MK_MySQL;
import essentialsmagic.EssentialsMagic.MagicKey.MK_commands;
import essentialsmagic.EssentialsMagic.MagicKey.MagicKey;
import essentialsmagic.EssentialsMagic.MagicKey.guis.home_menu;
import essentialsmagic.EssentialsMagic.wg.WorldGuardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class main extends JavaPlugin {
    private MF_MySQL mfMySQL;
    private MK_MySQL mkMySQL;
    private WorldGuardManager worldGuardManager;

    @Override
    public void onLoad() {
        if (getConfig().getBoolean("use_worldguard", false)) {
            worldGuardManager = new WorldGuardManager(this);
            worldGuardManager.onLoad();
        }
    }

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            FileConfiguration config = this.getConfig();
            DatabaseManager.initialize(this, config);

            mfMySQL = new MF_MySQL(this, config);
            mkMySQL = new MK_MySQL(this);

            new MK_commands(this, mkMySQL);
            new MagicKey(this);
            new home_menu(this);
            new mf_commands(this, mfMySQL);

            String message = LegacyComponentSerializer.legacySection().serialize(Component.text("[EssentialsMagic] plugin has been enabled.").color(NamedTextColor.LIGHT_PURPLE));
            Bukkit.getConsoleSender().sendMessage(message);

            if (worldGuardManager != null && worldGuardManager.isWorldGuardEnabled()) {
                getLogger().info("WorldGuard está ativo.");
            } else {
                getLogger().info("WorldGuard não está ativo.");
                worldGuardManager = null;
            }

            MagicFire magicFire = new MagicFire(this, mfMySQL, worldGuardManager);
            getServer().getPluginManager().registerEvents(magicFire, this);

            // Verificar e logar o status dos comandos
            if (getCommand("magicfire") != null) {
                getLogger().info("Comando magicfire está habilitado.");
            } else {
                getLogger().warning("Comando magicfire não está habilitado.");
            }

            if (getCommand("magickey") != null) {
                getLogger().info("Comando magickey está habilitado.");
            } else {
                getLogger().warning("Comando magickey não está habilitado.");
            }

        } catch (Exception e) {
            getLogger().severe("An error occurred during plugin enable: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        try {
            DatabaseManager.closeConnection(this);
            String message = LegacyComponentSerializer.legacySection().serialize(Component.text("[EssentialsMagic] plugin has been disabled.").color(NamedTextColor.LIGHT_PURPLE));
            Bukkit.getConsoleSender().sendMessage(message);
        } catch (Exception e) {
            getLogger().severe("An error occurred during plugin disable: " + e.getMessage());
            e.printStackTrace();
        }
    }
}