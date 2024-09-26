package essentialsmagic.EssentialsMagic.MagicKey;

import essentialsmagic.EssentialsMagic.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class MK_MySQL {
    private final JavaPlugin plugin;
    private final Connection connection;

    public MK_MySQL(JavaPlugin plugin) {
        this.plugin = plugin;
        this.connection = DatabaseManager.getConnection();
        this.checkAndCreateTable();
    }

    private void checkAndCreateTable() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS magic_keys (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "player_uuid VARCHAR(36), " +
                "key_id VARCHAR(255), " +
                "location TEXT, " +
                "uses INT);";

        try (PreparedStatement stmt = connection.prepareStatement(createTableSQL)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not create table", e);
        }
    }

    // Métodos para gerenciar as chaves no banco de dados
}