package com.crystalox.kitpvp.listener;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.commands.KitCommand;
import com.crystalox.kitpvp.kit.Kit;
import com.crystalox.kitpvp.kit.KitApplier;
import com.crystalox.kitpvp.util.Message;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinQuitListener implements Listener {

    private final KitPvPPlugin plugin;

    public JoinQuitListener(KitPvPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        teleportToSpawn(player);
        applyDefaultKit(player);
        player.getInventory().setItem(8, KitCommand.selectorItem());
        plugin.getStatsManager().load(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getStatsManager().save(event.getPlayer().getUniqueId());
    }

    private void teleportToSpawn(Player player) {
        Location spawn = plugin.getArenaManager().getSpawn();
        if (spawn == null) {
            return;
        }
        player.teleport(spawn);
        player.sendMessage(Message.of(messages(), "join-spawn-teleport"));
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

    private ConfigurationSection messages() {
        return plugin.getConfig().getConfigurationSection("messages");
    }
}
