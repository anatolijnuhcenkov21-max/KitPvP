package com.crystalox.kitpvp.scoreboard;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.stats.StatsManager;
import com.crystalox.kitpvp.stats.StatsManager.StatsEntry;
import com.crystalox.kitpvp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;
import java.util.UUID;

public class StatsScoreboard {

    private final KitPvPPlugin plugin;
    private BukkitTask task;

    public StatsScoreboard(KitPvPPlugin plugin) {
        this.plugin = plugin;
    }

    public StatsScoreboard start() {
        if (task == null) {
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::refreshAll, 20L, 20L);
        }
        return this;
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    private void refresh(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("stats");
        if (objective == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            objective = scoreboard.registerNewObjective("stats", "dummy", title());
        }
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(title());
        applyStatsLines(objective, player);
        applyTopLine(objective);
        player.setScoreboard(scoreboard);
    }

    private void applyStatsLines(Objective objective, Player player) {
        UUID uuid = player.getUniqueId();
        StatsManager stats = plugin.getStatsManager();
        objective.getScore(Message.color(" ")).setScore(8);
        objective.getScore(Message.color("&7Убийства: &f" + stats.getKills(uuid))).setScore(7);
        objective.getScore(Message.color("&7Смерти: &f" + stats.getDeaths(uuid))).setScore(6);
        objective.getScore(Message.color("&7Серия убийств: &f" + stats.getKillstreak(uuid))).setScore(5);
        objective.getScore(Message.color(" ")).setScore(4);
    }

    private void applyTopLine(Objective objective) {
        List<StatsEntry> top = plugin.getStatsManager().getTop(1);
        if (top.isEmpty()) {
            return;
        }
        StatsEntry entry = top.get(0);
        String name = Bukkit.getOfflinePlayer(entry.uuid).getName();
        if (name == null) {
            return;
        }
        objective.getScore(Message.color("&7Топ: &f" + name + " - " + entry.kills)).setScore(3);
    }

    private String title() {
        return Message.color(plugin.getConfig().getString("scoreboard-title"));
    }
}
