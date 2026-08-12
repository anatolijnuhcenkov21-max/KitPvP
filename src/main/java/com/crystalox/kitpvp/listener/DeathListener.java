package com.crystalox.kitpvp.listener;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.arena.Arena;
import com.crystalox.kitpvp.commands.KitCommand;
import com.crystalox.kitpvp.shop.KitSelection;
import com.crystalox.kitpvp.shop.ShopGui;
import com.crystalox.kitpvp.util.Message;
import com.crystalox.kitpvp.util.SpawnUtil;
import com.crystalox.kitpvp.util.TabUpdater;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class DeathListener implements Listener {

    private final KitPvPPlugin plugin;

    public DeathListener(KitPvPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().clear();
        event.setKeepInventory(true);
        Player victim = event.getEntity();
        if (!isInArena(victim.getLocation())) {
            event.setDeathMessage(null);
            return;
        }
        Player killer = victim.getKiller();
        if (killer == null) {
            event.setDeathMessage(noKillerMessage(victim));
            return;
        }
        plugin.getStatsManager().addKill(killer.getUniqueId());
        if (hasBow(killer)) {
            killer.getInventory().addItem(new ItemStack(Material.ARROW, plugin.getConfig().getInt("arrows-per-kill", 8)));
        }
        rewardKiller(killer);
        checkKillstreakReward(killer);
        plugin.getStatsManager().addDeath(victim.getUniqueId());
        TabUpdater.update(victim, plugin);
        event.setDeathMessage(killMessage(victim, killer));
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        event.setRespawnLocation(SpawnUtil.randomSpawn(player.getWorld()));
        player.getInventory().clear();
        player.getInventory().setItem(8, KitCommand.selectorItem());
        player.getInventory().setItem(4, ShopGui.diamondItem());
        TabUpdater.update(player, plugin);
        KitSelection.clear(player.getUniqueId());
    }

    private boolean hasBow(Player p) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && (item.getType() == Material.BOW || item.getType() == Material.CROSSBOW)) {
                return true;
            }
        }
        return false;
    }

    private void rewardKiller(Player killer) {
        int coins = plugin.getConfig().getInt("coins-per-kill", 5);
        plugin.getStatsManager().addCoins(killer.getUniqueId(), coins);
        killer.sendMessage(Message.color(plugin.getConfig().getString("messages.coins-earned", "&6+%amount% coins").replace("%amount%", String.valueOf(coins))));
        TabUpdater.update(killer, plugin);
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

    private boolean isInArena(Location loc) {
        Arena arena = plugin.getArenaManager().getArena();
        return arena != null && arena.isEnabled() && arena.contains(loc);
    }

    private ConfigurationSection messages() {
        return plugin.getConfig().getConfigurationSection("messages");
    }
}
