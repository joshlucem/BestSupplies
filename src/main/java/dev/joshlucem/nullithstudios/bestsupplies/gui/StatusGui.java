package dev.joshlucem.nullithstudios.bestsupplies.gui;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.model.FoodPackDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.model.RankDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.service.DailyService;
import dev.joshlucem.nullithstudios.bestsupplies.service.FoodService;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatusGui extends BaseGui {

    private static final int PLAYER_SLOT = 13;
    private static final int DAILY_SLOT = 20;
    private static final int BANK_SLOT = 22;
    private static final int FOOD_SLOT = 24;
    private static final int PENDING_SLOT = 31;
    private static final int BACK_SLOT = 49;

    public StatusGui(BestSupplies plugin, Player player) {
        super(plugin, player);
        init(plugin.getConfigManager().getGuiTitle("status"), 6);
    }

    @Override
    protected void build() {
        fillBorder(plugin.getConfigManager().getDecorationBorder());
        fillEmpty(plugin.getConfigManager().getDecorationFiller());

        buildPlayerInfo();
        buildDailyStatus();
        buildBankStatus();
        buildFoodStatus();
        buildPendingStatus();

        setItem(BACK_SLOT, createBackButton(), event -> plugin.getGuiManager().openHub(player));
    }

    private void buildPlayerInfo() {
        RankDefinition rank = plugin.getRankService().detectRank(player);
        String rankName = rank != null ? rank.getDisplayName() : "Aventurero";
        int streak = plugin.getDailyService().getStreak(player);
        int pending = plugin.getPendingService().getPendingCount(player);

        List<String> lore = new ArrayList<>();
        lore.add("<gray>Rango: " + rankName + "</gray>");
        lore.add("<gray>Racha: <gold>" + streak + " dias</gold></gray>");
        lore.add("<gray>Pendientes: <yellow>" + pending + "</yellow></gray>");

        ItemStack item = ItemParser.createItem(
            Material.PLAYER_HEAD,
            plugin.getConfigManager().getMessage("gui.status.player-item"),
            lore,
            null
        );

        setItem(PLAYER_SLOT, item);
    }

    private void buildDailyStatus() {
        DayOfWeek today = plugin.getTimeService().getCurrentDayOfWeek();
        DailyService.DailyStatus status = plugin.getDailyService().getDayStatus(player, today);

        List<String> lore = new ArrayList<>();
        Material material;
        String displayName;

        if (status == DailyService.DailyStatus.AVAILABLE) {
            material = Material.LIME_DYE;
            displayName = plugin.getConfigManager().getMessage("gui.status.daily-ready-item");
            lore.addAll(plugin.getConfigManager().getMessageList("gui.status.daily-ready-lore"));
        } else if (status == DailyService.DailyStatus.CLAIMED) {
            material = Material.YELLOW_DYE;
            displayName = plugin.getConfigManager().getMessage("gui.status.daily-claimed-item");
            lore.addAll(plugin.getConfigManager().getMessageList("gui.status.daily-claimed-lore"));
        } else {
            material = Material.GRAY_DYE;
            displayName = plugin.getConfigManager().getMessage("gui.status.daily-locked-item");
            lore.addAll(plugin.getConfigManager().getMessageList("gui.status.daily-locked-lore"));
        }

        lore.add("");
        lore.add("<gray>Clic para abrir diarias</gray>");

        setItem(DAILY_SLOT, ItemParser.createItem(material, displayName, lore, null), event -> {
            plugin.getGuiManager().openDaily(player);
        });
    }

    private void buildBankStatus() {
        boolean claimed = plugin.getBankService().hasClaimedWeekly(player);
        double amount = plugin.getBankService().getWeeklyAmount(player);

        List<String> lore = new ArrayList<>();
        Material material;
        String displayName;

        if (claimed) {
            material = Material.YELLOW_DYE;
            displayName = plugin.getConfigManager().getMessage("gui.status.bank-claimed-item");

            Duration timeUntil = plugin.getTimeService().getTimeUntilWeeklyReset();
            String timeStr = plugin.getTimeService().formatDuration(timeUntil);
            lore.addAll(getMessageList("gui.status.bank-claimed-lore", Map.of("%time%", timeStr)));
        } else {
            material = Material.LIME_DYE;
            displayName = plugin.getConfigManager().getMessage("gui.status.bank-ready-item");
            lore.addAll(getMessageList("gui.status.bank-ready-lore", Map.of("%amount%", Text.formatMoney(amount))));
        }

        lore.add("");
        lore.add("<gray>Clic para abrir banca</gray>");

        setItem(BANK_SLOT, ItemParser.createItem(material, displayName, lore, null), event -> {
            plugin.getGuiManager().openBank(player);
        });
    }

    private void buildFoodStatus() {
        Map<String, FoodPackDefinition> packs = plugin.getFoodService().getAvailablePacks(player);

        List<String> lore = new ArrayList<>();
        Material material = Material.GRAY_DYE;
        String displayName = plugin.getConfigManager().getMessage("gui.status.food-default-item");

        if (packs.isEmpty()) {
            lore.add("<gray>No tienes packs disponibles por rango.</gray>");
        } else {
            int ready = 0;
            for (Map.Entry<String, FoodPackDefinition> entry : packs.entrySet()) {
                FoodService.PackStatus status = plugin.getFoodService().getPackStatus(player, entry.getKey());
                String packName = entry.getValue().getDisplayName();

                if (status == FoodService.PackStatus.READY) {
                    ready++;
                    lore.add("<green>+ " + packName + "</green>");
                } else {
                    String time = plugin.getFoodService().formatTimeUntilAvailable(player, entry.getKey());
                    lore.add("<red>- " + packName + " <gray>(" + time + ")</gray></red>");
                }
            }

            if (ready > 0) {
                material = Material.LIME_DYE;
                displayName = plugin.getConfigManager().getMessage("gui.status.food-ready-item", Map.of("%count%", String.valueOf(ready)));
            } else {
                material = Material.RED_DYE;
                displayName = plugin.getConfigManager().getMessage("gui.status.food-cooldown-item");
            }
        }

        lore.add("");
        lore.add("<gray>Clic para abrir raciones</gray>");

        setItem(FOOD_SLOT, ItemParser.createItem(material, displayName, lore, null), event -> {
            plugin.getGuiManager().openFood(player);
        });
    }

    private void buildPendingStatus() {
        int pendingCount = plugin.getPendingService().getPendingCount(player);

        List<String> lore = new ArrayList<>();
        Material material;
        String displayName;

        if (pendingCount > 0) {
            material = Material.CHEST;
            displayName = plugin.getConfigManager().getMessage("gui.status.pending-has-item", Map.of("%count%", String.valueOf(pendingCount)));
            lore.add("<yellow>Tienes " + pendingCount + " entregas pendientes.</yellow>");
            lore.add("<gray>Retiralas para no perder control del inventario.</gray>");
        } else {
            material = Material.ENDER_CHEST;
            displayName = plugin.getConfigManager().getMessage("gui.status.pending-empty-item");
            lore.add("<gray>No tienes entregas pendientes.</gray>");
        }

        lore.add("");
        lore.add("<gray>Clic para abrir pendientes</gray>");

        setItem(PENDING_SLOT, ItemParser.createItem(material, displayName, lore, null), event -> {
            plugin.getGuiManager().openPending(player);
        });
    }

    @Override
    public void updateCountdowns() {
        buildPlayerInfo();
        buildDailyStatus();
        buildBankStatus();
        buildFoodStatus();
        buildPendingStatus();
    }
}
