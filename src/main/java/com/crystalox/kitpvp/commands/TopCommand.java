package com.crystalox.kitpvp.commands;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.stats.StatsManager;
import com.crystalox.kitpvp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class TopCommand implements CommandExecutor {

    private final KitPvPPlugin plugin;

    public TopCommand(KitPvPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<StatsManager.StatsEntry> list = plugin.getStatsManager().getTop(10);
        sender.sendMessage(Message.color("&6&lТоп игроков:"));
        for (int i = 0; i < Math.min(10, list.size()); i++) {
            StatsManager.StatsEntry entry = list.get(i);
            String name = Bukkit.getOfflinePlayer(entry.uuid).getName();
            sender.sendMessage(Message.color("&e" + (i + 1) + ". &f" + (name != null ? name : "?") + " &7— &e" + entry.kills + " &7убийств"));
        }
        return true;
    }
}
