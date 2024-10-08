package essentialsmagic.EssentialsMagic.MagicFire;

import essentialsmagic.EssentialsMagic.DatabaseManager;
import essentialsmagic.EssentialsMagic.MagicFire.guis.tp_menu;
import essentialsmagic.EssentialsMagic.MagicFire.MagicFire;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class MF_MySQL {
    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final Connection connection;

    public MF_MySQL(JavaPlugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        this.connection = DatabaseManager.getConnection();
        this.checkAndCreateTable();
    }

    private void checkAndCreateTable() {
        if (!MagicFire.isMagicFireEnabled(plugin)) return;

        String createTableSQL = "CREATE TABLE IF NOT EXISTS warp_network (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "player_uuid VARCHAR(36), " +
                "name VARCHAR(255), " +
                "description TEXT, " +
                "category VARCHAR(100), " +
                "icon VARCHAR(100), " +
                "world VARCHAR(100), " +
                "x DOUBLE, " +
                "y DOUBLE, " +
                "z DOUBLE, " +
                "status INT, " +
                "banned_players TEXT, " +
                "visits INT, " +
                "yaw FLOAT, " +
                "portal_type VARCHAR(100));";

        try (PreparedStatement stmt = connection.prepareStatement(createTableSQL)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not create table", e);
        }
    }

    public List<tp_menu.Portal> getPortals() {
        List<tp_menu.Portal> portals = new ArrayList<>();
        String query = "SELECT * FROM warp_network";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                tp_menu.Portal portal = new tp_menu.Portal(
                        resultSet.getString("name"),
                        resultSet.getString("world"),
                        resultSet.getDouble("x"),
                        resultSet.getDouble("y"),
                        resultSet.getDouble("z"),
                        resultSet.getInt("visits"),
                        resultSet.getString("icon"),
                        resultSet.getString("category"),
                        resultSet.getString("description"),
                        resultSet.getString("portal_type"),
                        resultSet.getString("yaw"),
                        resultSet.getString("banned_players")
                );
                portals.add(portal);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return portals;
    }

    public tp_menu.Portal getPortalByName(String name) {
        String query = "SELECT * FROM warp_network WHERE name = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new tp_menu.Portal(
                            resultSet.getString("name"),
                            resultSet.getString("world"),
                            resultSet.getDouble("x"),
                            resultSet.getDouble("y"),
                            resultSet.getDouble("z"),
                            resultSet.getInt("visits"),
                            resultSet.getString("icon"),
                            resultSet.getString("category"),
                            resultSet.getString("description"),
                            resultSet.getString("portal_type"),
                            resultSet.getString("yaw"),
                            resultSet.getString("banned_players")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void incrementVisits(String portalName) {
        String query = "UPDATE warp_network SET visits = visits + 1 WHERE name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, portalName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isPortalNearby(Location location, int radius) {
        String query = "SELECT COUNT(*) AS count FROM warp_network WHERE world = ? AND " +
                "SQRT(POW(x - ?, 2) + POW(y - ?, 2) + POW(z - ?, 2)) <= ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, location.getWorld().getName());
            pstmt.setDouble(2, location.getX());
            pstmt.setDouble(3, location.getY());
            pstmt.setDouble(4, location.getZ());
            pstmt.setInt(5, radius);
            try (ResultSet resultSet = pstmt.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Could not execute query: " + e.getMessage());
        }
        return false;
    }

    public boolean isPortalNameExists(String name) {
        String query = "SELECT COUNT(*) AS count FROM warp_network WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, name);
            try (ResultSet resultSet = pstmt.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Could not execute query: " + e.getMessage());
        }
        return false;
    }

    public boolean verifyAndInsertPortal(Player player, String playerUUID, String portalName, String playerName, String description, String category, String icon, String world, double x, double y, double z, int status, String bannedPlayers, int visits, String portalType, float yaw) {
        int maxPortals = getMaxPortals(player);
        String query = "SELECT COUNT(*) AS count FROM warp_network WHERE player_uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, playerUUID);
            try (ResultSet resultSet = pstmt.executeQuery()) {
                if (resultSet.next() && resultSet.getInt("count") < maxPortals) {
                    insertData(playerUUID, portalName, description, category, icon, world, x, y, z, status, bannedPlayers, visits, portalType, yaw);
                    return true;
                }
            }
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Could not execute query: " + e.getMessage());
        }
        return false;
    }

    public void insertData(String playerUUID, String portalName, String description, String category, String icon, String world, double x, double y, double z, int status, String bannedPlayers, int visits, String portalType, float yaw) {
        String insertSQL = "INSERT INTO warp_network (player_uuid, name, description, category, icon, world, x, y, z, status, banned_players, visits, portal_type, yaw) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            pstmt.setString(1, playerUUID);
            pstmt.setString(2, portalName);
            pstmt.setString(3, description);
            pstmt.setString(4, category);
            pstmt.setString(5, icon);
            pstmt.setString(6, world);
            pstmt.setDouble(7, x);
            pstmt.setDouble(8, y);
            pstmt.setDouble(9, z);
            pstmt.setInt(10, status);
            pstmt.setString(11, bannedPlayers);
            pstmt.setInt(12, visits);
            pstmt.setString(13, portalType);
            pstmt.setFloat(14, yaw);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not insert data", e);
        }
    }

    public void deleteNearbyPortal(Location location, int radius) {
        String query = "DELETE FROM warp_network WHERE world = ? AND " +
                "SQRT(POW(x - ?, 2) + POW(y - ?, 2) + POW(z - ?, 2)) <= ? LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, location.getWorld().getName());
            pstmt.setDouble(2, location.getX());
            pstmt.setDouble(3, location.getY());
            pstmt.setDouble(4, location.getZ());
            pstmt.setInt(5, radius);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Could not execute query: " + e.getMessage());
        }
    }

    private int getMaxPortals(Player player) {
        if (config.getBoolean("use_luckperms", true)) {
            Plugin luckPermsPlugin = Bukkit.getPluginManager().getPlugin("LuckPerms");
            if (luckPermsPlugin != null && luckPermsPlugin.isEnabled()) {
                LuckPerms luckPerms = LuckPermsProvider.get();
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    for (String role : config.getConfigurationSection("magicfire.roles").getKeys(false)) {
                        if (player.hasPermission("EssentialsMagic.MagicFire." + role)) {
                            return config.getInt("magicfire.roles." + role);
                        }
                    }
                }
            }
        }
        return config.getInt("magicfire.default");
    }

    public List<tp_menu.Portal> getPortalsByCategory(String category) {
        List<tp_menu.Portal> portals = new ArrayList<>();
        try {
            String query = "SELECT * FROM warp_network WHERE category = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, category);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String world = resultSet.getString("world");
                double x = resultSet.getDouble("x");
                double y = resultSet.getDouble("y");
                double z = resultSet.getDouble("z");
                int visits = resultSet.getInt("visits");
                String icon = resultSet.getString("icon");
                String description = resultSet.getString("description");
                String portalType = resultSet.getString("portal_type");
                String yaw = resultSet.getString("yaw");
                String banned_players = resultSet.getString("banned_players");

                tp_menu.Portal portal = new tp_menu.Portal(name, world, x, y, z, visits, icon, category, description, portalType, yaw, banned_players);
                portals.add(portal);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return portals;
    }

    public PortalInfo isPortalOwner(UUID playerUUID, String portalName) {
        String query = "SELECT yaw, world, x, y, z, banned_players FROM warp_network WHERE player_uuid = ? AND name = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerUUID.toString());
            statement.setString(2, portalName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new PortalInfo(
                            resultSet.getFloat("yaw"),
                            resultSet.getString("world"),
                            resultSet.getDouble("x"),
                            resultSet.getDouble("y"),
                            resultSet.getDouble("z"),
                            resultSet.getString("banned_players")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao verificar propriedade do portal: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void updatePortalType(UUID playerUUID, String newPortalType) {
        String query = "UPDATE warp_network SET portal_type = ? WHERE player_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, newPortalType);
            statement.setString(2, playerUUID.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao atualizar o tipo do portal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static class PortalInfo {
        private final float yaw;
        private final String world;
        private final double x;
        private final double y;
        private final double z;
        private final String banned_players;

        public PortalInfo(float yaw, String world, double x, double y, double z, String banned_players) {
            this.yaw = yaw;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.banned_players = banned_players;
        }

        public float getYaw() {
            return yaw;
        }

        public String getWorld() {
            return world;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public Location getLocation() {
            return new Location(Bukkit.getWorld(world), x, y, z);
        }

        public String getBannedPlayers() {
            return banned_players;
        }
    }

    public void updatePortalLocation(UUID playerUUID, String portalName, Location newLocation, float newYaw) {
        String query = "UPDATE warp_network SET world = ?, x = ?, y = ?, z = ?, yaw = ? WHERE player_uuid = ? AND name = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, newLocation.getWorld().getName());
            statement.setDouble(2, newLocation.getX());
            statement.setDouble(3, newLocation.getY());
            statement.setDouble(4, newLocation.getZ());
            statement.setFloat(5, newYaw);
            statement.setString(6, playerUUID.toString());
            statement.setString(7, portalName);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao atualizar a localização do portal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateBannedPlayers(String portalName, String bannedPlayers) {
        String query = "UPDATE warp_network SET banned_players = ? WHERE name = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, bannedPlayers);
            statement.setString(2, portalName);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao atualizar a lista de jogadores banidos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updatePortalIcon(UUID playerUUID, String portalName, String iconType) {
        String query = "UPDATE warp_network SET icon = ? WHERE player_uuid = ? AND name = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, iconType);
            statement.setString(2, playerUUID.toString());
            statement.setString(3, portalName);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao atualizar o ícone do portal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updatePortalCategory(UUID playerUUID, String portalName, String category) {
        String query = "UPDATE warp_network SET category = ? WHERE player_uuid = ? AND name = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, category);
            statement.setString(2, playerUUID.toString());
            statement.setString(3, portalName);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao atualizar a categoria do portal: " + e.getMessage());
            e.printStackTrace();
        }
    }
}