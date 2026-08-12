package com.crystalox.kitpvp.listener;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.kit.Kit;
import com.crystalox.kitpvp.kit.KitApplier;
import com.crystalox.kitpvp.shop.KitSelection;
import com.crystalox.kitpvp.util.Message;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnDropListener implements Listener {

    private static final Set<UUID> FALL_IMMUNE = ConcurrentHashMap.newKeySet();

    private final KitPvPPlugin plugin;
    private final ConfigurationSection msgs;

    public SpawnDropListener(KitPvPPlugin plugin) {
        this.plugin = plugin;
        this.msgs = plugin.getConfig().getConfigurationSection("messages");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        int height = plugin.getConfig().getInt("kit-give-height", 120);
        if (from.getY() > height && to.getY() <= height) {
            giveKit(p);
        }
        if (FALL_IMMUNE.contains(p.getUniqueId()) && p.isOnGround()) {
            FALL_IMMUNE.remove(p.getUniqueId());
        }
    }

    private void giveKit(Player p) {
        String kitId = KitSelection.get(p.getUniqueId());
        Kit kit = plugin.getKitManager().getKit(kitId != null ? kitId : "peasant");
        KitSelection.clear(p.getUniqueId());
        if (kit == null) {
            kit = plugin.getKitManager().getKit("peasant");
        }
        if (kit == null) {
            return;
        }
        KitApplier.apply(p, kit);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 0, true, false));
        FALL_IMMUNE.add(p.getUniqueId());
        p.sendMessage(Message.color(Message.of(msgs, "kit-given").replace("%kit%", kit.getDisplayName())));
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getCause() == DamageCause.FALL && FALL_IMMUNE.contains(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        FALL_IMMUNE.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        FALL_IMMUNE.remove(uuid);
        KitSelection.clear(uuid);
    }
}
