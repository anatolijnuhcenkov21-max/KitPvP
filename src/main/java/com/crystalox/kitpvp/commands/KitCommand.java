package com.crystalox.kitpvp.commands;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.kit.Kit;
import com.crystalox.kitpvp.shop.KitSelection;
import com.crystalox.kitpvp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

public class KitCommand implements CommandExecutor, Listener {

    private static final String GUI_TITLE = "Выбор класса";
    private static final int GUI_SIZE = 54;
    private static final int MAX_KITS = 45;

    public static KitCommand INSTANCE;

    private final KitPvPPlugin plugin;

    public KitCommand(KitPvPPlugin plugin) {
        KitCommand.INSTANCE = this;
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Message.color("&cТолько игроки могут использовать эту команду."));
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            openKitGui(player);
            return true;
        }
        Kit kit = plugin.getKitManager().getKit(args[0]);
        if (kit == null) {
            player.sendMessage(msg("kit-not-found").replace("%kit%", args[0]));
            return true;
        }
        selectKit(player, kit);
        return true;
    }

    private void selectKit(Player player, Kit kit) {
        if (kit.getPermission() != null && !player.hasPermission(kit.getPermission())) {
            player.sendMessage(msg("kit-no-permission").replace("%kit%", kit.getId()));
            return;
        }
        if (plugin.getKitCooldownManager().hasCooldown(player.getUniqueId(), kit.getId())) {
            sendCooldownMessage(player, kit);
            return;
        }
        if (isLocked(player, kit)) {
            player.sendMessage(Message.color(msg("class-locked").replace("%kit%", kit.getDisplayName())));
            return;
        }
        select(player, kit);
    }

    private void select(Player player, Kit kit) {
        KitSelection.set(player.getUniqueId(), kit.getId());
        plugin.getKitCooldownManager().set(player.getUniqueId(), kit.getId(), kit.getCooldownSeconds());
        player.sendMessage(Message.color(msg("class-selected").replace("%kit%", kit.getDisplayName())));
    }

    private void sendCooldownMessage(Player player, Kit kit) {
        player.sendMessage(msg("kit-cooldown")
                .replace("%kit%", kit.getId())
                .replace("%time%", plugin.getKitCooldownManager().format(player.getUniqueId(), kit.getId())));
    }

    public void openKitGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        int index = 0;
        for (Kit kit : plugin.getKitManager().getKits()) {
            if (index >= MAX_KITS) {
                break;
            }
            gui.setItem(index++, iconFor(player, kit));
        }
        player.openInventory(gui);
    }

    private ItemStack iconFor(Player player, Kit kit) {
        ItemStack icon = new ItemStack(kit.getIcon());
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(Message.color(kit.getDisplayName()));
        List<String> lore = new ArrayList<String>();
        for (String line : kit.getDescription()) {
            lore.add(Message.color(line));
        }
        if (isLocked(player, kit)) {
            lore.add(Message.color("&cЗаблокирован"));
            lore.add(Message.color("&7Открой в кейсах"));
        } else {
            lore.add(Message.color(stateLine(player, kit)));
        }
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private boolean isLocked(Player player, Kit kit) {
        return kit.getPrice() > 0 && !plugin.getKitStore().owns(player.getUniqueId(), kit.getId());
    }

    private String stateLine(Player player, Kit kit) {
        if (plugin.getKitStore().owns(player.getUniqueId(), kit.getId())) {
            return "&aОткрыт";
        }
        if (kit.getPrice() > 0) {
            return "&cЗаблокирован";
        }
        return "&aБесплатный";
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }
        String displayName = item.getItemMeta().getDisplayName();
        Player player = (Player) event.getWhoClicked();
        for (Kit kit : plugin.getKitManager().getKits()) {
            if (Message.color(kit.getDisplayName()).equals(displayName)) {
                if (isLocked(player, kit)) {
                    player.sendMessage(Message.color(msg("class-locked").replace("%kit%", kit.getDisplayName())));
                    player.closeInventory();
                    return;
                }
                select(player, kit);
                player.closeInventory();
                return;
            }
        }
    }

    private String msg(String key) {
        return Message.of(plugin.getConfig().getConfigurationSection("messages"), key);
    }

    public static ItemStack selectorItem() {
        ItemStack item = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Message.color("&6&lВыбор класса"));
        List<String> lore = new ArrayList<String>();
        lore.add(Message.color("&7ПКМ — выбрать класс"));
        meta.setLore(lore);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isSelector(held)) {
            return;
        }
        event.setCancelled(true);
        openKitGui(player);
    }

    private boolean isSelector(ItemStack item) {
        if (item == null || item.getType() != Material.WOODEN_SWORD) {
            return false;
        }
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        return ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Выбор класса");
    }
}

