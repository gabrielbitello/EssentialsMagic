package essentialsmagic.EssentialsMagic.PsGod;

import essentialsmagic.EssentialsMagic.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.plugin.Plugin;

public class PG_MySQL {
    private final Plugin plugin;

    public PG_MySQL(Plugin plugin) {
        this.plugin = plugin;
    }

    // src/main/java/essentialsmagic/EssentialsMagic/PsGod/PG_MySQL.java
    public void initializeDatabase() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS EM_PsGod ("
                + "id INT NOT NULL AUTO_INCREMENT,"
                + "player_uid VARCHAR(36) NOT NULL,"
                + "player_ip VARCHAR(45) NOT NULL,"
                + "message TEXT NOT NULL,"
                + "order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (id)"
                + ");";
        try (Statement statement = DatabaseManager.getConnection().createStatement()) {
            statement.execute(createTableSQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveOrder(String playerUID, String playerIP, String message) {
        String insertOrderSQL = "INSERT INTO EM_PsGod (player_uid, player_ip, message) VALUES (?, ?, ?)";
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(insertOrderSQL)) {
            statement.setString(1, playerUID);
            statement.setString(2, playerIP);
            statement.setString(3, message);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean playerExceedsOrderLimit(String playerUID) {
        String query = "SELECT COUNT(*) AS order_count FROM EM_PsGod WHERE player_uid = ? "
                + "AND order_date > (NOW() - INTERVAL 7 DAY)";
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {
            statement.setString(1, playerUID);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("order_count") >= plugin.getConfig().getInt("max_per_player");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String> getRandomOrders(int limit) {
        List<String> orders = new ArrayList<>();
        String query = "SELECT message FROM EM_PsGod ORDER BY RAND() LIMIT ?";
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(query)) {
            statement.setInt(1, limit);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                orders.add(resultSet.getString("message"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }
}