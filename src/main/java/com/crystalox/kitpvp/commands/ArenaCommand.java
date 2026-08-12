package com.crystalox.kitpvp.commands;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.util.Message;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ArenaCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN_PERMISSION = "crystalox.kitpvp.admin";
    private static final List<String> SUBCOMMANDS =
            Arrays.asList("pos1", "pos2", "savearena", "setspawn", "reload");

    private final KitPvPPlugin plugin;

    public ArenaCommand(KitPvPPlugin plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("kitpvp");
        if (command != null) {
            command.setTabCompleter(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Message.color("&cТолько игроки могут использовать эту команду."));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            player.sendMessage(msg("no-permission"));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(msg("unknown-command"));
            return true;
        }
        String sub = args[0].toLowerCase();
        if ("pos1".equals(sub)) {
            plugin.getArenaManager().setPos1(player.getLocation());
            player.sendMessage(msg("arena-pos1-set"));
        } else if ("pos2".equals(sub)) {
            plugin.getArenaManager().setPos2(player.getLocation());
            player.sendMessage(msg("arena-pos2-set"));
        } else if ("savearena".equals(sub)) {
            saveArena(player);
        } else if ("setspawn".equals(sub)) {
            setSpawn(player);
        } else if ("reload".equals(sub)) {
            reload(player);
        } else {
            player.sendMessage(msg("unknown-command"));
        }
        return true;
    }

    private void saveArena(Player player) {
        if (!plugin.getArenaManager().hasPositions()) {
            player.sendMessage(msg("arena-not-set"));
            return;
        }
        plugin.getArenaManager().saveArena();
        player.sendMessage(msg("arena-saved"));
    }

    private void setSpawn(Player player) {
        plugin.getArenaManager().saveSpawn(player.getLocation());
        Location spawn = plugin.getArenaManager().getSpawn();
        if (spawn != null) {
            player.teleport(spawn);
        }
        player.sendMessage(msg("spawn-set"));
    }

    private void reload(Player player) {
        plugin.reloadConfig();
        plugin.getKitManager().reload();
        player.sendMessage(msg("reload-success"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase();
        List<String> result = new ArrayList<String>();
        for (String sub : SUBCOMMANDS) {
            if (sub.startsWith(prefix)) {
                result.add(sub);
            }
        }
        return result;
    }

    private String msg(String key) {
        return Message.of(plugin.getConfig().getConfigurationSection("messages"), key);
    }
}
