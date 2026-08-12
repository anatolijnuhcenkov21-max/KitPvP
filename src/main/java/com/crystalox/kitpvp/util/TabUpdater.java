package com.crystalox.kitpvp.util;

import com.crystalox.kitpvp.KitPvPPlugin;
import org.bukkit.entity.Player;

public final class TabUpdater {

    private TabUpdater() {
    }

    public static void update(Player p, KitPvPPlugin plugin) {
        String fmt = plugin.getConfig().getString("tab-format", "&7%player% &8[&7KS:&f%streak%&8]");
        if (fmt == null || fmt.isEmpty()) {
            p.setPlayerListName(null);
            return;
        }
        String name = fmt.replace("%player%", p.getName())
                .replace("%streak%", String.valueOf(plugin.getStatsManager().getMaxStreak(p.getUniqueId())));
        p.setPlayerListName(Message.color(name));
    }
}
