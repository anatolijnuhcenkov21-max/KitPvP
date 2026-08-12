package com.crystalox.kitpvp.listener;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class StatSign {

    private final KitPvPPlugin plugin;

    public StatSign(KitPvPPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::update, 100L, 600L);
    }

    private void update() {
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("stat-sign");
        if (cfg == null) {
            return;
        }
        World world = Bukkit.getWorld(cfg.getString("world", "world"));
        if (world == null) {
            return;
        }
        Block block = world.getBlockAt((int) cfg.getDouble("x", 0), (int) cfg.getDouble("y", 64), (int) cfg.getDouble("z", 0));
        if (block.getType() == Material.AIR) {
            block.setType(Material.OAK_SIGN);
        }
        BlockState state = block.getState();
        if (!(state instanceof Sign)) {
            return;
        }
        Sign sign = (Sign) state;
        sign.setLine(0, "§6§lТоп игроков");
        List<StatsManager.StatsEntry> top = plugin.getStatsManager().getTop(3);
        for (int i = 0; i < 3; i++) {
            if (i < top.size()) {
                StatsManager.StatsEntry entry = top.get(i);
                String name = Bukkit.getOfflinePlayer(entry.uuid).getName();
                sign.setLine(i + 1, "§e" + (i + 1) + ". §f" + (name != null ? name : "?") + " §7— §e" + entry.kills);
            } else {
                sign.setLine(i + 1, "");
            }
        }
        sign.update();
    }
}
