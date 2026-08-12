package com.crystalox.kitpvp.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

public final class ItemParser {

    private ItemParser() {
    }

    public static ItemStack parse(ConfigurationSection section) {
        Material material = material(section);
        if (material == null) {
            return null;
        }
        ItemStack item = new ItemStack(material, section.getInt("amount", 1));
        applyMeta(item, section);
        applyEnchants(item, section);
        applyPotion(item, section);
        return item;
    }

    private static Material material(ConfigurationSection section) {
        String name = section.getString("material");
        if (name == null) {
            return null;
        }
        return Material.getMaterial(name.toUpperCase());
    }

    private static void applyMeta(ItemStack item, ConfigurationSection section) {
        ItemMeta meta = item.getItemMeta();
        String name = section.getString("name");
        if (name != null) {
            meta.setDisplayName(Message.color(name));
        }
        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            meta.setLore(colorLines(lore));
        }
        item.setItemMeta(meta);
    }

    private static List<String> colorLines(List<String> lines) {
        List<String> colored = new ArrayList<String>();
        for (String line : lines) {
            colored.add(Message.color(line));
        }
        return colored;
    }

    private static void applyEnchants(ItemStack item, ConfigurationSection section) {
        ConfigurationSection enchants = section.getConfigurationSection("enchants");
        if (enchants == null) {
            return;
        }
        for (String key : enchants.getKeys(false)) {
            Enchantment enchantment = Enchantment.getByName(key.toUpperCase());
            if (enchantment != null) {
                item.addUnsafeEnchantment(enchantment, enchants.getInt(key));
            }
        }
    }

    private static void applyPotion(ItemStack item, ConfigurationSection section) {
        if (!(item.getItemMeta() instanceof PotionMeta)) {
            return;
        }
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        applyBasePotion(meta, section);
        applyCustomEffects(meta, section);
        item.setItemMeta(meta);
    }

    private static void applyBasePotion(PotionMeta meta, ConfigurationSection section) {
        String potion = section.getString("potion");
        if (potion == null) {
            return;
        }
        try {
            PotionType type = PotionType.valueOf(potion.toUpperCase());
            meta.setBasePotionData(new PotionData(type, false, false));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static void applyCustomEffects(PotionMeta meta, ConfigurationSection section) {
        for (String entry : section.getStringList("potion-effects")) {
            applyCustomEffect(meta, entry);
        }
    }

    private static void applyCustomEffect(PotionMeta meta, String entry) {
        String[] parts = entry.split(":");
        if (parts.length != 3) {
            return;
        }
        PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
        if (type == null) {
            return;
        }
        int duration = parseInt(parts[1]) * 20;
        int amplifier = parseInt(parts[2]);
        try {
            meta.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
