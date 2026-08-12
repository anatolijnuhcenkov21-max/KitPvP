package com.crystalox.kitpvp.util;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.commands.KitCommand;
import com.crystalox.kitpvp.shop.KitSelection;
import com.crystalox.kitpvp.shop.ShopGui;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public final class SpawnUtil {

    private SpawnUtil() {
    }

    public static Location randomSpawn(World world) {
        Random r = new Random();
        int x = -11 + r.nextInt(10);
        int z = -27 + r.nextInt(27);
        return new Location(world, x + 0.5, 155, z + 0.5, 0f, 0f);
    }

    public static void sendToLobby(Player p, JavaPlugin plugin) {
        p.teleport(randomSpawn(p.getWorld()));
        p.getInventory().clear();
        p.getInventory().setItem(8, KitCommand.selectorItem());
        p.getInventory().setItem(4, ShopGui.diamondItem());
        TabUpdater.update(p, (KitPvPPlugin) plugin);
        KitSelection.clear(p.getUniqueId());
    }
}
