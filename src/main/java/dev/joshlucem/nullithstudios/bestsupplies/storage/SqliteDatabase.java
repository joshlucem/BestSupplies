package dev.joshlucem.nullithstudios.bestsupplies.storage;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.model.ChequeData;
import dev.joshlucem.nullithstudios.bestsupplies.model.PendingEntry;
import dev.joshlucem.nullithstudios.bestsupplies.model.PlayerState;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class SqliteDatabase implements Database {

    private final BestSupplies plugin;
    private Connection connection;

    public SqliteDatabase(BestSupplies plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            File dbFile = new File(dataFolder, "data.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            
            createTables();
            plugin.getLogger().info("Base de datos SQLite inicializada.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error inicializando SQLite", e);
            throw new RuntimeException(e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Player state
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_state (
                    player_uuid TEXT PRIMARY KEY,
                    streak INTEGER DEFAULT 0,
                    last_daily_date TEXT,
                    last_seen_date TEXT,
                    last_rank TEXT
                )
            """);
            
            // Daily claims
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS daily_claims (
                    player_uuid TEXT,
                    date TEXT,
                    claimed INTEGER DEFAULT 0,
                    PRIMARY KEY (player_uuid, date)
                )
            """);
            
            // Weekly claims
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS weekly_claims (
                    player_uuid TEXT,
                    week_key TEXT,
                    claimed INTEGER DEFAULT 0,
                    PRIMARY KEY (player_uuid, week_key)
                )
            """);
            
            // Food claims
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS food_claims (
                    player_uuid TEXT,
                    pack_id TEXT,
                    next_claim_at INTEGER DEFAULT 0,
                    PRIMARY KEY (player_uuid, pack_id)
                )
            """);
            
            // Cheques
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS cheques (
                    cheque_id TEXT PRIMARY KEY,
                    player_uuid TEXT,
                    week_key TEXT,
                    amount REAL,
                    redeemed INTEGER DEFAULT 0,
                    redeemed_at INTEGER DEFAULT 0
                )
            """);
            
            // Pending deliveries
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pending (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT,
                    type TEXT,
                    payload TEXT,
                    created_at INTEGER
                )
            """);
            
            // Create indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pending_player ON pending(player_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_cheques_player ON cheques(player_uuid)");
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Conexión SQLite cerrada.");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error cerrando conexión SQLite", e);
            }
        }
    }

    private Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initialize();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error verificando conexión", e);
        }
        return connection;
    }

    @Override
    public PlayerState getPlayerState(String playerUuid) {
        String sql = "SELECT streak, last_daily_date, last_seen_date, last_rank FROM player_state WHERE player_uuid = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new PlayerState(
                    playerUuid,
                    rs.getInt("streak"),
                    rs.getString("last_daily_date"),
                    rs.getString("last_seen_date"),
                    rs.getString("last_rank")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error obteniendo estado del jugador", e);
        }
        return new PlayerState(playerUuid);
    }

    @Override
    public void savePlayerState(PlayerState state) {
        String sql = """
            INSERT OR REPLACE INTO player_state (player_uuid, streak, last_daily_date, last_seen_date, last_rank)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, state.getPlayerUuid());
            ps.setInt(2, state.getStreak());
            ps.setString(3, state.getLastDailyDate());
            ps.setString(4, state.getLastSeenDate());
            ps.setString(5, state.getLastRank());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error guardando estado del jugador", e);
        }
    }

    @Override
    public boolean hasDailyClaim(String playerUuid, String date) {
        String sql = "SELECT claimed FROM daily_claims WHERE player_uuid = ? AND date = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, date);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("claimed") == 1;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error verificando claim diario", e);
        }
        return false;
    }

    @Override
    public void setDailyClaim(String playerUuid, String date, boolean claimed) {
        String sql = "INSERT OR REPLACE INTO daily_claims (player_uuid, date, claimed) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, date);
            ps.setInt(3, claimed ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error guardando claim diario", e);
        }
    }

    @Override
    public boolean hasWeeklyClaim(String playerUuid, String weekKey) {
        String sql = "SELECT claimed FROM weekly_claims WHERE player_uuid = ? AND week_key = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, weekKey);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("claimed") == 1;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error verificando claim semanal", e);
        }
        return false;
    }

    @Override
    public void setWeeklyClaim(String playerUuid, String weekKey, boolean claimed) {
        String sql = "INSERT OR REPLACE INTO weekly_claims (player_uuid, week_key, claimed) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, weekKey);
            ps.setInt(3, claimed ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error guardando claim semanal", e);
        }
    }

    @Override
    public void resetWeeklyClaim(String playerUuid, String weekKey) {
        String sql = "DELETE FROM weekly_claims WHERE player_uuid = ? AND week_key = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, weekKey);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error reseteando claim semanal", e);
        }
    }

    @Override
    public long getFoodClaimNextAt(String playerUuid, String packId) {
        String sql = "SELECT next_claim_at FROM food_claims WHERE player_uuid = ? AND pack_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, packId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("next_claim_at");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error obteniendo cooldown de comida", e);
        }
        return 0;
    }

    @Override
    public void setFoodClaimNextAt(String playerUuid, String packId, long nextClaimAt) {
        String sql = "INSERT OR REPLACE INTO food_claims (player_uuid, pack_id, next_claim_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, packId);
            ps.setLong(3, nextClaimAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error guardando cooldown de comida", e);
        }
    }

    @Override
    public void resetFoodClaim(String playerUuid, String packId) {
        String sql = "DELETE FROM food_claims WHERE player_uuid = ? AND pack_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, packId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error reseteando cooldown de comida", e);
        }
    }

    @Override
    public void resetAllFoodClaims(String playerUuid) {
        String sql = "DELETE FROM food_claims WHERE player_uuid = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error reseteando cooldowns de comida", e);
        }
    }

    @Override
    public void saveCheque(ChequeData cheque) {
        String sql = """
            INSERT OR REPLACE INTO cheques (cheque_id, player_uuid, week_key, amount, redeemed, redeemed_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, cheque.getChequeId());
            ps.setString(2, cheque.getPlayerUuid());
            ps.setString(3, cheque.getWeekKey());
            ps.setDouble(4, cheque.getAmount());
            ps.setInt(5, cheque.isRedeemed() ? 1 : 0);
            ps.setLong(6, cheque.getRedeemedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error guardando cheque", e);
        }
    }

    @Override
    public ChequeData getCheque(String chequeId) {
        String sql = "SELECT player_uuid, week_key, amount, redeemed, redeemed_at FROM cheques WHERE cheque_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, chequeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new ChequeData(
                    chequeId,
                    rs.getString("player_uuid"),
                    rs.getString("week_key"),
                    rs.getDouble("amount"),
                    rs.getInt("redeemed") == 1,
                    rs.getLong("redeemed_at")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error obteniendo cheque", e);
        }
        return null;
    }

    @Override
    public void redeemCheque(String chequeId, long redeemedAt) {
        String sql = "UPDATE cheques SET redeemed = 1, redeemed_at = ? WHERE cheque_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, redeemedAt);
            ps.setString(2, chequeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error canjeando cheque", e);
        }
    }

    @Override
    public void addPending(String playerUuid, PendingEntry.PendingType type, String payload) {
        String sql = "INSERT INTO pending (player_uuid, type, payload, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, type.name());
            ps.setString(3, payload);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error guardando entrega pendiente", e);
        }
    }

    @Override
    public List<PendingEntry> getPendingEntries(String playerUuid) {
        List<PendingEntry> entries = new ArrayList<>();
        String sql = "SELECT id, type, payload, created_at FROM pending WHERE player_uuid = ? ORDER BY created_at ASC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PendingEntry.PendingType type;
                try {
                    type = PendingEntry.PendingType.valueOf(rs.getString("type"));
                } catch (Exception e) {
                    type = PendingEntry.PendingType.ITEM;
                }
                entries.add(new PendingEntry(
                    rs.getInt("id"),
                    playerUuid,
                    type,
                    rs.getString("payload"),
                    rs.getLong("created_at")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error obteniendo entregas pendientes", e);
        }
        return entries;
    }

    @Override
    public void removePending(int id) {
        String sql = "DELETE FROM pending WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error eliminando entrega pendiente", e);
        }
    }

    @Override
    public int getPendingCount(String playerUuid) {
        String sql = "SELECT COUNT(*) FROM pending WHERE player_uuid = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error contando entregas pendientes", e);
        }
        return 0;
    }
}
