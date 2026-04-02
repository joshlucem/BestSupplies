package dev.joshlucem.nullithstudios.bestsupplies.gui;

import dev.joshlucem.nullithstudios.balance.api.BalanceApi;
import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.model.PendingEntry;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import dev.joshlucem.nullithstudios.bestsupplies.util.JsonUtil;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PendingGui extends BaseGui {

    private static final int[] BLACK_SLOTS = {0, 2, 8, 45, 47, 53};
    private static final int[] GRAY_SLOTS = {1, 3, 4, 5, 6, 7, 9, 11, 17, 18, 20, 26, 27, 29, 35, 36, 38, 44, 46, 48, 49, 50, 51, 52};
    private static final int[] CONTENT_SLOTS = {12, 13, 14, 15, 16, 21, 22, 23, 24, 25, 30, 31, 32, 33, 34, 39, 40, 41, 42, 43};

    private static final int BACK_SLOT = 10;
    private static final int PREV_SLOT = 19;
    private static final int NEXT_SLOT = 28;
    private static final int INFO_SLOT = 37;

    private List<PendingEntry> entries = List.of();
    private int page = 0;

    public PendingGui(BestSupplies plugin, Player player) {
        super(plugin, player);
        init(plugin.getConfigManager().getGuiTitle("pending"), 6);
    }

    @Override
    protected void build() {
        fillEmpty(Material.WHITE_STAINED_GLASS_PANE);
        setSlots(BLACK_SLOTS, ItemParser.createFiller(Material.BLACK_STAINED_GLASS_PANE));
        setSlots(GRAY_SLOTS, ItemParser.createFiller(Material.GRAY_STAINED_GLASS_PANE));

        entries = plugin.getPendingService().getPendingEntries(player);
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / CONTENT_SLOTS.length));

        if (page >= totalPages) {
            page = Math.max(0, totalPages - 1);
        }

        buildEntries();
        buildInfo(totalPages);
        buildNavigation(totalPages);
        buildBackButton();
    }

    private void buildEntries() {
        int start = page * CONTENT_SLOTS.length;

        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            int slot = CONTENT_SLOTS[i];
            int index = start + i;

            if (index >= entries.size()) {
                setItem(slot, ItemParser.createFiller(Material.WHITE_STAINED_GLASS_PANE));
                continue;
            }

            PendingEntry entry = entries.get(index);
            setItem(slot, createEntryItem(entry), event -> {
                plugin.getPendingService().withdrawPending(player, entry);
                rebuild();
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
                lore.add("<gray>Cheque: <gold>" + Text.formatCurrency(BalanceApi.CurrencyType.SILVER, cheque.amount()) + "</gold></gray>");
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

    private void buildInfo(int totalPages) {
        Map<String, String> placeholders = Map.of(
                "%bestsupplies_rank_tag%", plugin.getRankService().getRankTag(player),
                "%page%", String.valueOf(page + 1),
                "%total%", String.valueOf(totalPages),
                "%count%", String.valueOf(entries.size())
        );

        ItemStack info = ItemParser.createItem(
                Material.BOOK,
                plugin.getConfigManager().getMessage("gui.pending.info-item", placeholders),
                getMessageList("gui.pending.info-lore", placeholders),
                null
        );
        setItem(INFO_SLOT, info);
    }

    private void buildNavigation(int totalPages) {
        String backId = plugin.getConfigManager().getItemsAdderIcon("nav-back", "_iainternal:icon_back_orange");
        String nextId = plugin.getConfigManager().getItemsAdderIcon("nav-next", "_iainternal:icon_next_orange");
        String cancelId = plugin.getConfigManager().getItemsAdderIcon("nav-cancel", "_iainternal:icon_cancel");

        if (page > 0) {
            setItem(
                    PREV_SLOT,
                    createIconItem(backId, Material.ARROW,
                            plugin.getConfigManager().getMessage("gui.common.prev-page-item"),
                            plugin.getConfigManager().getMessageList("gui.common.prev-page-lore")),
                    event -> {
                        page--;
                        rebuild();
                    }
            );
        } else {
            setItem(PREV_SLOT, createIconItem(cancelId, Material.BARRIER,
                    plugin.getConfigManager().getMessage("gui.common.no-prev-item"),
                    plugin.getConfigManager().getMessageList("gui.common.no-prev-lore")));
        }

        if (page + 1 < totalPages) {
            setItem(
                    NEXT_SLOT,
                    createIconItem(nextId, Material.ARROW,
                            plugin.getConfigManager().getMessage("gui.common.next-page-item"),
                            plugin.getConfigManager().getMessageList("gui.common.next-page-lore")),
                    event -> {
                        page++;
                        rebuild();
                    }
            );
        } else {
            setItem(NEXT_SLOT, createIconItem(cancelId, Material.BARRIER,
                    plugin.getConfigManager().getMessage("gui.common.no-next-item"),
                    plugin.getConfigManager().getMessageList("gui.common.no-next-lore")));
        }
    }

    private void buildBackButton() {
        ItemStack back = ItemParser.createItem(
                Material.CHEST_MINECART,
                plugin.getConfigManager().getMessage("gui.pending.back-item"),
                plugin.getConfigManager().getMessageList("gui.pending.back-lore")
        );
        setItem(BACK_SLOT, back, event -> plugin.getGuiManager().openHub(player));
    }
}
