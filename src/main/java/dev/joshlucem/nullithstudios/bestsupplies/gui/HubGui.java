package dev.joshlucem.nullithstudios.bestsupplies.gui;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HubGui extends BaseGui {

    private static final int DEFAULT_OVERVIEW_SLOT = 4;
    private static final int DEFAULT_DAILY_SLOT = 20;
    private static final int DEFAULT_BANK_SLOT = 22;
    private static final int DEFAULT_FOOD_SLOT = 24;
    private static final int DEFAULT_STATUS_SLOT = 30;
    private static final int DEFAULT_PENDING_SLOT = 32;
    private static final int DEFAULT_CLOSE_SLOT = 49;

    public HubGui(BestSupplies plugin, Player player) {
        super(plugin, player);
        init(plugin.getConfigManager().getGuiTitle("hub"), 6);
    }

    @Override
    protected void build() {
        fillBorder(plugin.getConfigManager().getDecorationBorder());
        fillEmpty(plugin.getConfigManager().getDecorationFiller());

        buildOverview();
        buildActions();

        int closeSlot = getSlot("hub", "close", DEFAULT_CLOSE_SLOT);
        setItem(closeSlot, createCloseButton(), event -> {
            player.closeInventory();
        });
    }

    private void buildOverview() {
        int overviewSlot = getSlot("hub", "overview", DEFAULT_OVERVIEW_SLOT);
        String rankName = plugin.getRankService().getRankDisplayName(player);
        int streak = plugin.getDailyService().getStreak(player);
        int pendingCount = plugin.getPendingService().getPendingCount(player);
        boolean weeklyClaimed = plugin.getBankService().hasClaimedWeekly(player);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%rank%", rankName);
        placeholders.put("%streak%", String.valueOf(streak));
        placeholders.put("%pending%", String.valueOf(pendingCount));
        placeholders.put("%weekly_status%", weeklyClaimed
            ? plugin.getConfigManager().getMessage("bank.claimed")
            : plugin.getConfigManager().getMessage("bank.available"));

        ItemStack overviewItem = ItemParser.createItem(
            Material.BOOK,
            plugin.getConfigManager().getMessage("gui.hub.overview-item"),
            getMessageList("gui.hub.overview-lore", placeholders),
            null
        );
        setItem(overviewSlot, overviewItem);
    }

    private void buildActions() {
        int dailySlot = getSlot("hub", "daily", DEFAULT_DAILY_SLOT);
        setItem(
            dailySlot,
            ItemParser.createItem(
                Material.CHEST,
                plugin.getConfigManager().getMessage("gui.hub.daily-item"),
                plugin.getConfigManager().getMessageList("gui.hub.daily-lore")
            ),
            event -> {
                if (!player.hasPermission("bestsupplies.daily")) {
                    sendNoPermission();
                    return;
                }
                plugin.getGuiManager().openDaily(player);
            }
        );

        int bankSlot = getSlot("hub", "bank", DEFAULT_BANK_SLOT);
        setItem(
            bankSlot,
            ItemParser.createItem(
                Material.GOLD_INGOT,
                plugin.getConfigManager().getMessage("gui.hub.bank-item"),
                plugin.getConfigManager().getMessageList("gui.hub.bank-lore")
            ),
            event -> {
                if (!player.hasPermission("bestsupplies.bank")) {
                    sendNoPermission();
                    return;
                }
                plugin.getGuiManager().openBank(player);
            }
        );

        int foodSlot = getSlot("hub", "food", DEFAULT_FOOD_SLOT);
        setItem(
            foodSlot,
            ItemParser.createItem(
                Material.COOKED_BEEF,
                plugin.getConfigManager().getMessage("gui.hub.food-item"),
                plugin.getConfigManager().getMessageList("gui.hub.food-lore")
            ),
            event -> {
                if (!player.hasPermission("bestsupplies.food")) {
                    sendNoPermission();
                    return;
                }
                plugin.getGuiManager().openFood(player);
            }
        );

        int statusSlot = getSlot("hub", "status", DEFAULT_STATUS_SLOT);
        setItem(
            statusSlot,
            ItemParser.createItem(
                Material.PLAYER_HEAD,
                plugin.getConfigManager().getMessage("gui.hub.status-item"),
                plugin.getConfigManager().getMessageList("gui.hub.status-lore")
            ),
            event -> {
                if (!player.hasPermission("bestsupplies.status")) {
                    sendNoPermission();
                    return;
                }
                plugin.getGuiManager().openStatus(player);
            }
        );

        int pendingSlot = getSlot("hub", "pending", DEFAULT_PENDING_SLOT);
        Map<String, String> placeholders = Map.of(
            "%count%", String.valueOf(plugin.getPendingService().getPendingCount(player))
        );
        setItem(
            pendingSlot,
            ItemParser.createItem(
                Material.CHEST_MINECART,
                plugin.getConfigManager().getMessage("gui.hub.pending-item"),
                getMessageList("gui.hub.pending-lore", placeholders),
                null
            ),
            event -> {
                if (!player.hasPermission("bestsupplies.pending")) {
                    sendNoPermission();
                    return;
                }
                plugin.getGuiManager().openPending(player);
            }
        );
    }
}
