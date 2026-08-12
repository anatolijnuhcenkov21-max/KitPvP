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

        public double getKdr() {
            return deaths == 0 ? kills : (double) kills / deaths;
        }
    }

    private final Plugin plugin;
    private final File databaseFile;
    private final Map<UUID, StatsEntry> cache = new ConcurrentHashMap<UUID, StatsEntry>();
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
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS stats(uuid TEXT PRIMARY KEY, kills INT, deaths INT, killstreak INT)");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to open stats database: " + e.getMessage());
        }
        return connection;
    }

    public void load(final UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    StatsEntry entry = read(uuid);
                    if (entry == null) {
                        entry = new StatsEntry();
                        entry.uuid = uuid;
                    }
                    cache.put(uuid, entry);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load stats for " + uuid + ": " + e.getMessage());
                }
            }
        });
    }

    public int getKills(UUID uuid) {
        StatsEntry entry = getOrLoad(uuid);
        return entry == null ? 0 : entry.kills;
    }

    public int getDeaths(UUID uuid) {
        StatsEntry entry = getOrLoad(uuid);
        return entry == null ? 0 : entry.deaths;
    }

    public int getKillstreak(UUID uuid) {
        StatsEntry entry = getOrLoad(uuid);
        return entry == null ? 0 : entry.killstreak;
    }

    public void addKill(UUID uuid) {
        StatsEntry entry = getOrCreate(uuid);
        entry.kills++;
        entry.killstreak++;
        persistAsync(uuid);
    }

    public void addDeath(UUID uuid) {
        StatsEntry entry = getOrCreate(uuid);
        entry.deaths++;
        entry.killstreak = 0;
        persistAsync(uuid);
    }

    public void resetKillstreak(UUID uuid) {
        StatsEntry entry = cache.get(uuid);
        if (entry == null) {
            return;
        }
        entry.killstreak = 0;
        persistAsync(uuid);
    }

    public List<StatsEntry> getTop(int n) {
        List<StatsEntry> top = new ArrayList<StatsEntry>();
        try {
            Connection conn = getConnection();
            if (conn == null) {
                return top;
            }
            try (Statement statement = conn.createStatement()) {
                try (ResultSet result = statement.executeQuery("SELECT uuid, kills, deaths, killstreak FROM stats ORDER BY kills DESC LIMIT " + n)) {
                    while (result.next()) {
                        StatsEntry entry = new StatsEntry();
                        entry.uuid = UUID.fromString(result.getString("uuid"));
                        entry.kills = result.getInt("kills");
                        entry.deaths = result.getInt("deaths");
                        entry.killstreak = result.getInt("killstreak");
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
                for (StatsEntry entry : cache.values()) {
                    try {
                        write(entry);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to save stats for " + entry.uuid + ": " + e.getMessage());
                    }
                }
            }
        });
    }

    public void saveAllSync() {
        for (StatsEntry entry : cache.values()) {
            try {
                write(entry);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save stats for " + entry.uuid + ": " + e.getMessage());
            }
        }
    }

    public void save(UUID uuid) {
        StatsEntry entry = cache.get(uuid);
        if (entry == null) {
            return;
        }
        try {
            write(entry);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save stats for " + uuid + ": " + e.getMessage());
        }
    }

    private void persistAsync(final UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    StatsEntry entry = cache.get(uuid);
                    if (entry != null) {
                        write(entry);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to persist stats for " + uuid + ": " + e.getMessage());
                }
            }
        });
    }

    private StatsEntry getOrCreate(UUID uuid) {
        StatsEntry entry = cache.get(uuid);
        if (entry == null) {
            entry = new StatsEntry();
            entry.uuid = uuid;
            cache.put(uuid, entry);
        }
        return entry;
    }

    private StatsEntry getOrLoad(UUID uuid) {
        StatsEntry entry = cache.get(uuid);
        if (entry == null) {
            load(uuid);
        }
        return entry;
    }

    private StatsEntry read(UUID uuid) throws SQLException {
        Connection conn = getConnection();
        if (conn == null) {
            return null;
        }
        try (PreparedStatement statement = conn.prepareStatement("SELECT kills, deaths, killstreak FROM stats WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                StatsEntry entry = new StatsEntry();
                entry.uuid = uuid;
                entry.kills = result.getInt("kills");
                entry.deaths = result.getInt("deaths");
                entry.killstreak = result.getInt("killstreak");
                return entry;
            }
        }
    }

    private void write(StatsEntry entry) throws SQLException {
        Connection conn = getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement statement = conn.prepareStatement("INSERT INTO stats(uuid,kills,deaths,killstreak) VALUES(?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET kills=excluded.kills, deaths=excluded.deaths, killstreak=excluded.killstreak")) {
            statement.setString(1, entry.uuid.toString());
            statement.setInt(2, entry.kills);
            statement.setInt(3, entry.deaths);
            statement.setInt(4, entry.killstreak);
            statement.executeUpdate();
        }
    }
}
