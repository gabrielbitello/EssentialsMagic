package essentialsmagic.EssentialsMagic.wg;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldGuardManager {

    private final JavaPlugin plugin;
    public static StateFlag MAGIC_FIRE_FLAG;
    public static StateFlag MAGIC_KEY_CREATE_FLAG;
    public static StateFlag MAGIC_KEY_USE_FLAG;
    public static StateFlag HOME_CREATE_FLAG;
    public static StateFlag HOME_TELEPORT_FLAG;
    public static StateFlag PSGOD_FLAG;

    public WorldGuardManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void onLoad() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag magicFireFlag = new StateFlag("magicfire", true);  // ALLOW by default
            registry.register(magicFireFlag);
            MAGIC_FIRE_FLAG = magicFireFlag; // only set our field if there was no error
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

        try {
            StateFlag magicKeyCreateFlag = new StateFlag("magickey-create", true);  // ALLOW by default
            registry.register(magicKeyCreateFlag);
            MAGIC_KEY_CREATE_FLAG = magicKeyCreateFlag;
            plugin.getLogger().info("Flag 'magickey-create' registered successfully.");
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("magickey-create");
            if (existing instanceof StateFlag) {
                MAGIC_KEY_CREATE_FLAG = (StateFlag) existing;
                plugin.getLogger().info("Flag 'magickey-create' already exists, using the existing flag.");
            } else {
                plugin.getLogger().severe("Flag conflict detected and could not resolve it!");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
            }
        }

        try {
            StateFlag magicKeyUseFlag = new StateFlag("magickey-use", true);  // ALLOW by default
            registry.register(magicKeyUseFlag);
            MAGIC_KEY_USE_FLAG = magicKeyUseFlag;
            plugin.getLogger().info("Flag 'magickey-use' registered successfully.");
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("magickey-use");
            if (existing instanceof StateFlag) {
                MAGIC_KEY_USE_FLAG = (StateFlag) existing;
                plugin.getLogger().info("Flag 'magickey-use' already exists, using the existing flag.");
            } else {
                plugin.getLogger().severe("Flag conflict detected and could not resolve it!");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
            }
        }

        try {
            StateFlag homeCreateFlag = new StateFlag("home-create", true);  // ALLOW by default
            registry.register(homeCreateFlag);
            HOME_CREATE_FLAG = homeCreateFlag;
            plugin.getLogger().info("Flag 'home-create' registered successfully.");
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("home-create");
            if (existing instanceof StateFlag) {
                HOME_CREATE_FLAG = (StateFlag) existing;
                plugin.getLogger().info("Flag 'home-create' already exists, using the existing flag.");
            } else {
                plugin.getLogger().severe("Flag conflict detected and could not resolve it!");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
            }
        }

        try {
            StateFlag homeTeleportFlag = new StateFlag("home-teleport", true);  // ALLOW by default
            registry.register(homeTeleportFlag);
            HOME_TELEPORT_FLAG = homeTeleportFlag;
            plugin.getLogger().info("Flag 'home-teleport' registered successfully.");
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("home-teleport");
            if (existing instanceof StateFlag) {
                HOME_TELEPORT_FLAG = (StateFlag) existing;
                plugin.getLogger().info("Flag 'home-teleport' already exists, using the existing flag.");
            } else {
                plugin.getLogger().severe("Flag conflict detected and could not resolve it!");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
            }
        }

        try {
            StateFlag PsGod = new StateFlag("PsGod", false);  // ALLOW by default
            registry.register(PsGod);
            PSGOD_FLAG = PsGod;
            plugin.getLogger().info("Flag 'PsGod' registered successfully.");
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("PsGod");
            if (existing instanceof StateFlag) {
                PSGOD_FLAG = (StateFlag) existing;
                plugin.getLogger().info("Flag 'PsGod' already exists, using the existing flag.");
            } else {
                plugin.getLogger().severe("Flag conflict detected and could not resolve it!");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
            }
        }
    }

    public boolean isWorldGuardEnabled() {
        return plugin.getConfig().getBoolean("use_worldguard", false);
    }

    public static boolean isRegionFlagAllowed(Player player, StateFlag flag) {
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
        return set.testState(null, flag);
    }
}