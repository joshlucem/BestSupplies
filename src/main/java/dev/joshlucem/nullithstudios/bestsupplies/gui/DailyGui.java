package dev.joshlucem.nullithstudios.bestsupplies.gui;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.model.DailyRewardDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.service.DailyService;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyGui extends BaseGui {

    private static final DayOfWeek[] DAYS_ORDER = {
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    };

    private static final int DEFAULT_STREAK_SLOT = 13;
    private static final int DEFAULT_CLAIM_SLOT = 31;
    private static final int DEFAULT_WEEK_START_SLOT = 19;
    private static final int DEFAULT_BACK_SLOT = 49;

    public DailyGui(BestSupplies plugin, Player player) {
        super(plugin, player);
        plugin.getDailyService().checkAndUpdateStreak(player);
        init(plugin.getConfigManager().getGuiTitle("daily"), 6);
    }

    @Override
    protected void build() {
        fillBorder(plugin.getConfigManager().getDecorationBorder());
        fillEmpty(plugin.getConfigManager().getDecorationFiller());

        buildStreakItem();
        buildWeekDays();
        buildClaimButton();

        int backSlot = getSlot("daily", "back", DEFAULT_BACK_SLOT);
        setItem(backSlot, createBackButton(), event -> plugin.getGuiManager().openHub(player));
    }

    private int[] resolveWeekSlots() {
        int start = getSlot("daily", "week-start", DEFAULT_WEEK_START_SLOT);
        int[] slots = new int[DAYS_ORDER.length];
        for (int i = 0; i < DAYS_ORDER.length; i++) {
            slots[i] = start + i;
        }
        return slots;
    }

    private void buildStreakItem() {
        int streak = plugin.getDailyService().getStreak(player);
        int bonusPercent = plugin.getRewardService().getStreakBonusPercent(streak);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%streak%", String.valueOf(streak));
        placeholders.put("%bonus%", String.valueOf(bonusPercent));

        ItemStack item = ItemParser.createItem(
            Material.EXPERIENCE_BOTTLE,
            plugin.getConfigManager().getMessage("gui.daily.streak-item"),
            getMessageList("gui.daily.streak-lore", placeholders),
            null
        );

        int streakSlot = getSlot("daily", "streak", DEFAULT_STREAK_SLOT);
        setItem(streakSlot, item);
    }

    private void buildWeekDays() {
        DayOfWeek today = plugin.getTimeService().getCurrentDayOfWeek();
        int[] weekSlots = resolveWeekSlots();

        for (int i = 0; i < DAYS_ORDER.length; i++) {
            DayOfWeek day = DAYS_ORDER[i];
            int slot = weekSlots[i];

            DailyRewardDefinition reward = plugin.getConfigManager().getDailyReward(day);
            DailyService.DailyStatus status = plugin.getDailyService().getDayStatus(player, day);

            setItem(slot, createDayItem(day, reward, status), event -> {
                DayOfWeek currentToday = plugin.getTimeService().getCurrentDayOfWeek();
                DailyService.DailyStatus currentStatus = plugin.getDailyService().getDayStatus(player, day);

                if (day == currentToday && currentStatus == DailyService.DailyStatus.AVAILABLE) {
                    claimToday();
                }
            });
        }
    }

    private ItemStack createDayItem(DayOfWeek day, DailyRewardDefinition reward, DailyService.DailyStatus status) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%day%", Text.getDayNameSpanish(day));

        Material material;
        String displayName;
        List<String> lore = new ArrayList<>();

        switch (status) {
            case AVAILABLE -> {
                material = reward != null ? reward.getIcon() : Material.CHEST;
                displayName = plugin.getConfigManager().getMessage("gui.daily.day-available", placeholders);

                if (reward != null) {
                    lore.addAll(reward.getDescription());
                    if (reward.hasMoney()) {
                        lore.add("<gray>Dinero: <gold>$" + Text.formatMoney(reward.getMoney()) + "</gold></gray>");
                    }
                }
                lore.addAll(plugin.getConfigManager().getMessageList("gui.daily.day-available-lore"));
            }
            case CLAIMED -> {
                material = Material.LIME_DYE;
                displayName = plugin.getConfigManager().getMessage("gui.daily.day-claimed", placeholders);
                lore.addAll(plugin.getConfigManager().getMessageList("gui.daily.day-claimed-lore"));
            }
            case EXPIRED -> {
                material = Material.GRAY_DYE;
                displayName = plugin.getConfigManager().getMessage("gui.daily.day-expired", placeholders);
                lore.addAll(plugin.getConfigManager().getMessageList("gui.daily.day-expired-lore"));
            }
            case LOCKED -> {
                material = Material.RED_DYE;
                displayName = plugin.getConfigManager().getMessage("gui.daily.day-locked", placeholders);
                Duration timeUntil = plugin.getTimeService().getTimeUntilDay(day);
                lore.addAll(getMessageList(
                    "gui.daily.day-locked-lore",
                    Map.of("%time%", plugin.getTimeService().formatDuration(timeUntil))
                ));
            }
            default -> {
                material = Material.BARRIER;
                displayName = plugin.getConfigManager().getMessage("gui.daily.day-locked", placeholders);
                lore.add("<red>No disponible</red>");
            }
        }

        return ItemParser.createItem(material, displayName, lore, null);
    }

    private void buildClaimButton() {
        DayOfWeek today = plugin.getTimeService().getCurrentDayOfWeek();
        DailyService.DailyStatus todayStatus = plugin.getDailyService().getDayStatus(player, today);
        int claimSlot = getSlot("daily", "claim-today", DEFAULT_CLAIM_SLOT);

        if (todayStatus == DailyService.DailyStatus.AVAILABLE) {
            ItemStack item = ItemParser.createItem(
                Material.DIAMOND,
                plugin.getConfigManager().getMessage("gui.daily.claim-item"),
                plugin.getConfigManager().getMessageList("gui.daily.claim-lore")
            );
            setItem(claimSlot, item, event -> claimToday());
            return;
        }

        if (todayStatus == DailyService.DailyStatus.CLAIMED) {
            ItemStack item = ItemParser.createItem(
                Material.LIME_STAINED_GLASS_PANE,
                plugin.getConfigManager().getMessage("gui.daily.claimed-item"),
                plugin.getConfigManager().getMessageList("gui.daily.claimed-lore")
            );
            setItem(claimSlot, item);
            return;
        }

        ItemStack item = ItemParser.createItem(
            Material.BARRIER,
            plugin.getConfigManager().getMessage("gui.daily.unavailable-item"),
            plugin.getConfigManager().getMessageList("gui.daily.unavailable-lore")
        );
        setItem(claimSlot, item);
    }

    private void claimToday() {
        DailyService.ClaimResult result = plugin.getDailyService().claimDaily(player);

        if (result == DailyService.ClaimResult.SUCCESS) {
            rebuild();
            return;
        }

        if (result == DailyService.ClaimResult.ALREADY_CLAIMED) {
            Text.sendPrefixed(
                player,
                plugin.getConfigManager().getMessage("daily.already-claimed"),
                plugin.getConfigManager()
            );
        }
    }

    @Override
    public void updateCountdowns() {
        buildStreakItem();
        buildWeekDays();
        buildClaimButton();
    }
}
