package dev.joshlucem.nullithstudios.bestsupplies.service;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.model.PendingEntry;
import dev.joshlucem.nullithstudios.bestsupplies.storage.Database;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import dev.joshlucem.nullithstudios.bestsupplies.util.JsonUtil;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PendingService {

    private final BestSupplies plugin;
    private final Database database;

    public PendingService(BestSupplies plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    /**
     * Save items to pending deliveries
     */
    public void saveItemsToPending(Player player, List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        String playerUuid = player.getUniqueId().toString();
        String payload = JsonUtil.serializeItems(items);
        
        database.addPending(playerUuid, PendingEntry.PendingType.ITEM, payload);
        plugin.debug("Guardados " + items.size() + " items pendientes para " + player.getName());
        
        // Notify player
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%count%", String.valueOf(items.size()));
        Text.sendPrefixed(player, plugin.getConfigManager().getMessage("pending.saved-to-pending", placeholders), plugin.getConfigManager());
    }

    /**
     * Save a single item to pending
     */
    public void saveItemToPending(Player player, ItemStack item) {
        saveItemsToPending(player, List.of(item));
    }

    /**
     * Save a cheque to pending deliveries
     */
    public void saveChequeToPending(Player player, String chequeId, double amount, String weekKey) {
        String playerUuid = player.getUniqueId().toString();
        String payload = JsonUtil.serializeCheque(chequeId, amount, weekKey);
        
        database.addPending(playerUuid, PendingEntry.PendingType.CHEQUE, payload);
        plugin.debug("Guardado cheque pendiente para " + player.getName() + ": $" + amount);
        
        // Notify player
        Text.sendPrefixed(player, plugin.getConfigManager().getMessage("pending.saved-to-pending"), plugin.getConfigManager());
    }

    /**
     * Get all pending entries for a player
     */
    public List<PendingEntry> getPendingEntries(Player player) {
        return database.getPendingEntries(player.getUniqueId().toString());
    }

    /**
     * Get count of pending entries
     */
    public int getPendingCount(Player player) {
        return database.getPendingCount(player.getUniqueId().toString());
    }

    /**
     * Check if player has pending entries
     */
    public boolean hasPending(Player player) {
        return getPendingCount(player) > 0;
    }

    /**
     * Withdraw a specific pending entry
     * Returns true if successful
     */
    public boolean withdrawPending(Player player, PendingEntry entry) {
        if (entry == null) {
            return false;
        }

        if (entry.getType() == PendingEntry.PendingType.ITEM) {
            return withdrawItemPending(player, entry);
        } else if (entry.getType() == PendingEntry.PendingType.CHEQUE) {
            return withdrawChequePending(player, entry);
        }

        return false;
    }

    /**
     * Withdraw item pending entry
     */
    private boolean withdrawItemPending(Player player, PendingEntry entry) {
        List<ItemStack> items = JsonUtil.deserializeItems(entry.getPayload());
        
        if (items.isEmpty()) {
            // Invalid entry, just remove it
            database.removePending(entry.getId());
            return true;
        }

        // Check if player has space
        if (!ItemParser.hasAnyInventorySpace(player)) {
            Map<String, String> placeholders = new HashMap<>();
            Text.sendPrefixed(player, plugin.getConfigManager().getMessage("pending.inventory-full"), plugin.getConfigManager());
            return false;
        }

        // Give items, any that don't fit stay in pending
        List<ItemStack> notGiven = ItemParser.giveItems(player, items);
        
        if (notGiven.isEmpty()) {
            // All items given, remove pending entry
            database.removePending(entry.getId());
            Text.sendPrefixed(player, plugin.getConfigManager().getMessage("pending.withdraw-success"), plugin.getConfigManager());
            return true;
        } else {
            // Some items couldn't be given, update pending entry
            database.removePending(entry.getId());
            String newPayload = JsonUtil.serializeItems(notGiven);
            database.addPending(player.getUniqueId().toString(), PendingEntry.PendingType.ITEM, newPayload);
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%count%", String.valueOf(notGiven.size()));
            Text.sendPrefixed(player, plugin.getConfigManager().getMessage("pending.inventory-full", placeholders), plugin.getConfigManager());
            return true;
        }
    }

    /**
     * Withdraw cheque pending entry
     */
    private boolean withdrawChequePending(Player player, PendingEntry entry) {
        JsonUtil.ChequePayload cheque = JsonUtil.deserializeCheque(entry.getPayload());
        
        if (cheque == null) {
            // Invalid entry, just remove it
            database.removePending(entry.getId());
            return true;
        }

        // Check if player has inventory space for cheque item
        if (!ItemParser.hasAnyInventorySpace(player)) {
            Text.sendPrefixed(player, plugin.getConfigManager().getMessage("pending.inventory-full"), plugin.getConfigManager());
            return false;
        }

        // Give the cheque item
        ItemStack chequeItem = plugin.getBankService().createChequeItem(player, cheque.amount(), cheque.weekKey(), cheque.chequeId());
        player.getInventory().addItem(chequeItem);
        
        database.removePending(entry.getId());
        Text.sendPrefixed(player, plugin.getConfigManager().getMessage("pending.withdraw-success"), plugin.getConfigManager());
        return true;
    }

    /**
     * Withdraw all pending entries for a player
     * Returns number of entries successfully withdrawn
     */
    public int withdrawAllPending(Player player) {
        List<PendingEntry> entries = getPendingEntries(player);
        int withdrawn = 0;

        for (PendingEntry entry : entries) {
            // Stop if inventory is full
            if (!ItemParser.hasAnyInventorySpace(player)) {
                break;
            }

            if (withdrawPending(player, entry)) {
                withdrawn++;
            }
        }

        if (withdrawn > 0) {
            Text.sendPrefixed(player, plugin.getConfigManager().getMessage("pending.withdraw-all-success"), plugin.getConfigManager());
        }

        return withdrawn;
    }

    /**
     * Remove a pending entry by ID
     */
    public void removePending(int id) {
        database.removePending(id);
    }
}
