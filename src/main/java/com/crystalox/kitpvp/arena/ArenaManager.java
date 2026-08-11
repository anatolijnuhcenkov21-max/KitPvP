package com.crystalox.kitpvp.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class ArenaManager {

    private final JavaPlugin plugin;
    private Arena arena;
    private Location pos1;
    private Location pos2;

    public ArenaManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.arena = Arena.fromConfig(plugin.getConfig().getConfigurationSection("arena"));
    }

    public Arena getArena() {
        return arena;
    }

    public void setPos1(Location loc) {
        pos1 = loc;
    }

    public void setPos2(Location loc) {
        pos2 = loc;
    }

    public boolean hasPositions() {
        return pos1 != null && pos2 != null
                && pos1.getWorld() != null
                && pos1.getWorld().equals(pos2.getWorld());
    }

    public void saveArena() {
        if (!hasPositions()) {
            return;
        }
        ConfigurationSection section = plugin.getConfig().createSection("arena");
        section.set("enabled", true);
        section.set("world", pos1.getWorld().getName());
        writeLocation(section.createSection("min"), min(pos1, pos2));
        writeLocation(section.createSection("max"), max(pos1, pos2));
        plugin.saveConfig();
        arena = Arena.fromConfig(plugin.getConfig().getConfigurationSection("arena"));
    }

    public void saveSpawn(Location loc) {
        ConfigurationSection section = plugin.getConfig().createSection("spawn-location");
        section.set("world", loc.getWorld().getName());
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());
        section.set("yaw", (double) loc.getYaw());
        section.set("pitch", (double) loc.getPitch());
        plugin.saveConfig();
    }

    public Location getSpawn() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("spawn-location");
        if (section == null) {
            return null;
        }
        World world = Bukkit.getWorld(section.getString("world"));
        if (world == null) {
            return null;
        }
        return new Location(world,
                section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
    }

    private static Location min(Location a, Location b) {
        return new Location(a.getWorld(),
                Math.min(a.getX(), b.getX()),
                Math.min(a.getY(), b.getY()),
                Math.min(a.getZ(), b.getZ()));
    }

    private static Location max(Location a, Location b) {
        return new Location(a.getWorld(),
                Math.max(a.getX(), b.getX()),
                Math.max(a.getY(), b.getY()),
                Math.max(a.getZ(), b.getZ()));
    }

    private static void writeLocation(ConfigurationSection s, Location loc) {
        s.set("x", loc.getX());
        s.set("y", loc.getY());
        s.set("z", loc.getZ());
        s.set("yaw", (double) loc.getYaw());
        s.set("pitch", (double) loc.getPitch());
    }
}
