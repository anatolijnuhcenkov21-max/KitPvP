package com.crystalox.kitpvp.economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public final class EconomyBridge {

    private EconomyBridge() {
    }

    public static boolean withdraw(Player player, double amount) {
        if (amount <= 0) {
            return true;
        }
        Plugin eco = Bukkit.getPluginManager().getPlugin("EconomyPlus");
        if (eco == null || !eco.isEnabled()) {
            return false;
        }
        try {
            Class<?> api = Class.forName("com.crystalox.economy.api.EconomyPlusAPI");
            String currency = (String) api.getMethod("getDefaultCurrency").invoke(null);
            Object result = api.getMethod("withdraw", UUID.class, String.class, double.class)
                    .invoke(null, player.getUniqueId(), currency, amount);
            return Boolean.TRUE.equals(result);
        } catch (Throwable t) {
            return false;
        }
    }
}
