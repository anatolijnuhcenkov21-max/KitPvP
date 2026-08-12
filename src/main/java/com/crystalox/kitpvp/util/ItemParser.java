package com.crystalox.kitpvp.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ItemParser {

    private ItemParser() {
    }

    public static ItemStack parse(ConfigurationSection s) {
        return parse(s.getValues(false));
    }

    public static ItemStack parse(Map<String, Object> m) {
        Material material = material(m);
        int amount = num(m.get("amount"), 1);
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        applyName(meta, m);
        applyLore(meta, m);
        item.setItemMeta(meta);
        applyEnchants(item, m);
        applyGlow(item, m);
        applyPotion(item, m);
        return item;
    }

    private static Material material(Map<String, Object> m) {
        String name = str(m.get("material"));
        if (name == null) {
            return Material.STONE;
        }
        Material material = Material.getMaterial(name.toUpperCase());
        return material != null ? material : Material.STONE;
    }

    private static void applyName(ItemMeta meta, Map<String, Object> m) {
        String name = str(m.get("name"));
        if (name != null) {
            meta.setDisplayName(Message.color(name));
        }
    }

    private static void applyLore(ItemMeta meta, Map<String, Object> m) {
        Object raw = m.get("lore");
        if (!(raw instanceof List)) {
            return;
        }
        List<String> lore = new ArrayList<String>();
        for (Object line : (List<?>) raw) {
            String text = str(line);
            if (text != null) {
                lore.add(Message.color(text));
            }
        }
        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }
    }

    private static void applyEnchants(ItemStack item, Map<String, Object> m) {
        Object raw = m.get("enchants");
        if (!(raw instanceof Map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
            Enchantment enchantment = Enchantment.getByName(entry.getKey().toString().toUpperCase());
            if (enchantment != null) {
                item.addUnsafeEnchantment(enchantment, num(entry.getValue(), 1));
            }
        }
    }

    private static void applyGlow(ItemStack item, Map<String, Object> m) {
        if (!bool(m.get("glow"))) {
            return;
        }
        item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    private static void applyPotion(ItemStack item, Map<String, Object> m) {
        if (!(item.getItemMeta() instanceof PotionMeta)) {
            return;
        }
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        applyBasePotion(meta, m);
        applyCustomEffects(meta, m);
        item.setItemMeta(meta);
    }

    private static void applyBasePotion(PotionMeta meta, Map<String, Object> m) {
        String potion = str(m.get("potion"));
        if (potion == null) {
            return;
        }
        try {
            PotionType type = PotionType.valueOf(potion.toUpperCase());
            meta.setBasePotionData(new PotionData(type, false, false));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static void applyCustomEffects(PotionMeta meta, Map<String, Object> m) {
        Object raw = m.get("potion-effects");
        if (!(raw instanceof List)) {
            return;
        }
        for (Object entry : (List<?>) raw) {
            applyCustomEffect(meta, str(entry));
        }
    }

    private static void applyCustomEffect(PotionMeta meta, String entry) {
        if (entry == null) {
            return;
        }
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

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static int num(Object o, int def) {
        return o instanceof Number ? ((Number) o).intValue() : def;
    }

    private static boolean bool(Object o) {
        return o instanceof Boolean && (Boolean) o;
    }
}
