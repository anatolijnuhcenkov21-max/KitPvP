package com.crystalox.kitpvp.shop;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class ShopVillager implements Listener {

    private final KitPvPPlugin plugin;
    private Villager villager;

    public ShopVillager(KitPvPPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        spawn();
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!isAlive()) {
                spawn();
            }
        }, 100L, 100L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!isShopVillager(event.getRightClicked())) {
            return;
        }
        event.setCancelled(true);
        ShopGui.open(event.getPlayer(), plugin);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!isShopVillager(event.getEntity())) {
            return;
        }
        event.setCancelled(true);
    }

    public void shutdown() {
        if (villager != null) {
            villager.remove();
            villager = null;
        }
    }

    private void spawn() {
        if (villager != null && villager.isValid()) {
            return;
        }
        Location location = loc();
        if (location == null || location.getWorld() == null) {
            return;
        }
        villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setAI(false);
        villager.setSilent(true);
        villager.setInvulnerable(true);
        villager.setCollidable(false);
        villager.setGravity(true);
        villager.setPersistent(true);
        villager.setCustomName(Message.color("&6&lKit Shop"));
        villager.setCustomNameVisible(true);
        setProfession();
    }

    private void setProfession() {
        try {
            villager.setProfession(Villager.Profession.LIBRARIAN);
        } catch (Exception ignored) {
        }
    }

    private boolean isAlive() {
        return villager != null && villager.isValid() && !villager.isDead();
    }

    private boolean isShopVillager(Entity entity) {
        if (isAlive() && entity == villager) {
            return true;
        }
        if (!(entity instanceof Villager) || entity.getCustomName() == null) {
            return false;
        }
        return ChatColor.stripColor(entity.getCustomName()).equals("Kit Shop");
    }

    private Location loc() {
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("shop-villager");
        if (cfg == null) {
            return null;
        }
        World world = Bukkit.getWorld(cfg.getString("world", "world"));
        if (world == null) {
            return null;
        }
        return new Location(world, cfg.getDouble("x", 0.0), cfg.getDouble("y", 64.0), cfg.getDouble("z", 0.0), 0f, 0f);
    }
}
