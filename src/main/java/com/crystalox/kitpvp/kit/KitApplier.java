package com.crystalox.kitpvp.kit;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Map;

public final class KitApplier {

    private KitApplier() {
    }

    public static void apply(Player player, Kit kit) {
        apply(player, kit, true);
    }

    public static void apply(Player player, Kit kit, boolean clearFirst) {
        if (clearFirst) {
            player.getInventory().clear();
            applyArmor(player, kit);
        }
        applyItems(player, kit);
        applyEffects(player, kit);
        player.updateInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
    }

    private static void applyArmor(Player player, Kit kit) {
        ItemStack helmet = kit.getArmor().get("helmet");
        ItemStack chestplate = kit.getArmor().get("chestplate");
        ItemStack leggings = kit.getArmor().get("leggings");
        ItemStack boots = kit.getArmor().get("boots");
        if (helmet != null) {
            player.getInventory().setHelmet(helmet);
        }
        if (chestplate != null) {
            player.getInventory().setChestplate(chestplate);
        }
        if (leggings != null) {
            player.getInventory().setLeggings(leggings);
        }
        if (boots != null) {
            player.getInventory().setBoots(boots);
        }
    }

    private static void applyItems(Player player, Kit kit) {
        for (ItemStack item : kit.getItems()) {
            if (item == null) {
                continue;
            }
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    private static void applyEffects(Player player, Kit kit) {
        for (PotionEffect effect : kit.getEffects()) {
            player.addPotionEffect(effect);
        }
    }
}
