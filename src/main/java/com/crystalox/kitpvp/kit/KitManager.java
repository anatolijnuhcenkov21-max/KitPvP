package com.crystalox.kitpvp.kit;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class KitManager {

    private final JavaPlugin plugin;
    private final Map<String, Kit> kits = new LinkedHashMap<String, Kit>();

    public KitManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        kits.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("kits");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            kits.put(id.toLowerCase(), Kit.fromConfig(id, section.getConfigurationSection(id)));
        }
    }

    public Kit getKit(String id) {
        return kits.get(id.toLowerCase());
    }

    public Collection<Kit> getKits() {
        return kits.values();
    }

    public Kit getDefaultKit() {
        String defaultId = plugin.getConfig().getString("default-kit");
        if (defaultId != null) {
            Kit kit = getKit(defaultId);
            if (kit != null) {
                return kit;
            }
        }
        if (kits.isEmpty()) {
            return null;
        }
        return kits.values().iterator().next();
    }

    public boolean hasKit(String id) {
        return kits.containsKey(id.toLowerCase());
    }
}
