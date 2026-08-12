package com.crystalox.kitpvp;

import com.crystalox.kitpvp.arena.ArenaManager;
import com.crystalox.kitpvp.combat.CombatListener;
import com.crystalox.kitpvp.commands.ArenaCommand;
import com.crystalox.kitpvp.commands.KitCommand;
import com.crystalox.kitpvp.commands.StatsCommand;
import com.crystalox.kitpvp.kit.KitCooldownManager;
import com.crystalox.kitpvp.kit.KitManager;
import com.crystalox.kitpvp.listener.ArenaProtectionListener;
import com.crystalox.kitpvp.listener.DeathListener;
import com.crystalox.kitpvp.listener.JoinQuitListener;
import com.crystalox.kitpvp.scoreboard.StatsScoreboard;
import com.crystalox.kitpvp.stats.StatsManager;
import org.bukkit.plugin.java.JavaPlugin;

public class KitPvPPlugin extends JavaPlugin {

    private static KitPvPPlugin instance;

    private KitManager kitManager;
    private KitCooldownManager kitCooldownManager;
    private StatsManager statsManager;
    private ArenaManager arenaManager;

    public static KitPvPPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        kitManager = new KitManager(this);
        kitCooldownManager = new KitCooldownManager();
        statsManager = new StatsManager(this);
        arenaManager = new ArenaManager(this);
        getServer().getPluginManager().registerEvents(new JoinQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ArenaProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getCommand("kit").setExecutor(new KitCommand(this));
        getCommand("kits").setExecutor(new KitCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("kitpvp").setExecutor(new ArenaCommand(this));
        new StatsScoreboard(this).start();
        getLogger().info("KitPvP v" + getDescription().getVersion() + " enabled");
    }

    @Override
    public void onDisable() {
        statsManager.saveAllSync();
        instance = null;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public KitCooldownManager getKitCooldownManager() {
        return kitCooldownManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }
}
