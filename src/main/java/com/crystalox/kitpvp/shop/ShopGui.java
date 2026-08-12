package com.crystalox.kitpvp.shop;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.kit.Kit;
import com.crystalox.kitpvp.stats.StatsManager;
import com.crystalox.kitpvp.util.Format;
import com.crystalox.kitpvp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopGui implements Listener {

    private static final int PAGE_SIZE = 18;
    private static final String SHOP_TITLE = "Kit Shop";
    private static final Map<UUID, Integer> pages = new ConcurrentHashMap<UUID, Integer>();

    private final KitPvPPlugin plugin;

    public ShopGui(KitPvPPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static ItemStack diamondItem() {
        return named(Material.DIAMOND, "&b&lKit Shop", "&7Right-click to open the shop");
    }

    public static ItemStack coinItem(int amount) {
        return named(Material.GOLD_INGOT, "&6Coins: &f" + amount);
    }

    public static void open(Player player, KitPvPPlugin plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, Message.color("&8" + SHOP_TITLE));
        List<Kit> kits = sortedKits(plugin);
        int page = pages.getOrDefault(player.getUniqueId(), 0);
        int total = totalPages(kits);
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = start + i;
            inv.setItem(i, index < kits.size() ? kitIcon(player, plugin, kits.get(index)) : filler());
        }
        inv.setItem(20, crateButton(plugin));
        inv.setItem(22, coinItem(plugin.getStatsManager().getCoins(player.getUniqueId())));
        inv.setItem(24, pageIndicator(page, total));
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!SHOP_TITLE.equals(ChatColor.stripColor(event.getView().getTitle()))) {
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
        int slot = event.getSlot();
        if (slot == 20) {
            rollCrate(player, plugin);
        } else if (slot == 24) {
            pages.compute(player.getUniqueId(), (k, v) -> (v == null ? 0 : v + 1) % totalPages(sortedKits(plugin)));
            open(player, plugin);
        } else if (slot < PAGE_SIZE) {
            buyFromSlot(player, slot);
        }
    }

    private void buyFromSlot(Player player, int slot) {
        Kit kit = kitForSlot(player, plugin, slot);
        if (kit == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        PlayerKitStore store = plugin.getKitStore();
        if (kit.getPrice() <= 0 || store.owns(uuid, kit.getId())) {
            player.sendMessage(Message.of(msgs(), "kit-already-owned").replace("%kit%", kit.getDisplayName()));
            return;
        }
        int cost = (int) kit.getPrice();
        StatsManager stats = plugin.getStatsManager();
        if (stats.spendCoins(uuid, cost)) {
            completePurchase(player, store, kit, cost);
        } else {
            player.sendMessage(Message.of(msgs(), "coins-not-enough")
                    .replace("%cost%", String.valueOf(cost))
                    .replace("%coins%", String.valueOf(stats.getCoins(uuid))));
        }
    }

    private void completePurchase(Player player, PlayerKitStore store, Kit kit, int cost) {
        store.buy(player.getUniqueId(), kit.getId());
        player.sendMessage(Message.of(msgs(), "class-bought")
                .replace("%kit%", kit.getDisplayName())
                .replace("%cost%", String.valueOf(cost)));
        player.closeInventory();
        open(player, plugin);
    }

    private Kit kitForSlot(Player player, KitPvPPlugin plugin, int slot) {
        int page = pages.getOrDefault(player.getUniqueId(), 0);
        List<Kit> kits = sortedKits(plugin);
        int index = page * PAGE_SIZE + slot;
        return index < kits.size() ? kits.get(index) : null;
    }

    private void rollCrate(Player player, KitPvPPlugin plugin) {
        int cost = plugin.getConfig().getInt("crate-cost", 50);
        StatsManager stats = plugin.getStatsManager();
        UUID uuid = player.getUniqueId();
        if (!stats.spendCoins(uuid, cost)) {
            player.sendMessage(Message.of(msgs(), "crate-no-funds").replace("%cost%", String.valueOf(cost)));
            return;
        }
        grantReward(player, plugin, pickReward(plugin.getConfig().getMapList("crate-rewards")));
        player.closeInventory();
    }

    private void grantReward(Player player, KitPvPPlugin plugin, Map<?, ?> reward) {
        String type = String.valueOf(reward.get("type")).toUpperCase();
        String name = String.valueOf(reward.get("name"));
        int amount = ((Number) reward.get("amount")).intValue();
        if ("COINS".equals(type)) {
            plugin.getStatsManager().addCoins(player.getUniqueId(), amount);
        } else {
            giveItem(player, String.valueOf(reward.get("material")), amount);
        }
        player.sendMessage(Message.of(msgs(), "crate-rolled").replace("%reward%", name));
    }

    private void giveItem(Player player, String name, int amount) {
        Material material = Material.getMaterial(name.toUpperCase());
        if (material == null) {
            return;
        }
        ItemStack item = new ItemStack(material, amount);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    private Map<?, ?> pickReward(List<Map<?, ?>> rewards) {
        int total = 0;
        for (Map<?, ?> reward : rewards) {
            total += ((Number) reward.get("weight")).intValue();
        }
        int roll = new Random().nextInt(total);
        for (Map<?, ?> reward : rewards) {
            roll -= ((Number) reward.get("weight")).intValue();
            if (roll < 0) {
                return reward;
            }
        }
        return rewards.get(rewards.size() - 1);
    }

    private static List<Kit> sortedKits(KitPvPPlugin plugin) {
        List<Kit> kits = new ArrayList<Kit>(plugin.getKitManager().getKits());
        kits.sort(Comparator.comparingDouble(Kit::getPrice));
        return kits;
    }

    private static int totalPages(List<Kit> kits) {
        return Math.max(1, (kits.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private static ItemStack kitIcon(Player player, KitPvPPlugin plugin, Kit kit) {
        ItemStack icon = new ItemStack(kit.getIcon());
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(Message.color(kit.getDisplayName()));
        List<String> lore = new ArrayList<String>();
        for (String line : kit.getDescription()) {
            lore.add(Message.color(line));
        }
        lore.add(Message.color(stateLine(player, plugin, kit)));
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private static String stateLine(Player player, KitPvPPlugin plugin, Kit kit) {
        if (plugin.getKitStore().owns(player.getUniqueId(), kit.getId())) {
            return "&aOwned";
        }
        if (kit.getPrice() > 0) {
            return "&6Price: &f%price% &6coins".replace("%price%", Format.number(kit.getPrice()));
        }
        return "&aFree";
    }

    private static ItemStack crateButton(KitPvPPlugin plugin) {
        int cost = plugin.getConfig().getInt("crate-cost", 50);
        return named(Material.CHEST, "&dCrate",
                "&7Cost: &e%cost% &7coins".replace("%cost%", String.valueOf(cost)),
                "&7Random rewards: coins,",
                "&7diamonds, netherite,",
                "&7golden apples, pearls");
    }

    private static ItemStack pageIndicator(int page, int total) {
        return named(Material.PAPER, "&fPage " + (page + 1) + "/" + total);
    }

    private static ItemStack filler() {
        return named(Material.GLASS_PANE, " ");
    }

    private static ItemStack named(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Message.color(name));
        if (loreLines.length > 0) {
            List<String> lore = new ArrayList<String>();
            for (String line : loreLines) {
                lore.add(Message.color(line));
            }
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ConfigurationSection msgs() {
        return plugin.getConfig().getConfigurationSection("messages");
    }
}
