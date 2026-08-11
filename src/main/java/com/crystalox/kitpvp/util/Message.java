package com.crystalox.kitpvp.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

public final class Message {

    private Message() {
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String of(ConfigurationSection msgs, String key) {
        return color(msgs.getString(key));
    }
}
