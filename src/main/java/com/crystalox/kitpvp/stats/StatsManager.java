package com.crystalox.kitpvp.stats;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatsManager {

    public static class StatsEntry {
        public UUID uuid;
        public int kills;
        public int deaths;
        public int killstreak;
        public int maxstreak;
        public int coins;

        public double getKdr() {
            return deaths == 0 ? kills : (double) kills / deaths;
        }
    }

    private static final int SLOT_KILLS = 0;
    private static final int SLOT_DEATHS = 1;
    private static final int SLOT_KILLSTREAK = 2;
    private static final int SLOT_MAXSTREAK = 3;
    private static final int SLOT_COINS = 4;

    private final Plugin plugin;
    private final File databaseFile;
    private final Map<UUID, int[]> cache = new ConcurrentHashMap<UUID, int[]>();
    private Connection connection;

    public StatsManager(Plugin plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "stats.db");
    }

    private synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS stats(uuid TEXT PRIMARY KEY, kills INT, deaths INT, killstreak INT, maxstreak INT, coins INT)");
                    migrate(statement, "ALTER TABLE stats ADD COLUMN maxstreak INT DEFAULT 0");
                    migrate(statement, "ALTER TABLE stats ADD COLUMN coins INT DEFAULT 0");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to open stats database: " + e.getMessage());
        }
        return connection;
    }

    private void migrate(Statement statement, String sql) {
        try {
            statement.executeUpdate(sql);
        } catch (SQLException ignored) {
        }
    }

    public void load(final UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    int[] stats = read(uuid);
                    if (stats == null) {
                        stats = new int[5];
                    }
                    cache.put(uuid, stats);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load stats for " + uuid + ": " + e.getMessage());
                }
            }
        });
    }

    public int getKills(UUID uuid) {
        return getSlot(uuid, SLOT_KILLS);
    }

    public int getDeaths(UUID uuid) {
        return getSlot(uuid, SLOT_DEATHS);
    }

    public int getKillstreak(UUID uuid) {
        return getSlot(uuid, SLOT_KILLSTREAK);
    }

    public int getMaxStreak(UUID uuid) {
        return getSlot(uuid, SLOT_MAXSTREAK);
    }

    public int getCoins(UUID uuid) {
        return getSlot(uuid, SLOT_COINS);
    }

    private int getSlot(UUID uuid, int slot) {
        int[] stats = getOrLoad(uuid);
        return stats == null ? 0 : stats[slot];
    }

    public void addKill(UUID uuid) {
        int[] stats = getOrCreate(uuid);
        stats[SLOT_KILLS]++;
        stats[SLOT_KILLSTREAK]++;
        if (stats[SLOT_KILLSTREAK] > stats[SLOT_MAXSTREAK]) {
            stats[SLOT_MAXSTREAK] = stats[SLOT_KILLSTREAK];
        }
        persistAsync(uuid);
    }

    public void addDeath(UUID uuid) {
        int[] stats = getOrCreate(uuid);
        stats[SLOT_DEATHS]++;
        stats[SLOT_KILLSTREAK] = 0;
        persistAsync(uuid);
    }

    public void resetKillstreak(UUID uuid) {
        int[] stats = cache.get(uuid);
        if (stats == null) {
            return;
        }
        stats[SLOT_KILLSTREAK] = 0;
        persistAsync(uuid);
    }

    public void addCoins(UUID uuid, int amount) {
        int[] stats = getOrCreate(uuid);
        stats[SLOT_COINS] += amount;
        persistAsync(uuid);
    }

    public boolean spendCoins(UUID uuid, int amount) {
        int[] stats = getOrCreate(uuid);
        if (stats[SLOT_COINS] < amount) {
            return false;
        }
        stats[SLOT_COINS] -= amount;
        persistAsync(uuid);
        return true;
    }

    public List<StatsEntry> getTop(int n) {
        List<StatsEntry> top = new ArrayList<StatsEntry>();
        try {
            Connection conn = getConnection();
            if (conn == null) {
                return top;
            }
            try (Statement statement = conn.createStatement()) {
                try (ResultSet result = statement.executeQuery("SELECT uuid, kills, deaths, killstreak, maxstreak, coins FROM stats ORDER BY kills DESC LIMIT " + n)) {
                    while (result.next()) {
                        StatsEntry entry = new StatsEntry();
                        entry.uuid = UUID.fromString(result.getString("uuid"));
                        entry.kills = result.getInt("kills");
                        entry.deaths = result.getInt("deaths");
                        entry.killstreak = result.getInt("killstreak");
                        entry.maxstreak = result.getInt("maxstreak");
                        entry.coins = result.getInt("coins");
                        top.add(entry);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load top stats: " + e.getMessage());
        }
        return top;
    }

    public void saveAll() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, int[]> entry : cache.entrySet()) {
                    try {
                        write(entry.getKey(), entry.getValue());
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to save stats for " + entry.getKey() + ": " + e.getMessage());
                    }
                }
            }
        });
    }

    public void saveAllSync() {
        for (Map.Entry<UUID, int[]> entry : cache.entrySet()) {
            try {
                write(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save stats for " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    public void save(UUID uuid) {
        int[] stats = cache.get(uuid);
        if (stats == null) {
            return;
        }
        try {
            write(uuid, stats);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save stats for " + uuid + ": " + e.getMessage());
        }
    }

    private void persistAsync(final UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    int[] stats = cache.get(uuid);
                    if (stats != null) {
                        write(uuid, stats);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to persist stats for " + uuid + ": " + e.getMessage());
                }
            }
        });
    }

    private int[] getOrCreate(UUID uuid) {
        int[] stats = cache.get(uuid);
        if (stats == null) {
            stats = new int[5];
            cache.put(uuid, stats);
        }
        return stats;
    }

    private int[] getOrLoad(UUID uuid) {
        int[] stats = cache.get(uuid);
        if (stats == null) {
            load(uuid);
        }
        return stats;
    }

    private int[] read(UUID uuid) throws SQLException {
        Connection conn = getConnection();
        if (conn == null) {
            return null;
        }
        try (PreparedStatement statement = conn.prepareStatement("SELECT kills, deaths, killstreak, maxstreak, coins FROM stats WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                int[] stats = new int[5];
                stats[SLOT_KILLS] = result.getInt("kills");
                stats[SLOT_DEATHS] = result.getInt("deaths");
                stats[SLOT_KILLSTREAK] = result.getInt("killstreak");
                stats[SLOT_MAXSTREAK] = result.getInt("maxstreak");
                stats[SLOT_COINS] = result.getInt("coins");
                return stats;
            }
        }
    }

    private void write(UUID uuid, int[] stats) throws SQLException {
        Connection conn = getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement statement = conn.prepareStatement("INSERT INTO stats(uuid,kills,deaths,killstreak,maxstreak,coins) VALUES(?,?,?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET kills=excluded.kills, deaths=excluded.deaths, killstreak=excluded.killstreak, maxstreak=excluded.maxstreak, coins=excluded.coins")) {
            statement.setString(1, uuid.toString());
            statement.setInt(2, stats[SLOT_KILLS]);
            statement.setInt(3, stats[SLOT_DEATHS]);
            statement.setInt(4, stats[SLOT_KILLSTREAK]);
            statement.setInt(5, stats[SLOT_MAXSTREAK]);
            statement.setInt(6, stats[SLOT_COINS]);
            statement.executeUpdate();
        }
    }
}
