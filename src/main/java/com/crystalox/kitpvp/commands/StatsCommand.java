package com.crystalox.kitpvp.commands;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.stats.StatsManager;
import com.crystalox.kitpvp.util.Format;
import com.crystalox.kitpvp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class StatsCommand implements CommandExecutor {

    private static final int TOP_SIZE = 10;

    private final KitPvPPlugin plugin;

    public StatsCommand(KitPvPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 && !(sender instanceof Player)) {
            sender.sendMessage(Message.color("&cТолько игроки могут использовать эту команду."));
            return true;
        }
        Player target = args.length == 0 ? (Player) sender : Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(msg("unknown-command"));
            return true;
        }
        sendStats(sender, target);
        sendTop(sender);
        return true;
    }

    private void sendStats(CommandSender sender, Player target) {
        UUID uuid = target.getUniqueId();
        StatsManager stats = plugin.getStatsManager();
        sender.sendMessage(msg("stats-title").replace("%player%", target.getName()));
        sender.sendMessage(msg("stats-kills").replace("%kills%", Format.number(stats.getKills(uuid))));
        sender.sendMessage(msg("stats-deaths").replace("%deaths%", Format.number(stats.getDeaths(uuid))));
        sender.sendMessage(msg("stats-kdr").replace("%kdr%", kdr(stats, uuid)));
        sender.sendMessage(msg("stats-killstreak").replace("%killstreak%", Format.number(stats.getKillstreak(uuid))));
    }

    private String kdr(StatsManager stats, UUID uuid) {
        int kills = stats.getKills(uuid);
        int deaths = stats.getDeaths(uuid);
        double kdr = deaths == 0 ? kills : (double) kills / deaths;
        return String.format("%.2f", kdr);
    }

    private void sendTop(CommandSender sender) {
        sender.sendMessage(msg("stats-top-header"));
        List<StatsManager.StatsEntry> top = plugin.getStatsManager().getTop(TOP_SIZE);
        int position = 1;
        for (StatsManager.StatsEntry entry : top) {
            sender.sendMessage(msg("stats-top-entry")
                    .replace("%position%", String.valueOf(position))
                    .replace("%player%", playerName(entry.uuid))
                    .replace("%kills%", Format.number(entry.kills)));
            position++;
        }
    }

    private String playerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline.getName();
        return name == null ? "?" : name;
    }

    private String msg(String key) {
        return Message.of(plugin.getConfig().getConfigurationSection("messages"), key);
    }
}
