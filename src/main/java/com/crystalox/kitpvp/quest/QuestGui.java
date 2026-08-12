package com.crystalox.kitpvp.quest;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.kit.Kit;
import com.crystalox.kitpvp.shop.ShopGui;
import com.crystalox.kitpvp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class QuestGui implements Listener {

    private static final String QUEST_TITLE = "Квесты";

    private final KitPvPPlugin plugin;

    public QuestGui(KitPvPPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static void open(Player player, KitPvPPlugin plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, Message.color("&d&l" + QUEST_TITLE));
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler());
        }
        List<Quest> daily = visibleQuests(player, plugin, plugin.getQuestManager().getDaily());
        List<Quest> weekly = visibleQuests(player, plugin, plugin.getQuestManager().getWeekly());
        for (int i = 0; i < daily.size() && i < 9; i++) {
            inv.setItem(i, questItem(player, plugin, daily.get(i)));
        }
        for (int i = 0; i < weekly.size() && i < 9; i++) {
            inv.setItem(i + 9, questItem(player, plugin, weekly.get(i)));
        }
        inv.setItem(22, ShopGui.coinItem(plugin.getStatsManager().getCoins(player.getUniqueId())));
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!QUEST_TITLE.equals(ChatColor.stripColor(event.getView().getTitle()))) {
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
        Quest quest = questAt(player, event.getSlot());
        if (quest == null) {
            return;
        }
        if (plugin.getQuestManager().getProgress(player.getUniqueId(), quest) < quest.getTarget()) {
            player.sendMessage(Message.color("&cКвест ещё не выполнен!"));
            return;
        }
        if (plugin.getQuestManager().claim(player.getUniqueId(), quest)) {
            plugin.getStatsManager().addCoins(player.getUniqueId(), quest.getReward());
            player.sendMessage(Message.color("&aКвест выполнен! +" + quest.getReward() + " монет"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            open(player, plugin);
        } else {
            player.sendMessage(Message.color("&cКвест ещё не выполнен!"));
        }
    }

    private Quest questAt(Player player, int slot) {
        if (slot < 9) {
            return visibleQuest(player, plugin.getQuestManager().getDaily(), slot);
        }
        if (slot >= 9 && slot < 18) {
            return visibleQuest(player, plugin.getQuestManager().getWeekly(), slot - 9);
        }
        return null;
    }

    private Quest visibleQuest(Player player, List<Quest> quests, int index) {
        List<Quest> visible = visibleQuests(player, plugin, quests);
        return index < visible.size() ? visible.get(index) : null;
    }

    private static List<Quest> visibleQuests(Player player, KitPvPPlugin plugin, List<Quest> quests) {
        List<Quest> visible = new ArrayList<Quest>();
        for (Quest quest : quests) {
            if (quest.getKit() == null || plugin.getKitStore().owns(player.getUniqueId(), quest.getKit())) {
                visible.add(quest);
            }
        }
        return visible;
    }

    private static ItemStack questItem(Player player, KitPvPPlugin plugin, Quest quest) {
        int progress = plugin.getQuestManager().getProgress(player.getUniqueId(), quest);
        boolean complete = progress >= quest.getTarget();
        List<String> lore = new ArrayList<String>();
        lore.add(Message.color("&7Прогресс: &f" + progress + "&7/&f" + quest.getTarget()));
        if (quest.getKit() != null) {
            Kit kit = plugin.getKitManager().getKit(quest.getKit());
            String kitName = kit != null ? kit.getDisplayName() : quest.getKit();
            lore.add(Message.color("&7Класс: &f" + kitName));
        }
        lore.add(Message.color("&6Награда: &f" + quest.getReward() + " монет"));
        if (complete) {
            lore.add(Message.color("&aНажми, чтобы забрать!"));
        }
        return named(complete ? Material.SUNFLOWER : Material.PAPER, quest.getName(), lore);
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
