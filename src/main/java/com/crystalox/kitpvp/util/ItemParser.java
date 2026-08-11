package com.crystalox.kitpvp.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
}
