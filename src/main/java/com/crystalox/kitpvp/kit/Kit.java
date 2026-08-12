package com.crystalox.kitpvp.kit;

import com.crystalox.kitpvp.util.ItemParser;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Kit {

    private final String id;
    private final String displayName;
    private final Material icon;
    private final List<String> description;
    private final long cooldownSeconds;
    private final String permission;
    private final double price;
    private final List<ItemStack> items;
    private final Map<String, ItemStack> armor;
    private final List<PotionEffect> effects;

    private Kit(String id, String displayName, Material icon, List<String> description,
                long cooldownSeconds, String permission, double price, List<ItemStack> items,
                Map<String, ItemStack> armor, List<PotionEffect> effects) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.cooldownSeconds = cooldownSeconds;
        this.permission = permission;
        this.price = price;
        this.items = items;
        this.armor = armor;
        this.effects = effects;
    }

    public static Kit fromConfig(String id, ConfigurationSection section) {
        String displayName = section.getString("display-name", id);
        Material icon = icon(section);
        List<String> description = section.getStringList("description");
        long cooldownSeconds = section.getLong("cooldown-seconds", 0L);
        String permission = permission(section);
        double price = section.getDouble("price", 0.0);
        List<ItemStack> items = items(section);
        Map<String, ItemStack> armor = armor(section);
        List<PotionEffect> effects = effects(section);
        return new Kit(id, displayName, icon, description, cooldownSeconds, permission, price,
                items, armor, effects);
    }

    private static Material icon(ConfigurationSection section) {
        String name = section.getString("icon");
        if (name == null) {
            return Material.STONE;
        }
        Material material = Material.getMaterial(name.toUpperCase());
        return material != null ? material : Material.STONE;
    }

    private static String permission(ConfigurationSection section) {
        String permission = section.getString("permission");
        if (permission == null || permission.isEmpty()) {
            return null;
        }
        return permission;
    }

    private static List<ItemStack> items(ConfigurationSection section) {
        List<ItemStack> items = new ArrayList<ItemStack>();
        for (Map<?, ?> m : section.getMapList("items")) {
            ItemStack item = ItemParser.parse((Map<String, Object>) m);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private static Map<String, ItemStack> armor(ConfigurationSection section) {
        Map<String, ItemStack> armor = new HashMap<String, ItemStack>();
        ConfigurationSection armorSection = section.getConfigurationSection("armor");
        if (armorSection == null) {
            return armor;
        }
        for (String key : armorSection.getKeys(false)) {
            armor.put(key, ItemParser.parse(armorSection.getConfigurationSection(key)));
        }
        return armor;
    }

    private static List<PotionEffect> effects(ConfigurationSection section) {
        List<PotionEffect> effects = new ArrayList<PotionEffect>();
        for (String line : section.getStringList("effects")) {
            PotionEffect effect = parseEffect(line);
            if (effect != null) {
                effects.add(effect);
            }
        }
        return effects;
    }

    private static PotionEffect parseEffect(String line) {
        String[] parts = line.split(":");
        if (parts.length != 3) {
            return null;
        }
        try {
            PotionEffectType type = PotionEffectType.getByName(parts[0]);
            if (type == null) {
                return null;
            }
            return new PotionEffect(type, Integer.parseInt(parts[1]) * 20,
                    Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public List<String> getDescription() {
        return description;
    }

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public String getPermission() {
        return permission;
    }

    public double getPrice() {
        return price;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public Map<String, ItemStack> getArmor() {
        return armor;
    }

    public List<PotionEffect> getEffects() {
        return effects;
    }
}
