package com.crystalox.kitpvp.shop;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KitSelection {

    private static final Map<UUID, String> SELECTED = new ConcurrentHashMap<UUID, String>();

    private KitSelection() {
    }

    public static String get(UUID uuid) {
        return SELECTED.get(uuid);
    }

    public static void set(UUID uuid, String kitId) {
        SELECTED.put(uuid, kitId);
    }

    public static void clear(UUID uuid) {
        SELECTED.remove(uuid);
    }
}
