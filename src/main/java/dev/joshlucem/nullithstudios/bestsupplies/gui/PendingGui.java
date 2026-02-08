package dev.joshlucem.nullithstudios.bestsupplies.gui;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.model.PendingEntry;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import dev.joshlucem.nullithstudios.bestsupplies.util.JsonUtil;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PendingGui extends BaseGui {

    private List<PendingEntry> entries;

    public PendingGui(BestSupplies plugin, Player player) {
        super(plugin, player);
        init(plugin.getConfigManager().getGuiTitle("pending"), 6);
    }

    @Override
    protected void build() {
        fillBorder(plugin.getConfigManager().getDecorationBorder());
        fillEmpty(plugin.getConfigManager().getDecorationFiller());

        // Load entries
        entries = plugin.getPendingService().getPendingEntries(player);

        if (entries.isEmpty()) {
            // Empty state
            buildEmptyState();
        } else {
            // Build entry items
            buildEntryItems();
        }

        // Back button (slot 49)
        setItem(49, createBackButton(), event -> {
            plugin.getGuiManager().openHub(player);
        });

        // Withdraw all button (slot 45) if there are entries
        if (!entries.isEmpty()) {
            buildWithdrawAllButton();
        }
    }

    private void buildEmptyState() {
        ItemStack item = ItemParser.createItem(
            Material.BARRIER,
            "<gray>Sin entregas pendientes</gray>",
            List.of(
                plugin.getConfigManager().getMessage("pending.empty")
            )
        );
        setItem(22, item);
    }

    private void buildEntryItems() {
        // Available slots for entries (10-16, 19-25, 28-34, 37-43)
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 
                       19, 20, 21, 22, 23, 24, 25,
                       28, 29, 30, 31, 32, 33, 34,
                       37, 38, 39, 40, 41, 42, 43};

        int slotIndex = 0;
        for (PendingEntry entry : entries) {
            if (slotIndex >= slots.length) {
                break; // Max entries shown
            }

            int slot = slots[slotIndex];
            ItemStack item = createEntryItem(entry);
            
            final PendingEntry clickEntry = entry;
            setItem(slot, item, event -> {
                if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    // Shift-click: withdraw all
                    withdrawAll();
                } else {
                    // Normal click: withdraw this entry
                    withdrawEntry(clickEntry);
                }
            });

            slotIndex++;
        }

        // Show count if more entries than slots
        if (entries.size() > slots.length) {
            int remaining = entries.size() - slots.length;
            ItemStack moreItem = ItemParser.createItem(
                Material.PAPER,
                "<yellow>+" + remaining + " más...</yellow>",
                List.of("<gray>Retira algunos items para ver más.</gray>")
            );
            // Put in last available slot
            setItem(slots[slots.length - 1], moreItem);
        }
    }

    private ItemStack createEntryItem(PendingEntry entry) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%id%", String.valueOf(entry.getId()));

        Material material;
        List<String> lore = new ArrayList<>();

        if (entry.getType() == PendingEntry.PendingType.ITEM) {
            // Parse items to show preview
            List<ItemStack> items = JsonUtil.deserializeItems(entry.getPayload());
            
            if (!items.isEmpty()) {
                material = items.get(0).getType();
                lore.add("<gray>Contenido:</gray>");
                
                int shown = 0;
                for (ItemStack item : items) {
                    if (shown >= 5) {
                        lore.add("<gray>... y " + (items.size() - shown) + " más</gray>");
                        break;
                    }
                    String name = item.getType().name().replace("_", " ").toLowerCase();
                    lore.add("<gray>- " + item.getAmount() + "x " + capitalizeFirst(name) + "</gray>");
                    shown++;
                }
            } else {
                material = Material.CHEST;
                lore.add("<gray>Items</gray>");
            }
        } else if (entry.getType() == PendingEntry.PendingType.CHEQUE) {
            material = Material.PAPER;
            JsonUtil.ChequePayload cheque = JsonUtil.deserializeCheque(entry.getPayload());
            if (cheque != null) {
                lore.add("<gray>Cheque por: <gold>$" + Text.formatMoney(cheque.amount()) + "</gold></gray>");
                lore.add("<gray>Semana: " + cheque.weekKey() + "</gray>");
            } else {
                lore.add("<gray>Cheque</gray>");
            }
        } else {
            material = Material.CHEST;
            lore.add("<gray>Entrega</gray>");
        }

        lore.add("");
        lore.addAll(plugin.getConfigManager().getMessageList("gui.pending.withdraw-lore"));

        return ItemParser.createItem(
            material,
            plugin.getConfigManager().getMessage("gui.pending.item-entry", placeholders),
            lore,
            null
        );
    }

    private void buildWithdrawAllButton() {
        ItemStack item = ItemParser.createItem(
            Material.HOPPER,
            "<green>Retirar Todo</green>",
            List.of(
                "<gray>Retira todas las entregas</gray>",
                "<gray>pendientes (si hay espacio).</gray>"
            )
        );

        setItem(45, item, event -> withdrawAll());
    }

    private void withdrawEntry(PendingEntry entry) {
        boolean success = plugin.getPendingService().withdrawPending(player, entry);
        
        // Refresh the GUI
        build();
    }

    private void withdrawAll() {
        int withdrawn = plugin.getPendingService().withdrawAllPending(player);
        
        // Refresh the GUI
        build();
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
