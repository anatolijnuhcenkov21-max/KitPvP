package com.crystalox.kitpvp.quest;

import com.crystalox.kitpvp.KitPvPPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class QuestManager {

    private static class QuestRow {
        final int progress;
        final String periodKey;

        QuestRow(int progress, String periodKey) {
            this.progress = progress;
            this.periodKey = periodKey;
        }
    }

    private final KitPvPPlugin plugin;
    private final File file;
    private final List<Quest> daily = new ArrayList<Quest>();
    private final List<Quest> weekly = new ArrayList<Quest>();
    private Connection connection;

    public QuestManager(KitPvPPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kits.db");
        loadQuests();
    }

    private void loadQuests() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("quests");
        if (section == null) {
            return;
        }
        daily.addAll(loadPeriod(section.getConfigurationSection("daily")));
        weekly.addAll(loadPeriod(section.getConfigurationSection("weekly")));
    }

    private List<Quest> loadPeriod(ConfigurationSection section) {
        List<Quest> quests = new ArrayList<Quest>();
        if (section == null) {
            return quests;
        }
        for (String id : section.getKeys(false)) {
            quests.add(Quest.fromConfig(id, section.getConfigurationSection(id)));
        }
        return quests;
    }

    public String currentKey(String period) {
        String pattern = "WEEKLY".equals(period) ? "yyyy-ww" : "yyyy-MM-dd";
        return new SimpleDateFormat(pattern).format(new Date());
    }

    public int getProgress(UUID uuid, Quest quest) {
        QuestRow row = read(uuid, quest.getId());
        if (row == null) {
            return 0;
        }
        String key = currentKey(quest.getPeriod());
        if (!key.equals(row.periodKey)) {
            resetRow(uuid, quest.getId(), key);
            return 0;
        }
        return row.progress;
    }

    public void addProgress(UUID uuid, Quest quest, int amount) {
        int progress = getProgress(uuid, quest) + amount;
        write(uuid, quest.getId(), progress, currentKey(quest.getPeriod()));
    }

    public void onKill(UUID uuid, String activeKit) {
        for (Quest quest : daily) {
            bumpKill(uuid, quest, activeKit);
        }
        for (Quest quest : weekly) {
            bumpKill(uuid, quest, activeKit);
        }
    }

    private void bumpKill(UUID uuid, Quest quest, String activeKit) {
        if (quest.getId().startsWith("kitkill")) {
            if (quest.getKit() != null && quest.getKit().equals(activeKit)) {
                addProgress(uuid, quest, 1);
            }
        } else if (quest.getId().startsWith("kill")) {
            addProgress(uuid, quest, 1);
        }
    }

    public void onCrate(UUID uuid) {
        bump(uuid, "crate");
    }

    private void bump(UUID uuid, String prefix) {
        for (Quest quest : daily) {
            bumpQuest(uuid, quest, prefix);
        }
        for (Quest quest : weekly) {
            bumpQuest(uuid, quest, prefix);
        }
    }

    private void bumpQuest(UUID uuid, Quest quest, String prefix) {
        if (quest.getId().startsWith(prefix)) {
            addProgress(uuid, quest, 1);
        }
    }

    public boolean claim(UUID uuid, Quest quest) {
        if (getProgress(uuid, quest) < quest.getTarget()) {
            return false;
        }
        reset(uuid, quest.getId());
        return true;
    }

    public List<Quest> getDaily() {
        return daily;
    }

    public List<Quest> getWeekly() {
        return weekly;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to close quest database: " + e.getMessage());
        }
        connection = null;
    }

    private synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS quests(uuid TEXT, quest TEXT, progress INT, period_key TEXT, PRIMARY KEY(uuid, quest))");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to open quest database: " + e.getMessage());
        }
        return connection;
    }

    private QuestRow read(UUID uuid, String questId) {
        Connection conn = getConnection();
        if (conn == null) {
            return null;
        }
        try (PreparedStatement statement = conn.prepareStatement("SELECT progress, period_key FROM quests WHERE uuid = ? AND quest = ?")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, questId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new QuestRow(result.getInt("progress"), result.getString("period_key"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load quest row: " + e.getMessage());
            return null;
        }
    }

    private void write(UUID uuid, String questId, int progress, String key) {
        Connection conn = getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement statement = conn.prepareStatement("INSERT INTO quests(uuid,quest,progress,period_key) VALUES(?,?,?,?) ON CONFLICT(uuid,quest) DO UPDATE SET progress=excluded.progress, period_key=excluded.period_key")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, questId);
            statement.setInt(3, progress);
            statement.setString(4, key);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save quest progress: " + e.getMessage());
        }
    }

    private void resetRow(UUID uuid, String questId, String key) {
        Connection conn = getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement statement = conn.prepareStatement("UPDATE quests SET progress = 0, period_key = ? WHERE uuid = ? AND quest = ?")) {
            statement.setString(1, key);
            statement.setString(2, uuid.toString());
            statement.setString(3, questId);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to reset quest row: " + e.getMessage());
        }
    }

    private void reset(UUID uuid, String questId) {
        Connection conn = getConnection();
        if (conn == null) {
            return;
        }
        try (PreparedStatement statement = conn.prepareStatement("UPDATE quests SET progress = 0 WHERE uuid = ? AND quest = ?")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, questId);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to reset quest: " + e.getMessage());
        }
    }
}
