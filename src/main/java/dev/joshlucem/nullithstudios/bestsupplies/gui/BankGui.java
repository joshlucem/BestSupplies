package dev.joshlucem.nullithstudios.bestsupplies.gui;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.service.BankService;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BankGui extends BaseGui {

    private static final int[] BLACK_SLOTS = {0, 2, 8, 45, 47, 53};
    private static final int[] GRAY_SLOTS = {1, 3, 4, 5, 6, 7, 9, 11, 17, 18, 20, 26, 27, 29, 35, 36, 38, 44, 46, 48, 49, 50, 51, 52};
    private static final int[] DAY_SLOTS = {12, 13, 14, 15, 16, 21, 22, 23, 24, 25, 30, 31, 32, 33, 34, 39, 40, 41, 42, 43};

    private static final int BACK_SLOT = 10;
    private static final int PREV_SLOT = 19;
    private static final int NEXT_SLOT = 28;
    private static final int INFO_SLOT = 37;

    private int page = 0;

    public BankGui(BestSupplies plugin, Player player) {
        super(plugin, player);
        init(plugin.getConfigManager().getGuiTitle("bank"), 6);
    }

    @Override
    protected void build() {
        fillEmpty(Material.WHITE_STAINED_GLASS_PANE);
        setSlots(BLACK_SLOTS, ItemParser.createFiller(Material.BLACK_STAINED_GLASS_PANE));
        setSlots(GRAY_SLOTS, ItemParser.createFiller(Material.GRAY_STAINED_GLASS_PANE));

        List<LocalDate> monthDays = getCurrentMonthDays();
        int totalPages = Math.max(1, (int) Math.ceil((double) monthDays.size() / DAY_SLOTS.length));
        if (page >= totalPages) {
            page = Math.max(0, totalPages - 1);
        }

        buildMonthDays(monthDays);
        buildInfo(monthDays.size(), totalPages);
        buildNavigation(totalPages);
        buildBackButton();
    }

    private List<LocalDate> getCurrentMonthDays() {
        LocalDate today = plugin.getTimeService().getCurrentDate();
        YearMonth yearMonth = YearMonth.from(today);

        List<LocalDate> days = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            days.add(yearMonth.atDay(day));
        }
        return days;
    }

    private void buildMonthDays(List<LocalDate> monthDays) {
        int start = page * DAY_SLOTS.length;

        for (int i = 0; i < DAY_SLOTS.length; i++) {
            int slot = DAY_SLOTS[i];
            int index = start + i;

            if (index >= monthDays.size()) {
                setItem(slot, ItemParser.createFiller(Material.WHITE_STAINED_GLASS_PANE));
                continue;
            }

            LocalDate date = monthDays.get(index);
            BankService.MonthlyDayStatus status = plugin.getBankService().getMonthlyDayStatus(player, date);

            if (status == BankService.MonthlyDayStatus.AVAILABLE) {
                setItem(slot, createDayItem(date, status), event -> {
                    BankService.MonthlyClaimResult result = plugin.getBankService().claimMonthlyDay(player, date);
                    if (result == BankService.MonthlyClaimResult.SUCCESS) {
                        rebuild();
                    }
                });
            } else {
                setItem(slot, createDayItem(date, status));
            }
        }
    }

    private ItemStack createDayItem(LocalDate date, BankService.MonthlyDayStatus status) {
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es")).toLowerCase(Locale.ROOT);
        Map<String, String> placeholders = Map.of(
                "%day%", String.valueOf(date.getDayOfMonth()),
                "%month%", monthName,
                "%amount%", Text.formatMoney(plugin.getBankService().getMonthlyAmount(player, date))
        );

        if (status == BankService.MonthlyDayStatus.LOCKED) {
            return createIconItem(
                    plugin.getConfigManager().getItemsAdderIcon("lock", "mcicons:icon_lock"),
                    Material.BARRIER,
                    plugin.getConfigManager().getMessage("gui.bank.day-locked", placeholders),
                    plugin.getConfigManager().getMessageList("gui.bank.day-locked-lore")
            );
        }

        if (status == BankService.MonthlyDayStatus.CLAIMED) {
            return ItemParser.createItem(
                    Material.PAPER,
                    plugin.getConfigManager().getMessage("gui.bank.day-claimed", placeholders),
                    plugin.getConfigManager().getMessageList("gui.bank.day-claimed-lore")
            );
        }

        return ItemParser.createItem(
                Material.PAPER,
                plugin.getConfigManager().getMessage("gui.bank.day-available", placeholders),
                plugin.getConfigManager().getMessageList("gui.bank.day-available-lore")
        );
    }

    private void buildInfo(int monthDays, int totalPages) {
        String rankTag = plugin.getRankService().getRankTag(player);

        Map<String, String> placeholders = Map.of(
                "%bestsupplies_rank_tag%", rankTag,
                "%page%", String.valueOf(page + 1),
                "%total%", String.valueOf(totalPages),
                "%days%", String.valueOf(monthDays)
        );

        ItemStack info = ItemParser.createItem(
                Material.BOOK,
                plugin.getConfigManager().getMessage("gui.bank.info-item", placeholders),
                getMessageList("gui.bank.info-lore", placeholders),
                null
        );
        setItem(INFO_SLOT, info);
    }

    private void buildNavigation(int totalPages) {
        String backId = plugin.getConfigManager().getItemsAdderIcon("nav-back", "_iainternal:icon_back_orange");
        String nextId = plugin.getConfigManager().getItemsAdderIcon("nav-next", "_iainternal:icon_next_orange");
        String cancelId = plugin.getConfigManager().getItemsAdderIcon("nav-cancel", "_iainternal:icon_cancel");

        if (page > 0) {
            ItemStack prev = createIconItem(
                    backId,
                    Material.ARROW,
                    plugin.getConfigManager().getMessage("gui.common.prev-page-item"),
                    plugin.getConfigManager().getMessageList("gui.common.prev-page-lore")
            );
            setItem(PREV_SLOT, prev, event -> {
                page--;
                rebuild();
            });
        } else {
            ItemStack cancel = createIconItem(
                    cancelId,
                    Material.BARRIER,
                    plugin.getConfigManager().getMessage("gui.common.no-prev-item"),
                    plugin.getConfigManager().getMessageList("gui.common.no-prev-lore")
            );
            setItem(PREV_SLOT, cancel);
        }

        if (page + 1 < totalPages) {
            ItemStack next = createIconItem(
                    nextId,
                    Material.ARROW,
                    plugin.getConfigManager().getMessage("gui.common.next-page-item"),
                    plugin.getConfigManager().getMessageList("gui.common.next-page-lore")
            );
            setItem(NEXT_SLOT, next, event -> {
                page++;
                rebuild();
            });
        } else {
            ItemStack cancel = createIconItem(
                    cancelId,
                    Material.BARRIER,
                    plugin.getConfigManager().getMessage("gui.common.no-next-item"),
                    plugin.getConfigManager().getMessageList("gui.common.no-next-lore")
            );
            setItem(NEXT_SLOT, cancel);
        }
    }

    private void buildBackButton() {
        ItemStack back = ItemParser.createItem(
                Material.CHEST_MINECART,
                plugin.getConfigManager().getMessage("gui.bank.back-item"),
                plugin.getConfigManager().getMessageList("gui.bank.back-lore")
        );
        setItem(BACK_SLOT, back, event -> plugin.getGuiManager().openHub(player));
    }
}
