package com.crystalox.kitpvp.kit;

import com.crystalox.kitpvp.util.Format;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KitCooldownManager {

    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public boolean hasCooldown(UUID uuid, String kitId) {
        Long expiry = getExpiry(uuid, kitId);
        return expiry != null && expiry > System.currentTimeMillis();
    }

    public long getRemaining(UUID uuid, String kitId) {
        Long expiry = getExpiry(uuid, kitId);
        if (expiry == null) {
            return 0L;
        }
        return Math.max(0L, expiry - System.currentTimeMillis());
    }

    public void set(UUID uuid, String kitId, long seconds) {
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(kitId, System.currentTimeMillis() + seconds * 1000L);
    }

    public String format(UUID uuid, String kitId) {
        return Format.time(getRemaining(uuid, kitId));
    }

    private Long getExpiry(UUID uuid, String kitId) {
        Map<String, Long> byPlayer = cooldowns.get(uuid);
        if (byPlayer == null) {
            return null;
        }
        return byPlayer.get(kitId);
    }
}
