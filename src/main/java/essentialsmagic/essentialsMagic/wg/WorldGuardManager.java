package essentialsmagic.EssentialsMagic.wg;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldGuardManager {

    private final JavaPlugin plugin;
    public static StateFlag MAGIC_FIRE_FLAG;

    public WorldGuardManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void onLoad() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag("magicfire", true);  // ALLOW by default
            registry.register(flag);
            MAGIC_FIRE_FLAG = flag; // only set our field if there was no error
            plugin.getLogger().info("Flag 'magicfire' registered successfully.");
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("magicfire");
            if (existing instanceof StateFlag) {
                MAGIC_FIRE_FLAG = (StateFlag) existing;
                plugin.getLogger().info("Flag 'magicfire' already exists, using the existing flag.");
            } else {
                plugin.getLogger().severe("Flag conflict detected and could not resolve it!");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
            }
        }
    }

    public boolean isWorldGuardEnabled() {
        return plugin.getConfig().getBoolean("use_worldguard", false);
    }
}