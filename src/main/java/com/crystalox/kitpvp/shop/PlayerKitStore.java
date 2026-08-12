package com.crystalox.kitpvp.shop;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class PlayerKitStore {

    private final JavaPlugin plugin;
    private final File file;
    private Connection connection;

    public PlayerKitStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kits.db");
    }

    private synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS owned(uuid TEXT, kit TEXT, PRIMARY KEY(uuid, kit))");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to open kit store: " + e.getMessage());
        }
        return connection;
    }

    public boolean owns(UUID uuid, String kitId) {
        Connection conn = getConnection();
        if (conn == null) {
            return false;
        }
        try (PreparedStatement statement = conn.prepareStatement("SELECT 1 FROM owned WHERE uuid = ? AND kit = ?")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, kitId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check kit ownership: " + e.getMessage());
            return false;
        }
    }

    public void buy(final UUID uuid, final String kitId) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    Connection conn = getConnection();
                    if (conn == null) {
                        return;
                    }
                    try (PreparedStatement statement = conn.prepareStatement("INSERT OR IGNORE INTO owned(uuid, kit) VALUES(?,?)")) {
                        statement.setString(1, uuid.toString());
                        statement.setString(2, kitId);
                        statement.executeUpdate();
                    }
                } catch (SQLException e) {
                    plugin.getLogger().warning("Failed to buy kit " + kitId + ": " + e.getMessage());
                }
            }
        });
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to close kit store: " + e.getMessage());
        }
        connection = null;
    }
}
