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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PendingGui extends BaseGui {

    private static final int[] ENTRY_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    private static final int PREV_PAGE_SLOT = 45;
    private static final int WITHDRAW_ALL_SLOT = 46;
    private static final int PAGE_INFO_SLOT = 47;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 53;

    private List<PendingEntry> entries = List.of();
    private int page = 0;

    public PendingGui(BestSupplies plugin, Player player) {
        super(plugin, player);
        init(plugin.getConfigManager().getGuiTitle("pending"), 6);
    }

    @Override
    protected void build() {
        fillBorder(plugin.getConfigManager().getDecorationBorder());
        fillEmpty(plugin.getConfigManager().getDecorationFiller());

        entries = plugin.getPendingService().getPendingEntries(player);
        int totalPages = getTotalPages();

        if (page >= totalPages) {
            page = Math.max(0, totalPages - 1);
        }

        if (entries.isEmpty()) {
            buildEmptyState();
        } else {
            buildEntryItems();
        }

        buildControls(totalPages);
    }

    private int getTotalPages() {
        if (entries.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil((double) entries.size() / ENTRY_SLOTS.length);
    }

    private void buildEmptyState() {
        ItemStack item = ItemParser.createItem(
            Material.BARRIER,
            plugin.getConfigManager().getMessage("gui.pending.empty-item"),
            List.of(plugin.getConfigManager().getMessage("pending.empty"))
        );
        setItem(22, item);
    }

    private void buildEntryItems() {
        int startIndex = page * ENTRY_SLOTS.length;
        int endIndex = Math.min(startIndex + ENTRY_SLOTS.length, entries.size());

        int slotCursor = 0;
        for (int index = startIndex; index < endIndex; index++) {
            PendingEntry entry = entries.get(index);
            int slot = ENTRY_SLOTS[slotCursor++];

            setItem(slot, createEntryItem(entry), event -> {
                if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    withdrawAll();
                } else {
                    withdrawEntry(entry);
                }
            });
        }
    }

    private ItemStack createEntryItem(PendingEntry entry) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%id%", String.valueOf(entry.getId()));

        Material material;
        List<String> lore = new ArrayList<>();

        if (entry.getType() == PendingEntry.PendingType.ITEM) {
            List<ItemStack> items = JsonUtil.deserializeItems(entry.getPayload());

            if (!items.isEmpty()) {
                material = items.get(0).getType();
                lore.add("<gray>Contenido:</gray>");

                int shown = 0;
                for (ItemStack item : items) {
                    if (shown >= 5) {
                        lore.add("<gray>... y " + (items.size() - shown) + " mas</gray>");
                        break;
                    }
                    String itemName = plugin.getConfigManager().getItemName(item.getType());
                    lore.add("<gray>- " + item.getAmount() + "x " + itemName + "</gray>");
                    shown++;
                }
            } else {
                material = Material.CHEST;
                lore.add("<gray>Items sin detalle.</gray>");
            }
        } else if (entry.getType() == PendingEntry.PendingType.CHEQUE) {
            material = Material.PAPER;
            JsonUtil.ChequePayload cheque = JsonUtil.deserializeCheque(entry.getPayload());
            if (cheque != null) {
                lore.add("<gray>Cheque: <gold>$" + Text.formatMoney(cheque.amount()) + "</gold></gray>");
                lore.add("<gray>Periodo: " + cheque.weekKey() + "</gray>");
            } else {
                lore.add("<gray>Cheque sin datos.</gray>");
            }
        } else {
            material = Material.CHEST;
            lore.add("<gray>Entrega pendiente.</gray>");
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

    private void buildControls(int totalPages) {
        setItem(BACK_SLOT, createBackButton(), event -> plugin.getGuiManager().openHub(player));

        ItemStack pageInfo = ItemParser.createItem(
            Material.BOOK,
            plugin.getConfigManager().getMessage("gui.pending.page-item", Map.of(
                "%page%", String.valueOf(page + 1),
                "%total%", String.valueOf(totalPages)
            )),
            plugin.getConfigManager().getMessageList("gui.pending.page-lore")
        );
        setItem(PAGE_INFO_SLOT, pageInfo);

        if (!entries.isEmpty()) {
            ItemStack withdrawAll = ItemParser.createItem(
                Material.HOPPER,
                plugin.getConfigManager().getMessage("gui.pending.withdraw-all-item"),
                plugin.getConfigManager().getMessageList("gui.pending.withdraw-all-lore")
            );
            setItem(WITHDRAW_ALL_SLOT, withdrawAll, event -> withdrawAll());
        } else {
            setItem(WITHDRAW_ALL_SLOT, ItemParser.createFiller(plugin.getConfigManager().getDecorationFiller()));
        }

        if (page > 0) {
            ItemStack prev = ItemParser.createItem(
                Material.ARROW,
                plugin.getConfigManager().getMessage("gui.pending.prev-page-item"),
                null
            );
            setItem(PREV_PAGE_SLOT, prev, event -> {
                page--;
                rebuild();
            });
        }

        if (page + 1 < totalPages) {
            ItemStack next = ItemParser.createItem(
                Material.ARROW,
                plugin.getConfigManager().getMessage("gui.pending.next-page-item"),
                null
            );
            setItem(NEXT_PAGE_SLOT, next, event -> {
                page++;
                rebuild();
            });
        }
    }

    private void withdrawEntry(PendingEntry entry) {
        plugin.getPendingService().withdrawPending(player, entry);
        rebuild();
    }

    private void withdrawAll() {
        plugin.getPendingService().withdrawAllPending(player);
        rebuild();
    }
}
