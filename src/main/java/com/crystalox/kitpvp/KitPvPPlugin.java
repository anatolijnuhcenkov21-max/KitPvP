package com.crystalox.kitpvp;

import com.crystalox.kitpvp.arena.ArenaManager;
import com.crystalox.kitpvp.combat.CombatListener;
import com.crystalox.kitpvp.commands.ArenaCommand;
import com.crystalox.kitpvp.event.EventManager;
import com.crystalox.kitpvp.commands.KitCommand;
import com.crystalox.kitpvp.commands.StatsCommand;
import com.crystalox.kitpvp.commands.TopCommand;
import com.crystalox.kitpvp.gui.LeaderboardGui;
import com.crystalox.kitpvp.kit.KitCooldownManager;
import com.crystalox.kitpvp.kit.KitManager;
import com.crystalox.kitpvp.listener.ArenaProtectionListener;
import com.crystalox.kitpvp.listener.DeathListener;
import com.crystalox.kitpvp.listener.DropProtectionListener;
import com.crystalox.kitpvp.listener.JoinQuitListener;
import com.crystalox.kitpvp.listener.SpawnDropListener;
import com.crystalox.kitpvp.quest.QuestGui;
import com.crystalox.kitpvp.quest.QuestManager;
import com.crystalox.kitpvp.scoreboard.StatsScoreboard;
import com.crystalox.kitpvp.shop.PlayerKitStore;
import com.crystalox.kitpvp.shop.ShopVillager;
import com.crystalox.kitpvp.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class KitPvPPlugin extends JavaPlugin {

    private static KitPvPPlugin instance;

    private KitManager kitManager;
    private KitCooldownManager kitCooldownManager;
    private StatsManager statsManager;
    private ArenaManager arenaManager;
    private PlayerKitStore kitStore;
    private ShopVillager shopVillager;
    private QuestManager questManager;
    private EventManager eventManager;

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
        eventManager = new EventManager(this);
        kitStore = new PlayerKitStore(this);
        questManager = new QuestManager(this);
        clearArenaWeather();
        getServer().getPluginManager().registerEvents(new JoinQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ArenaProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnDropListener(this), this);
        getServer().getPluginManager().registerEvents(new DropProtectionListener(this), this);
        new com.crystalox.kitpvp.shop.ShopGui(this);
        new QuestGui(this);
        shopVillager = new ShopVillager(this);
        getCommand("kit").setExecutor(new KitCommand(this));
        getCommand("kits").setExecutor(new KitCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("top").setExecutor(new TopCommand(this));
        getCommand("kitpvp").setExecutor(new ArenaCommand(this));
        new LeaderboardGui(this);
        new StatsScoreboard(this).start();
        getLogger().info("KitPvP v" + getDescription().getVersion() + " enabled");
    }

    private void clearArenaWeather() {
        World world = Bukkit.getWorld(getConfig().getString("arena.world", "world"));
        if (world == null) {
            return;
        }
        world.setStorm(false);
        world.setThundering(false);
        world.setWeatherDuration(Integer.MAX_VALUE);
        world.setThunderDuration(Integer.MAX_VALUE);
    }

    @Override
    public void onDisable() {
        statsManager.saveAllSync();
        if (kitStore != null) {
            kitStore.close();
        }
        if (questManager != null) {
            questManager.close();
        }
        if (shopVillager != null) {
            shopVillager.shutdown();
        }
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

    public PlayerKitStore getKitStore() {
        return kitStore;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }
}
