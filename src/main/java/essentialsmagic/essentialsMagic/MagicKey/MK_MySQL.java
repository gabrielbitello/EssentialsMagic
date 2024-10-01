package essentialsmagic.EssentialsMagic.MagicKey;

import essentialsmagic.EssentialsMagic.DatabaseManager;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
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
        if (!MagicKey.isMagicKeyEnabled(plugin)) return;

        String createTableSQL = "CREATE TABLE IF NOT EXISTS MK_Homes (" +
                "player_id VARCHAR(36) PRIMARY KEY," +
                "world VARCHAR(255) NOT NULL," +
                "x DOUBLE NOT NULL," +
                "y DOUBLE NOT NULL," +
                "z DOUBLE NOT NULL," +
                "yaw VARCHAR(255) NOT NULL," +
                "`MK-a` VARCHAR(800) NULL," +
                "`MK-b` VARCHAR(2000) NULL" +
                ");";

        try (PreparedStatement statement = connection.prepareStatement(createTableSQL)) {
            statement.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao criar a tabela homes.", e);
        }
    }

    public void setHome(UUID playerId, String world, double x, double y, double z, float yaw) {
        String query = "REPLACE INTO MK_Homes (player_id, world, x, y, z, yaw) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, world);
            statement.setDouble(3, x);
            statement.setDouble(4, y);
            statement.setDouble(5, z);
            statement.setFloat(6, yaw);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao definir a home do jogador.", e);
        }
    }

    public Location getHome(UUID playerId) {
        String query = "SELECT world, x, y, z, yaw FROM MK_Homes WHERE player_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String world = resultSet.getString("world");
                    double x = resultSet.getDouble("x");
                    double y = resultSet.getDouble("y");
                    double z = resultSet.getDouble("z");
                    float yaw = resultSet.getFloat("yaw");

                    return new Location(plugin.getServer().getWorld(world), x, y, z, yaw, 0);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao obter a home do jogador.", e);
        }
        return null;
    }

    public void savePortalKey(UUID playerId, String keyData) {
        String existingData = loadPortalKey(playerId);
        String newData = (existingData == null ? "" : existingData) + keyData + "/";

        // Verificar o tamanho da string antes de salvar
        if (newData.length() > 800) {
            plugin.getLogger().severe("Erro: Dados da chave são muito longos para salvar no banco de dados.");
            return;
        }

        String query = "UPDATE MK_Homes SET `MK-a` = ? WHERE player_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, newData);
            statement.setString(2, playerId.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao salvar a chave de portal: " + e.getMessage());
        }
    }

    public String loadPortalKey(UUID playerId) {
        String query = "SELECT `MK-a` FROM MK_Homes WHERE player_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("MK-a");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao carregar a chave de portal: " + e.getMessage());
        }
        return null;
    }
}