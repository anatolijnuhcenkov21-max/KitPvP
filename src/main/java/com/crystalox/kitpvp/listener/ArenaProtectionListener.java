package com.crystalox.kitpvp.listener;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.arena.Arena;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ArenaProtectionListener implements Listener {

    private final KitPvPPlugin plugin;

    public ArenaProtectionListener(KitPvPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().hasPermission("crystalox.kitpvp.admin")) {
            return;
        }
        if (isInArena(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getPlayer().hasPermission("crystalox.kitpvp.admin")) {
            return;
        }
        if (isInArena(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getPlayer().hasPermission("crystalox.kitpvp.admin")) {
            return;
        }
        if (isInArena(block.getLocation())) {
            event.setCancelled(true);
        }
    }

    private boolean isInArena(Location loc) {
        Arena arena = plugin.getArenaManager().getArena();
        return arena != null && arena.isEnabled() && arena.contains(loc);
    }
}
