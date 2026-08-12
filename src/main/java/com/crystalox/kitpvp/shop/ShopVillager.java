package com.crystalox.kitpvp.shop;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.commands.KitCommand;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ShopVillager implements Listener {

    private final KitPvPPlugin plugin;
    private final Map<String, Villager> npcs = new ConcurrentHashMap<String, Villager>();

    public ShopVillager(KitPvPPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().runTaskLater(plugin, this::spawnAll, 40L);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::spawnAll, 200L, 200L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!isNpc(event.getRightClicked())) {
            return;
        }
        event.setCancelled(true);
        String name = ChatColor.stripColor(event.getRightClicked().getCustomName());
        if ("Kit Shop".equals(name)) {
            ShopGui.open(event.getPlayer(), plugin);
        } else if ("Class Selector".equals(name)) {
            if (KitCommand.INSTANCE != null) {
                KitCommand.INSTANCE.openKitGui(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (isNpc(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    public void shutdown() {
        for (Villager v : npcs.values()) {
            v.remove();
        }
        npcs.clear();
    }

    private void spawnAll() {
        spawnNpc("shop");
        spawnNpc("class");
    }

    private void spawnNpc(String key) {
        Villager v = npcs.get(key);
        if (v != null && v.isValid()) {
            return;
        }
        Location location = loc(key);
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("NPC " + key + ": config location invalid");
            return;
        }
        World world = location.getWorld();
        world.loadChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4);
        Location ground = ground(world, location);
        try {
            v = (Villager) world.spawnEntity(ground, EntityType.VILLAGER);
        } catch (Exception e) {
            plugin.getLogger().warning("NPC " + key + ": spawn failed: " + e.getMessage());
            return;
        }
        v.setRotation(-90f, 0f);
        configure(v, key);
        npcs.put(key, v);
        plugin.getLogger().info("NPC " + key + " spawned at " + ground.getBlockX() + "," + ground.getBlockY() + "," + ground.getBlockZ());
    }

    private void configure(Villager v, String key) {
        v.setAI(false);
        v.setSilent(true);
        v.setInvulnerable(true);
        v.setCollidable(false);
        v.setGravity(false);
        v.setPersistent(true);
        String name = key.equals("shop") ? "&6&lKit Shop" : "&a&lClass Selector";
        v.setCustomName(Message.color(name));
        v.setCustomNameVisible(true);
        try {
            v.setProfession(Villager.Profession.LIBRARIAN);
        } catch (Exception ignored) {
        }
    }

    private Location ground(World world, Location location) {
        for (int y = location.getBlockY(); y > location.getBlockY() - 15; y--) {
            if (world.getBlockAt(location.getBlockX(), y, location.getBlockZ()).getType().isSolid()) {
                return new Location(world, location.getBlockX() + 0.5, y + 1, location.getBlockZ() + 0.5, 0f, 0f);
            }
        }
        return new Location(world, location.getX(), location.getY(), location.getZ(), 0f, 0f);
    }

    private boolean isNpc(Entity entity) {
        if (npcs.containsValue(entity)) {
            return true;
        }
        if (!(entity instanceof Villager) || entity.getCustomName() == null) {
            return false;
        }
        String name = ChatColor.stripColor(entity.getCustomName());
        return name.equals("Kit Shop") || name.equals("Class Selector");
    }

    private Location loc(String key) {
        String section = key.equals("shop") ? "shop-villager" : "class-villager";
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection(section);
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
