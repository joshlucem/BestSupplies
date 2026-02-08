package dev.joshlucem.nullithstudios.bestsupplies.gui;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class HubGui extends BaseGui {

    public HubGui(BestSupplies plugin, Player player) {
        super(plugin, player);
        init(plugin.getConfigManager().getGuiTitle("hub"), 6);
    }

    @Override
    protected void build() {
        // Fill with decoration
        fillBorder(plugin.getConfigManager().getDecorationBorder());
        fillEmpty(plugin.getConfigManager().getDecorationFiller());

        // Status button (slot 13)
        int statusSlot = getSlot("hub", "status", 13);
        ItemStack statusItem = ItemParser.createItem(
            Material.PLAYER_HEAD,
            plugin.getConfigManager().getMessage("gui.hub.status-item"),
            plugin.getConfigManager().getMessageList("gui.hub.status-lore")
        );
        setItem(statusSlot, statusItem, event -> {
            if (player.hasPermission("bestsupplies.status")) {
                plugin.getGuiManager().openStatus(player);
            }
        });

        // Daily button (slot 21)
        int dailySlot = getSlot("hub", "daily", 21);
        ItemStack dailyItem = ItemParser.createItem(
            Material.CHEST,
            plugin.getConfigManager().getMessage("gui.hub.daily-item"),
            plugin.getConfigManager().getMessageList("gui.hub.daily-lore")
        );
        setItem(dailySlot, dailyItem, event -> {
            if (player.hasPermission("bestsupplies.daily")) {
                plugin.getGuiManager().openDaily(player);
            }
        });

        // Bank button (slot 23)
        int bankSlot = getSlot("hub", "bank", 23);
        ItemStack bankItem = ItemParser.createItem(
            Material.GOLD_INGOT,
            plugin.getConfigManager().getMessage("gui.hub.bank-item"),
            plugin.getConfigManager().getMessageList("gui.hub.bank-lore")
        );
        setItem(bankSlot, bankItem, event -> {
            if (player.hasPermission("bestsupplies.bank")) {
                plugin.getGuiManager().openBank(player);
            }
        });

        // Food button (slot 31)
        int foodSlot = getSlot("hub", "food", 31);
        ItemStack foodItem = ItemParser.createItem(
            Material.COOKED_BEEF,
            plugin.getConfigManager().getMessage("gui.hub.food-item"),
            plugin.getConfigManager().getMessageList("gui.hub.food-lore")
        );
        setItem(foodSlot, foodItem, event -> {
            if (player.hasPermission("bestsupplies.food")) {
                plugin.getGuiManager().openFood(player);
            }
        });

        // Close button (slot 49)
        int closeSlot = getSlot("hub", "close", 49);
        setItem(closeSlot, createCloseButton(), event -> {
            player.closeInventory();
        });
    }
}
