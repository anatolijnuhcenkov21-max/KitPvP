package com.crystalox.kitpvp.commands;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.stats.StatsManager;
import com.crystalox.kitpvp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CoinsCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ACTIONS = Arrays.asList("give", "set");

    private final KitPvPPlugin plugin;

    public CoinsCommand(KitPvPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crystalox.kitpvp.admin")) {
            sender.sendMessage(Message.color("&cНедостаточно прав."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(usage());
            return true;
        }
        int amount = parseAmount(sender, args[1]);
        if (amount < 0) {
            return true;
        }
        Player target = resolveTarget(sender, args);
        if (target == null) {
            return true;
        }
        String action = args[0].toLowerCase();
        if (action.equals("give")) {
            give(sender, target, amount);
        } else if (action.equals("set")) {
            set(sender, target, amount);
        } else {
            sender.sendMessage(usage());
        }
        return true;
    }

    private int parseAmount(CommandSender sender, String raw) {
        try {
            int amount = Integer.parseInt(raw);
            if (amount < 0) {
                sender.sendMessage(Message.color("&cНеверное число."));
                return -1;
            }
            return amount;
        } catch (NumberFormatException e) {
            sender.sendMessage(Message.color("&cНеверное число."));
            return -1;
        }
    }

    private Player resolveTarget(CommandSender sender, String[] args) {
        if (args.length >= 3) {
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(Message.color("&cИгрок не найден."));
            }
            return target;
        }
        if (sender instanceof Player) {
            return (Player) sender;
        }
        sender.sendMessage(Message.color("&cУкажи игрока."));
        return null;
    }

    private void give(CommandSender sender, Player target, int amount) {
        plugin.getStatsManager().addCoins(target.getUniqueId(), amount);
        sender.sendMessage(Message.color("&aВыдано &6" + amount + " &aмонет игроку &e" + target.getName() + "&a."));
        target.sendMessage(Message.color("&aВы получили &6" + amount + " &aмонет!"));
    }

    private void set(CommandSender sender, Player target, int amount) {
        UUID uuid = target.getUniqueId();
        StatsManager stats = plugin.getStatsManager();
        int current = stats.getCoins(uuid);
        if (amount > current) {
            stats.addCoins(uuid, amount - current);
        } else {
            stats.spendCoins(uuid, current - amount);
        }
        sender.sendMessage(Message.color("&aБаланс игрока &e" + target.getName() + " &aустановлен: &6" + amount));
    }

    private String usage() {
        return Message.color("&cИспользование: /coins <give|set> <amount> [player]");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(ACTIONS, args[0]);
        }
        if (args.length == 3) {
            return filter(onlineNames(), args[2]);
        }
        return new ArrayList<String>();
    }

    private List<String> onlineNames() {
        List<String> names = new ArrayList<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private List<String> filter(List<String> options, String prefix) {
        List<String> result = new ArrayList<String>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(option);
            }
        }
        return result;
    }
}
