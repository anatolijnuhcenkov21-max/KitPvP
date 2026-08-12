package com.crystalox.kitpvp.shop;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.kit.Kit;
import com.crystalox.kitpvp.stats.StatsManager;
import com.crystalox.kitpvp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
import java.util.Random;
import java.util.UUID;

public class ShopGui implements Listener {

    private static final String SHOP_TITLE = "Магазин";

    private final KitPvPPlugin plugin;

    public ShopGui(KitPvPPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static ItemStack diamondItem() {
        return named(Material.DIAMOND, "&b&lМагазин", "&7ПКМ — открыть магазин");
    }

    public static ItemStack coinItem(int amount) {
        return named(Material.GOLD_INGOT, "&6Монеты: &f" + amount);
    }

    public static void open(Player player, KitPvPPlugin plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, Message.color("&8" + SHOP_TITLE));
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler());
        }
        inv.setItem(4, infoBook());
        inv.setItem(13, crateButton(player, plugin));
        inv.setItem(16, dailyButton(plugin));
        inv.setItem(22, coinItem(plugin.getStatsManager().getCoins(player.getUniqueId())));
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
        if (event.getSlot() == 13) {
            rollCrate((Player) event.getWhoClicked(), plugin);
        } else if (event.getSlot() == 16) {
            claimDaily((Player) event.getWhoClicked());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
        if (!isShopDiamond(held)) {
            return;
        }
        event.setCancelled(true);
        open(event.getPlayer(), plugin);
    }

    private boolean isShopDiamond(ItemStack held) {
        if (held == null || held.getType() != Material.DIAMOND || !held.hasItemMeta() || !held.getItemMeta().hasDisplayName()) {
            return false;
        }
        return ChatColor.stripColor(held.getItemMeta().getDisplayName()).equals(SHOP_TITLE);
    }

    private void rollCrate(Player player, KitPvPPlugin plugin) {
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
        int cost = crateCost(player, plugin);
        StatsManager stats = plugin.getStatsManager();
        UUID uuid = player.getUniqueId();
        if (!stats.spendCoins(uuid, cost)) {
            player.sendMessage(Message.of(msgs(), "crate-no-funds").replace("%cost%", String.valueOf(cost)));
            return;
        }
        plugin.getKitStore().incCrateCount(uuid);
        grantReward(player, plugin, pickReward(plugin.getConfig().getMapList("crate-rewards")));
        plugin.getQuestManager().onCrate(player.getUniqueId());
        player.closeInventory();
    }

    private void grantReward(Player player, KitPvPPlugin plugin, Map<?, ?> reward) {
        String type = String.valueOf(reward.get("type")).toUpperCase();
        String name = String.valueOf(reward.get("name"));
        if ("CLASS".equals(type)) {
            grantClass(player, plugin);
            return;
        }
        int amount = ((Number) reward.get("amount")).intValue();
        if ("COINS".equals(type)) {
            plugin.getStatsManager().addCoins(player.getUniqueId(), amount);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        } else {
            giveItem(player, String.valueOf(reward.get("material")), amount);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        }
        player.sendMessage(Message.of(msgs(), "crate-rolled").replace("%reward%", name));
    }

    private void grantClass(Player player, KitPvPPlugin plugin) {
        Kit kit = randomLockedKit(player, plugin);
        if (kit == null) {
            fallbackCoins(player, plugin);
            return;
        }
        plugin.getKitStore().buy(player.getUniqueId(), kit.getId());
        player.sendMessage(Message.of(msgs(), "class-unlocked").replace("%kit%", kit.getDisplayName()));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
        player.closeInventory();
    }

    private void fallbackCoins(Player player, KitPvPPlugin plugin) {
        for (Map<?, ?> reward : plugin.getConfig().getMapList("crate-rewards")) {
            if ("COINS".equals(String.valueOf(reward.get("type")).toUpperCase())) {
                plugin.getStatsManager().addCoins(player.getUniqueId(), ((Number) reward.get("amount")).intValue());
                break;
            }
        }
        player.sendMessage(Message.color("&cВсе классы открыты!"));
        player.closeInventory();
    }

    private void claimDaily(Player player) {
        UUID uuid = player.getUniqueId();
        int amount = plugin.getConfig().getInt("daily-bonus", 25);
        if (plugin.getKitStore().claimDaily(uuid)) {
            plugin.getStatsManager().addCoins(uuid, amount);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.sendMessage(Message.color("&aЕжедневный бонус: &6+" + amount + " &aмонет!"));
        } else {
            player.sendMessage(Message.color("&cБонус уже получен! Заходи завтра."));
        }
        player.closeInventory();
    }

    private Kit randomLockedKit(Player player, KitPvPPlugin plugin) {
        List<Kit> locked = new ArrayList<Kit>();
        for (Kit kit : plugin.getKitManager().getKits()) {
            if (kit.getPrice() > 0 && !plugin.getKitStore().owns(player.getUniqueId(), kit.getId())) {
                locked.add(kit);
            }
        }
        if (locked.isEmpty()) {
            return null;
        }
        return locked.get(new Random().nextInt(locked.size()));
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

    private static ItemStack infoBook() {
        return named(Material.BOOK, "&fКак открывать классы",
                "&7Классы выпадают из кейсов!",
                "&7Купить их нельзя.");
    }

    private static ItemStack crateButton(Player player, KitPvPPlugin plugin) {
        int cost = crateCost(player, plugin);
        return named(Material.CHEST, "&dКейс",
                "&7Цена: &e%cost% &7монет (+10 за каждый кейс)".replace("%cost%", String.valueOf(cost)),
                "&7Награды: монеты, алмазы,",
                "&7незерит, золотые яблоки,",
                "&7и ОТКРЫТИЕ КЛАССОВ!");
    }

    private static int crateCost(Player player, KitPvPPlugin plugin) {
        return plugin.getConfig().getInt("crate-cost", 50) + plugin.getKitStore().getCrateCount(player.getUniqueId()) * 10;
    }

    private static ItemStack dailyButton(KitPvPPlugin plugin) {
        int amount = plugin.getConfig().getInt("daily-bonus", 25);
        return named(Material.SUNFLOWER, "&aЕжедневный бонус",
                "&7+%amount% монет раз в 24 часа".replace("%amount%", String.valueOf(amount)));
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
