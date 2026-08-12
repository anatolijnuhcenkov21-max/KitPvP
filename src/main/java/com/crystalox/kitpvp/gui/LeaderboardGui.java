package com.crystalox.kitpvp.gui;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.stats.StatsManager;
import com.crystalox.kitpvp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LeaderboardGui implements Listener {

    private static final String TITLE = "Топ игроков";
    private static final int PAGE_SIZE = 44;
    private static final int TOP_SIZE = 200;

    private static final Map<UUID, Integer> pages = new ConcurrentHashMap<UUID, Integer>();

    private final KitPvPPlugin plugin;

    public LeaderboardGui(KitPvPPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static void open(Player player, KitPvPPlugin plugin) {
        open(player, plugin, 0);
    }

    private static void open(Player player, KitPvPPlugin plugin, int page) {
        pages.put(player.getUniqueId(), page);
        List<StatsManager.StatsEntry> top = plugin.getStatsManager().getTop(TOP_SIZE);
        int total = Math.max(1, (top.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        Inventory inv = Bukkit.createInventory(null, 54, Message.color("&6&l" + TITLE));
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler());
        }
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = start + i;
            if (index >= top.size()) {
                break;
            }
            inv.setItem(2 + i, entryItem(top.get(index), index + 1));
        }
        if (page > 0) {
            inv.setItem(48, nav(Material.ARROW, "&a◀ Назад"));
        }
        inv.setItem(50, nav(Material.PAPER, "&fСтр. " + (page + 1) + "/" + total));
        if (page < total - 1) {
            inv.setItem(52, nav(Material.ARROW, "&aВперёд ▶"));
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!TITLE.equals(ChatColor.stripColor(event.getView().getTitle()))) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        Integer current = pages.get(player.getUniqueId());
        int page = current == null ? 0 : current;
        List<StatsManager.StatsEntry> top = plugin.getStatsManager().getTop(TOP_SIZE);
        int total = Math.max(1, (top.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (event.getSlot() == 48 && page > 0) {
            open(player, plugin, page - 1);
        } else if (event.getSlot() == 52 && page < total - 1) {
            open(player, plugin, page + 1);
        }
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !isStatSign(block)) {
            return;
        }
        event.setCancelled(true);
        open(event.getPlayer(), plugin);
    }

    private boolean isStatSign(Block block) {
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("stat-sign");
        if (cfg == null) {
            return false;
        }
        World world = Bukkit.getWorld(cfg.getString("world", "world"));
        if (world == null || !world.equals(block.getWorld())) {
            return false;
        }
        return block.getX() == (int) cfg.getDouble("x", 0)
                && block.getY() == (int) cfg.getDouble("y", 64)
                && block.getZ() == (int) cfg.getDouble("z", 0);
    }

    private static ItemStack entryItem(StatsManager.StatsEntry entry, int position) {
        String name = Bukkit.getOfflinePlayer(entry.uuid).getName();
        String display = name != null ? name : "?";
        return named(Material.PAPER,
                "&e" + position + ". &f" + display + " &7— &e" + entry.kills + " &7убийств | &6" + entry.coins + " &7монет",
                new ArrayList<String>());
    }

    private static ItemStack nav(Material material, String name) {
        return named(material, name, new ArrayList<String>());
    }

    private static ItemStack filler() {
        return named(Material.GLASS_PANE, " ", new ArrayList<String>());
    }

    private static ItemStack named(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Message.color(name));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
