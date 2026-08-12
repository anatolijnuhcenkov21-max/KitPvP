package com.crystalox.kitpvp.combat;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.arena.Arena;
import com.crystalox.kitpvp.util.Message;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatListener implements Listener {

    private final KitPvPPlugin plugin;
    private final ComboTracker comboTracker;

    public CombatListener(KitPvPPlugin plugin) {
        this.plugin = plugin;
        this.comboTracker = new ComboTracker(plugin.getConfig().getInt("combo-window-ms"));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player damaged = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        if (isProtected(damaged)) {
            event.setCancelled(true);
            return;
        }
        if (isInArena(damaged)) {
            trackCombo(attacker, damaged);
        }
    }

    private boolean isProtected(Player damaged) {
        Location spawn = plugin.getArenaManager().getSpawn();
        if (spawn == null || !spawn.getWorld().equals(damaged.getWorld())) {
            return false;
        }
        int radius = plugin.getConfig().getInt("spawn-protection-radius");
        return damaged.getLocation().distance(spawn) <= radius;
    }

    private boolean isInArena(Player damaged) {
        Arena arena = plugin.getArenaManager().getArena();
        return arena != null && arena.isEnabled() && arena.contains(damaged.getLocation());
    }

    private void trackCombo(Player attacker, Player damaged) {
        comboTracker.hit(attacker.getUniqueId(), System.currentTimeMillis());
        if (plugin.getConfig().getBoolean("enable-combo")) {
            int combo = comboTracker.getCombo(attacker.getUniqueId());
            if (combo >= 2) {
                attacker.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Message.color("&6Комбо x" + combo)));
            }
        }
        comboTracker.reset(damaged.getUniqueId());
    }
}
