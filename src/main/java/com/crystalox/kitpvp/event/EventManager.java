package com.crystalox.kitpvp.event;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EventManager {

    public enum EventType {
        DOUBLE_COINS,
        BLOOD_MOON,
        STREAK_KING
    }

    private final KitPvPPlugin plugin;
    private final Map<UUID, Integer> streakBest = new ConcurrentHashMap<>();
    private EventType current;
    private EventType last;
    private long eventEndsAt;
    private long nextEventAt;

    public EventManager(KitPvPPlugin plugin) {
        this.plugin = plugin;
        nextEventAt = System.currentTimeMillis() + plugin.getConfig().getInt("event-interval-minutes", 30) * 60000L;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 100L, 100L);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        if (current != null && now >= eventEndsAt) {
            endEvent();
        } else if (current == null && now >= nextEventAt) {
            startRandom();
        }
    }

    public boolean isActive(EventType type) {
        return current == type;
    }

    public int getKillMultiplier() {
        return current == EventType.DOUBLE_COINS ? 2 : 1;
    }

    public double getDamageMultiplier() {
        return current == EventType.BLOOD_MOON ? 1.5 : 1.0;
    }

    public void onKill(UUID uuid, int streak) {
        if (current == EventType.STREAK_KING) {
            streakBest.merge(uuid, streak, Math::max);
        }
    }

    private void startRandom() {
        List<EventType> pool = new ArrayList<>(Arrays.asList(EventType.values()));
        pool.remove(last);
        EventType pick = pool.get(new Random().nextInt(pool.size()));
        last = pick;
        current = pick;
        eventEndsAt = System.currentTimeMillis() + minutes("event-duration-minutes", 10);
        if (pick == EventType.STREAK_KING) {
            streakBest.clear();
        }
        announceStart(pick);
        plugin.getServer().getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f));
    }

    private void endEvent() {
        EventType ended = current;
        if (ended == EventType.STREAK_KING) {
            rewardStreakKing();
        }
        announceEnd(ended);
        current = null;
        nextEventAt = System.currentTimeMillis() + minutes("event-interval-minutes", 30);
    }

    private void rewardStreakKing() {
        UUID winner = null;
        int best = 0;
        for (Map.Entry<UUID, Integer> entry : streakBest.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                winner = entry.getKey();
            }
        }
        if (winner == null) {
            return;
        }
        int reward = plugin.getConfig().getInt("streak-king-reward", 200);
        Player w = Bukkit.getPlayer(winner);
        plugin.getStatsManager().addCoins(winner, reward);
        if (w != null) {
            w.sendMessage(Message.color("&6&lТы — Король арены! +" + reward + " монет"));
            w.playSound(w.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        }
        plugin.getServer().broadcastMessage(Message.color("&6&lКороль арены: &e" + (w != null ? w.getName() : "?") + " &7— серия &e" + best + "&7! +" + reward + " монет"));
    }

    private void announceStart(EventType type) {
        String key = type == EventType.DOUBLE_COINS ? "event-2x-start" : type == EventType.BLOOD_MOON ? "event-moon-start" : "event-streak-start";
        String msg = message(key);
        if (type == EventType.STREAK_KING) {
            msg = msg.replace("%reward%", String.valueOf(plugin.getConfig().getInt("streak-king-reward", 200)));
        }
        plugin.getServer().broadcastMessage(msg.replace("%time%", String.valueOf(plugin.getConfig().getInt("event-duration-minutes", 10))));
    }

    private void announceEnd(EventType type) {
        String key = type == EventType.DOUBLE_COINS ? "event-2x-end" : type == EventType.BLOOD_MOON ? "event-moon-end" : "event-streak-end";
        plugin.getServer().broadcastMessage(message(key));
    }

    private String message(String key) {
        return Message.color(plugin.getConfig().getString("messages." + key, key));
    }

    private long minutes(String key, int def) {
        return plugin.getConfig().getInt(key, def) * 60000L;
    }
}
