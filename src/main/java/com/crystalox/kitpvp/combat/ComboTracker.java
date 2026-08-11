package com.crystalox.kitpvp.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ComboTracker {

    private final long windowMs;
    private final Map<UUID, ComboEntry> combos = new ConcurrentHashMap<UUID, ComboEntry>();

    public ComboTracker(long windowMs) {
        this.windowMs = windowMs;
    }

    public void hit(UUID uuid, long now) {
        combos.compute(uuid, (key, existing) -> {
            ComboEntry entry = existing;
            if (entry == null || now - entry.lastHit > windowMs) {
                entry = new ComboEntry();
            }
            entry.combo++;
            entry.lastHit = now;
            return entry;
        });
    }

    public int getCombo(UUID uuid) {
        ComboEntry entry = combos.get(uuid);
        return entry == null ? 0 : entry.combo;
    }

    public void reset(UUID uuid) {
        combos.remove(uuid);
    }

    private static class ComboEntry {
        int combo;
        long lastHit;
    }
}
