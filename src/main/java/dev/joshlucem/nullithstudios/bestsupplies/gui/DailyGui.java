package dev.joshlucem.nullithstudios.bestsupplies.gui;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.model.DailyRewardDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.service.DailyService;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DailyGui extends BaseGui {

    private static final int[] BLACK_SLOTS = {0, 8, 45, 53};
    private static final int[] GRAY_SLOTS = {1, 2, 3, 4, 5, 6, 7, 9, 17, 18, 26, 27, 35, 36, 44, 46, 47, 48, 49, 50, 51, 52};

    private static final int[] WEEK_SLOTS = {28, 20, 21, 13, 23, 24, 34};
    private static final int[] WEEK_OFFSETS = {-3, -2, -1, 0, 1, 2, 3};

    private static final int BACK_SLOT = 40;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM");

    public DailyGui(BestSupplies plugin, Player player) {
        super(plugin, player);
        plugin.getDailyService().checkAndUpdateStreak(player);
        init(plugin.getConfigManager().getGuiTitle("daily"), 6);
    }

    @Override
    protected void build() {
        fillEmpty(Material.WHITE_STAINED_GLASS_PANE);
        setSlots(BLACK_SLOTS, ItemParser.createFiller(Material.BLACK_STAINED_GLASS_PANE));
        setSlots(GRAY_SLOTS, ItemParser.createFiller(Material.GRAY_STAINED_GLASS_PANE));

        buildWeek();
        buildBackButton();
    }

    private void buildWeek() {
        LocalDate today = plugin.getTimeService().getCurrentDate();

        for (int i = 0; i < WEEK_SLOTS.length; i++) {
            int slot = WEEK_SLOTS[i];
            int offset = WEEK_OFFSETS[i];
            LocalDate date = today.plusDays(offset);
            DailyService.DailyDateStatus status = plugin.getDailyService().getDateStatus(player, date);

            ItemStack item = createDateItem(date, status);
            setItem(slot, item, event -> {
                if (status == DailyService.DailyDateStatus.TODAY_AVAILABLE) {
                    claimToday();
                }
            });
        }
    }

    private ItemStack createDateItem(LocalDate date, DailyService.DailyDateStatus status) {
        DailyRewardDefinition reward = plugin.getConfigManager().getDailyReward(date.getDayOfWeek());
        Map<String, String> placeholders = Map.of(
                "%day%", Text.getDayNameSpanish(date.getDayOfWeek()),
                "%date%", date.format(DATE_FORMAT)
        );

        Material icon;
        String name;
        List<String> lore = new ArrayList<>();

        switch (status) {
            case TODAY_AVAILABLE -> {
                icon = Material.LIME_DYE;
                name = plugin.getConfigManager().getMessage("gui.daily.today-available", placeholders);
                lore.add("<green>Haz clic para reclamar la recompensa de hoy.</green>");
            }
            case TODAY_CLAIMED -> {
                icon = Material.ORANGE_DYE;
                name = plugin.getConfigManager().getMessage("gui.daily.today-claimed", placeholders);
                lore.add("<gray>Ya reclamaste la recompensa de hoy.</gray>");
            }
            case PAST_CLAIMED -> {
                icon = Material.LIGHT_GRAY_DYE;
                name = plugin.getConfigManager().getMessage("gui.daily.past-claimed", placeholders);
                lore.add("<gray>Esta recompensa ya fue reclamada.</gray>");
            }
            case PAST_MISSED -> {
                icon = Material.GRAY_DYE;
                name = plugin.getConfigManager().getMessage("gui.daily.past-missed", placeholders);
                lore.add("<gray>Esta recompensa expiro sin reclamar.</gray>");
            }
            case FUTURE_LOCKED -> {
                icon = Material.YELLOW_DYE;
                name = plugin.getConfigManager().getMessage("gui.daily.future-locked", placeholders);
                lore.add("<gray>Se desbloqueara en los proximos dias.</gray>");
            }
            default -> {
                icon = Material.BARRIER;
                name = "<red>No disponible</red>";
            }
        }

        if (reward != null) {
            lore.add("");
            lore.add("<gray>Recompensa:</gray>");
            if (reward.hasMoney()) {
                lore.add("<gray>- Dinero: <gold>€" + Text.formatMoney(reward.getMoney()) + "</gold></gray>");
            }
            int shown = 0;
            for (String itemStr : reward.getItems()) {
                if (shown >= 3) {
                    lore.add("<gray>- ...</gray>");
                    break;
                }
                lore.add("<gray>- " + itemStr + "</gray>");
                shown++;
            }
        }

        return ItemParser.createItem(icon, name, lore, null);
    }

    private void buildBackButton() {
        ItemStack back = ItemParser.createItem(
                Material.CHEST_MINECART,
                plugin.getConfigManager().getMessage("gui.daily.back-item"),
                plugin.getConfigManager().getMessageList("gui.daily.back-lore")
        );
        setItem(BACK_SLOT, back, event -> plugin.getGuiManager().openHub(player));
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
}
