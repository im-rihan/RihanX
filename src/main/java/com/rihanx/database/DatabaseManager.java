package com.rihanx.database;

import com.rihanx.RihanX;
import com.rihanx.cache.LocationCache;
import com.rihanx.managers.ConfigManager;
import com.rihanx.models.BackLocation;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Optional SQLite persistence for back locations.
 */
public final class DatabaseManager {

    private final @NotNull RihanX plugin;
    private final @NotNull ConfigManager config;
    private Connection connection;

    public DatabaseManager(@NotNull RihanX plugin, @NotNull ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void init() {
        if (!config.databaseEnabled()) {
            plugin.getLogger().info("Database disabled; using file persistence only.");
            return;
        }
        if (!"sqlite".equalsIgnoreCase(config.getDatabaseType())) {
            plugin.getLogger().warning("Only SQLite is implemented; database disabled.");
            return;
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create data folder for database");
            }
            File dbFile = new File(plugin.getDataFolder(), config.getSqliteFile());
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS back_locations (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            player_uuid TEXT NOT NULL,
                            world TEXT NOT NULL,
                            x REAL NOT NULL,
                            y REAL NOT NULL,
                            z REAL NOT NULL,
                            yaw REAL NOT NULL,
                            pitch REAL NOT NULL,
                            timestamp INTEGER NOT NULL,
                            sort_order INTEGER NOT NULL
                        )
                        """);
            }
            plugin.getLogger().info("SQLite database initialized.");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", ex);
            closeQuietly();
        }
    }

    public void shutdown() {
        closeQuietly();
    }

    public boolean isEnabled() {
        return connection != null;
    }

    public void saveBack(@NotNull UUID playerId, @NotNull Deque<BackLocation> locations) {
        if (!isEnabled()) {
            return;
        }
        try {
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM back_locations WHERE player_uuid = ?")) {
                delete.setString(1, playerId.toString());
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO back_locations (player_uuid, world, x, y, z, yaw, pitch, timestamp, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                int order = 0;
                for (BackLocation location : locations) {
                    insert.setString(1, playerId.toString());
                    insert.setString(2, location.getWorldName());
                    insert.setDouble(3, location.getX());
                    insert.setDouble(4, location.getY());
                    insert.setDouble(5, location.getZ());
                    insert.setFloat(6, location.getYaw());
                    insert.setFloat(7, location.getPitch());
                    insert.setLong(8, location.getTimestamp());
                    insert.setInt(9, order++);
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save back location for " + playerId, ex);
        }
    }

    public void loadBacks(@NotNull LocationCache cache) {
        if (!isEnabled()) {
            return;
        }
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT player_uuid, world, x, y, z, yaw, pitch, timestamp FROM back_locations ORDER BY player_uuid, sort_order ASC");
             ResultSet rs = select.executeQuery()) {
            UUID currentPlayer = null;
            Deque<BackLocation> deque = new ArrayDeque<>();
            while (rs.next()) {
                UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                if (currentPlayer == null || !currentPlayer.equals(playerId)) {
                    if (currentPlayer != null) {
                        cache.restore(currentPlayer, deque);
                    }
                    currentPlayer = playerId;
                    deque = new ArrayDeque<>();
                }
                BackLocation location = new BackLocation(
                        playerId,
                        rs.getString("world"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch"),
                        rs.getLong("timestamp")
                );
                deque.addLast(location);
            }
            if (currentPlayer != null) {
                cache.restore(currentPlayer, deque);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load back locations", ex);
        }
    }

    private void closeQuietly() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Error closing database connection", ex);
            }
            connection = null;
        }
    }
}
