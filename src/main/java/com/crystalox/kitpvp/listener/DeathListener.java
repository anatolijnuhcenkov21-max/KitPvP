package com.crystalox.kitpvp.listener;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.arena.Arena;
import com.crystalox.kitpvp.kit.Kit;
import com.crystalox.kitpvp.kit.KitApplier;
import com.crystalox.kitpvp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class DeathListener implements Listener {

    private final KitPvPPlugin plugin;

    public DeathListener(KitPvPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!isInArena(victim.getLocation())) {
            event.setDeathMessage(null);
            return;
        }
        event.getDrops().clear();
        Player killer = victim.getKiller();
        if (killer == null) {
            event.setDeathMessage(noKillerMessage(victim));
            return;
        }
        plugin.getStatsManager().addKill(killer.getUniqueId());
        checkKillstreakReward(killer);
        plugin.getStatsManager().addDeath(victim.getUniqueId());
        event.setDeathMessage(killMessage(victim, killer));
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Location respawn = event.getRespawnLocation();
        if (!isInArena(respawn) && !isArenaWorld(respawn)) {
            return;
        }
        event.setRespawnLocation(plugin.getArenaManager().getSpawn());
        final Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                applyDefaultKit(player);
            }
        }, 1L);
    }

    private void checkKillstreakReward(Player killer) {
        int streak = plugin.getStatsManager().getKillstreak(killer.getUniqueId());
        ConfigurationSection rewards = plugin.getConfig().getConfigurationSection("killstreak-rewards");
        if (rewards == null) {
            return;
        }
        for (String key : rewards.getKeys(false)) {
            if (parseInt(key) == streak) {
                giveReward(killer, rewards.getString(key));
                return;
            }
        }
    }

    private void giveReward(Player killer, String reward) {
        if (reward == null) {
            return;
        }
        if (reward.startsWith("cmd:")) {
            String command = reward.substring(4).replace("%player%", killer.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            return;
        }
        Bukkit.broadcastMessage(Message.color(reward.replace("%player%", killer.getName())));
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String killMessage(Player victim, Player killer) {
        return Message.of(messages(), "death-format")
                .replace("%victim%", victim.getName())
                .replace("%killer%", killer.getName());
    }

    private String noKillerMessage(Player victim) {
        return Message.of(messages(), "death-format-nokiller")
                .replace("%victim%", victim.getName());
    }

    private void applyDefaultKit(Player player) {
        player.getInventory().clear();
        Kit kit = plugin.getKitManager().getDefaultKit();
        if (kit == null) {
            return;
        }
        KitApplier.apply(player, kit);
        player.sendMessage(Message.of(messages(), "default-kit-given").replace("%kit%", kit.getId()));
    }

    private boolean isInArena(Location loc) {
        Arena arena = plugin.getArenaManager().getArena();
        return arena != null && arena.isEnabled() && arena.contains(loc);
    }

    private boolean isArenaWorld(Location loc) {
        Arena arena = plugin.getArenaManager().getArena();
        return arena != null && arena.isEnabled() && arena.getWorld().equals(loc.getWorld());
    }

    private ConfigurationSection messages() {
        return plugin.getConfig().getConfigurationSection("messages");
    }
}
