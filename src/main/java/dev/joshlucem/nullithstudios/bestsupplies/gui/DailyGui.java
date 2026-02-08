package dev.joshlucem.nullithstudios.bestsupplies.gui;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.model.DailyRewardDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.service.DailyService;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyGui extends BaseGui {

    private static final int[] WEEK_SLOTS = {19, 20, 21, 22, 23, 24, 25}; // Mon-Sun
    private static final DayOfWeek[] DAYS_ORDER = {
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    };

    public DailyGui(BestSupplies plugin, Player player) {
        super(plugin, player);
        
        // Check and update streak when opening
        plugin.getDailyService().checkAndUpdateStreak(player);
        
        init(plugin.getConfigManager().getGuiTitle("daily"), 6);
    }

    @Override
    protected void build() {
        fillBorder(plugin.getConfigManager().getDecorationBorder());
        fillEmpty(plugin.getConfigManager().getDecorationFiller());

        // Streak display (slot 13)
        buildStreakItem();

        // Week days (slots 19-25)
        buildWeekDays();

        // Claim today button (slot 31)
        buildClaimButton();

        // Back button (slot 49)
        setItem(49, createBackButton(), event -> {
            plugin.getGuiManager().openHub(player);
        });
    }

    private void buildStreakItem() {
        int streak = plugin.getDailyService().getStreak(player);
        int bonusPercent = plugin.getRewardService().getStreakBonusPercent(streak);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%streak%", String.valueOf(streak));
        placeholders.put("%bonus%", String.valueOf(bonusPercent));

        List<String> loreStrings = plugin.getConfigManager().getMessageList("gui.daily.streak-lore");
        List<String> processedLore = new ArrayList<>();
        for (String line : loreStrings) {
            String processed = line;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                processed = processed.replace(entry.getKey(), entry.getValue());
            }
            processedLore.add(processed);
        }

        ItemStack item = ItemParser.createItem(
            Material.EXPERIENCE_BOTTLE,
            plugin.getConfigManager().getMessage("gui.daily.streak-item"),
            processedLore,
            null
        );

        setItem(13, item);
    }

    private void buildWeekDays() {
        DayOfWeek today = plugin.getTimeService().getCurrentDayOfWeek();

        for (int i = 0; i < DAYS_ORDER.length; i++) {
            DayOfWeek day = DAYS_ORDER[i];
            int slot = WEEK_SLOTS[i];

            DailyRewardDefinition reward = plugin.getConfigManager().getDailyReward(day);
            DailyService.DailyStatus status = plugin.getDailyService().getDayStatus(player, day);

            ItemStack item = createDayItem(day, reward, status, today);
            
            final DayOfWeek clickedDay = day;
            setItem(slot, item, event -> {
                // Only allow clicking on today's reward if available
                if (clickedDay == today && status == DailyService.DailyStatus.AVAILABLE) {
                    claimToday();
                }
            });
        }
    }

    private ItemStack createDayItem(DayOfWeek day, DailyRewardDefinition reward, 
                                     DailyService.DailyStatus status, DayOfWeek today) {
        Material material;
        String dayName = Text.getDayNameSpanish(day);
        String displayName;
        List<String> lore = new ArrayList<>();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%day%", dayName);

        switch (status) {
            case AVAILABLE:
                material = reward != null ? reward.getIcon() : Material.CHEST;
                displayName = plugin.getConfigManager().getMessage("gui.daily.day-available", placeholders);
                if (reward != null) {
                    lore.addAll(reward.getDescription());
                    if (reward.hasMoney()) {
                        lore.add("<gray>Dinero: <gold>$" + Text.formatMoney(reward.getMoney()) + "</gold></gray>");
                    }
                }
                lore.add("");
                lore.add("<green>¡Clic para reclamar!</green>");
                break;

            case CLAIMED:
                material = Material.LIME_DYE;
                displayName = plugin.getConfigManager().getMessage("gui.daily.day-claimed", placeholders);
                lore.add("<yellow>Ya reclamaste esta recompensa.</yellow>");
                break;

            case EXPIRED:
                material = Material.GRAY_DYE;
                displayName = plugin.getConfigManager().getMessage("gui.daily.day-expired", placeholders);
                lore.add("<red>Esta recompensa ya expiró.</red>");
                break;

            case LOCKED:
            default:
                material = Material.RED_DYE;
                displayName = plugin.getConfigManager().getMessage("gui.daily.day-locked", placeholders);
                Duration timeUntil = plugin.getTimeService().getTimeUntilDay(day);
                String timeStr = plugin.getTimeService().formatDuration(timeUntil);
                lore.add("<gray>Se desbloquea en " + timeStr + "</gray>");
                break;
        }

        return ItemParser.createItem(material, displayName, lore, null);
    }

    private void buildClaimButton() {
        DailyService.DailyStatus todayStatus = plugin.getDailyService().getDayStatus(player, 
            plugin.getTimeService().getCurrentDayOfWeek());

        ItemStack item;
        ClickAction action = null;

        if (todayStatus == DailyService.DailyStatus.AVAILABLE) {
            item = ItemParser.createItem(
                Material.DIAMOND,
                plugin.getConfigManager().getMessage("gui.daily.claim-item"),
                plugin.getConfigManager().getMessageList("gui.daily.claim-lore")
            );
            action = event -> claimToday();
        } else if (todayStatus == DailyService.DailyStatus.CLAIMED) {
            item = ItemParser.createItem(
                Material.LIME_STAINED_GLASS_PANE,
                "<yellow>Ya reclamaste hoy</yellow>",
                List.of("<gray>Vuelve mañana para tu</gray>", "<gray>siguiente recompensa.</gray>")
            );
        } else {
            // This shouldn't happen, but just in case
            item = ItemParser.createItem(
                Material.BARRIER,
                "<red>No disponible</red>",
                null
            );
        }

        setItem(31, item, action);
    }

    private void claimToday() {
        DailyService.ClaimResult result = plugin.getDailyService().claimDaily(player);
        
        if (result == DailyService.ClaimResult.SUCCESS) {
            // Refresh the GUI
            build();
        } else if (result == DailyService.ClaimResult.ALREADY_CLAIMED) {
            Text.sendPrefixed(player, plugin.getConfigManager().getMessage("daily.already-claimed"), 
                plugin.getConfigManager());
        }
    }

    @Override
    public void updateCountdowns() {
        // Update only the locked (future) days
        DayOfWeek today = plugin.getTimeService().getCurrentDayOfWeek();

        for (int i = 0; i < DAYS_ORDER.length; i++) {
            DayOfWeek day = DAYS_ORDER[i];
            
            // Only update future days
            if (day.getValue() > today.getValue()) {
                int slot = WEEK_SLOTS[i];
                DailyRewardDefinition reward = plugin.getConfigManager().getDailyReward(day);
                DailyService.DailyStatus status = plugin.getDailyService().getDayStatus(player, day);
                
                ItemStack item = createDayItem(day, reward, status, today);
                inventory.setItem(slot, item);
            }
        }
    }
}
