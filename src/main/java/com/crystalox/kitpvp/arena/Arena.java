package com.crystalox.kitpvp.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class Arena {

    private World world;
    private Location min;
    private Location max;
    private Location spawn;

    public boolean isEnabled() {
        return world != null;
    }

    public boolean contains(Location loc) {
        if (!isEnabled() || loc == null || !loc.getWorld().equals(world)) {
            return false;
        }
        return loc.getX() >= min.getX() && loc.getX() <= max.getX()
                && loc.getZ() >= min.getZ() && loc.getZ() <= max.getZ();
    }

    public Location getSpawn() {
        return spawn;
    }

    public World getWorld() {
        return world;
    }

    public static Arena fromConfig(ConfigurationSection s) {
        Arena arena = new Arena();
        if (s == null || !s.getBoolean("enabled", true)) {
            return arena;
        }
        String worldName = s.getString("world");
        if (worldName != null) {
            arena.world = Bukkit.getWorld(worldName);
        }
        if (arena.world == null) {
            return arena;
        }
        arena.min = readLocation(s.getConfigurationSection("min"), arena.world);
        arena.max = readLocation(s.getConfigurationSection("max"), arena.world);
        arena.spawn = readLocation(s.getConfigurationSection("spawn"), arena.world);
        return arena;
    }

    public void saveTo(ConfigurationSection s) {
        if (world != null) {
            s.set("world", world.getName());
        }
        writeLocation(s.createSection("min"), min);
        writeLocation(s.createSection("max"), max);
        writeLocation(s.createSection("spawn"), spawn);
    }

    private static Location readLocation(ConfigurationSection s, World world) {
        if (s == null) {
            return null;
        }
        return new Location(world,
                s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
    }

    private static void writeLocation(ConfigurationSection s, Location loc) {
        if (loc == null) {
            return;
        }
        s.set("x", loc.getX());
        s.set("y", loc.getY());
        s.set("z", loc.getZ());
        s.set("yaw", (double) loc.getYaw());
        s.set("pitch", (double) loc.getPitch());
    }
}
