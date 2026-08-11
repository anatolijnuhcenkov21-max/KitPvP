package com.crystalox.kitpvp.commands;

import com.crystalox.kitpvp.KitPvPPlugin;
import com.crystalox.kitpvp.economy.EconomyBridge;
import com.crystalox.kitpvp.kit.Kit;
import com.crystalox.kitpvp.kit.KitApplier;
import com.crystalox.kitpvp.util.Format;
import com.crystalox.kitpvp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class KitCommand implements CommandExecutor, Listener {

    private static final String GUI_TITLE = "Kits";
    private static final int GUI_SIZE = 54;
    private static final int MAX_KITS = 45;

    private final KitPvPPlugin plugin;

    public KitCommand(KitPvPPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Message.color("&cOnly players can use this command."));
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
        giveKit(player, kit);
        return true;
    }

    private void giveKit(Player player, Kit kit) {
        if (kit.getPermission() != null && !player.hasPermission(kit.getPermission())) {
            player.sendMessage(msg("kit-no-permission").replace("%kit%", kit.getId()));
            return;
        }
        if (plugin.getKitCooldownManager().hasCooldown(player.getUniqueId(), kit.getId())) {
            sendCooldownMessage(player, kit);
            return;
        }
        if (kit.getPrice() > 0 && !buyKit(player, kit)) {
            return;
        }
        KitApplier.apply(player, kit);
        plugin.getKitCooldownManager().set(player.getUniqueId(), kit.getId(), kit.getCooldownSeconds());
        player.sendMessage(msg("kit-given")
                .replace("%kit%", kit.getId())
                .replace("%player%", player.getName()));
    }

    private boolean buyKit(Player player, Kit kit) {
        if (EconomyBridge.withdraw(player, kit.getPrice())) {
            player.sendMessage(msg("kit-bought")
                    .replace("%kit%", kit.getId())
                    .replace("%price%", Format.number(kit.getPrice())));
            return true;
        }
        player.sendMessage(msg("kit-cannot-afford").replace("%kit%", kit.getId()));
        return false;
    }

    private void sendCooldownMessage(Player player, Kit kit) {
        player.sendMessage(msg("kit-cooldown")
                .replace("%kit%", kit.getId())
                .replace("%time%", plugin.getKitCooldownManager().format(player.getUniqueId(), kit.getId())));
    }

    private void openKitGui(Player player) {
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
        lore.add(Message.color(stateLine(player, kit)));
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private String stateLine(Player player, Kit kit) {
        if (kit.getPermission() != null && !player.hasPermission(kit.getPermission())) {
            return "&cLocked";
        }
        if (plugin.getKitCooldownManager().hasCooldown(player.getUniqueId(), kit.getId())) {
            return "&cCooldown: " + plugin.getKitCooldownManager().format(player.getUniqueId(), kit.getId());
        }
        if (kit.getPrice() > 0) {
            return "&6Price: " + Format.number(kit.getPrice());
        }
        return "&aAvailable";
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
                player.closeInventory();
                giveKit(player, kit);
                return;
            }
        }
    }

    private String msg(String key) {
        return Message.of(plugin.getConfig().getConfigurationSection("messages"), key);
    }
}
