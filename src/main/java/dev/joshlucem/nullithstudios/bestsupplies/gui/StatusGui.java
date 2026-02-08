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
import java.util.*;

public class StatusGui extends BaseGui {

    public StatusGui(BestSupplies plugin, Player player) {
        super(plugin, player);
        init(plugin.getConfigManager().getGuiTitle("status"), 6);
    }

    @Override
    protected void build() {
        fillBorder(plugin.getConfigManager().getDecorationBorder());
        fillEmpty(plugin.getConfigManager().getDecorationFiller());

        // Player info (slot 13)
        buildPlayerInfo();

        // Daily status (slot 20)
        buildDailyStatus();

        // Bank status (slot 22)
        buildBankStatus();

        // Food status (slot 24)
        buildFoodStatus();

        // Pending status (slot 31)
        buildPendingStatus();

        // Back button (slot 49)
        setItem(49, createBackButton(), event -> {
            plugin.getGuiManager().openHub(player);
        });
    }

    private void buildPlayerInfo() {
        RankDefinition rank = plugin.getRankService().detectRank(player);
        String rankName = rank != null ? rank.getDisplayName() : "Aventurero";
        int streak = plugin.getDailyService().getStreak(player);

        List<String> lore = new ArrayList<>();
        lore.add("<gray>Rango: " + rankName + "</gray>");
        lore.add("<gray>Racha: <gold>" + streak + " días</gold></gray>");

        ItemStack item = ItemParser.createItem(
            Material.PLAYER_HEAD,
            "<gold>Tu Estado</gold>",
            lore,
            null
        );

        setItem(13, item);
    }

    private void buildDailyStatus() {
        DayOfWeek today = plugin.getTimeService().getCurrentDayOfWeek();
        DailyService.DailyStatus status = plugin.getDailyService().getDayStatus(player, today);

        List<String> lore = new ArrayList<>();
        Material material;
        String displayName;

        switch (status) {
            case AVAILABLE:
                material = Material.LIME_DYE;
                displayName = "<green>Diaria: DISPONIBLE</green>";
                lore.add("<green>¡Tu recompensa diaria está lista!</green>");
                lore.add("");
                lore.add("<gray>Clic para ir a Diarias</gray>");
                break;

            case CLAIMED:
                material = Material.YELLOW_DYE;
                displayName = "<yellow>Diaria: RECLAMADA</yellow>";
                lore.add("<yellow>Ya reclamaste tu recompensa de hoy.</yellow>");
                lore.add("");
                lore.add("<gray>Vuelve mañana.</gray>");
                break;

            default:
                material = Material.GRAY_DYE;
                displayName = "<gray>Diaria</gray>";
                lore.add("<gray>Estado desconocido.</gray>");
                break;
        }

        ItemStack item = ItemParser.createItem(material, displayName, lore, null);
        setItem(20, item, event -> {
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
            displayName = "<yellow>Cheque: RECLAMADO</yellow>";
            
            Duration timeUntil = plugin.getTimeService().getTimeUntilWeeklyReset();
            String timeStr = plugin.getTimeService().formatDuration(timeUntil);
            
            lore.add("<yellow>Ya reclamaste tu cheque esta semana.</yellow>");
            lore.add("");
            lore.add("<gray>Reinicia en: <aqua>" + timeStr + "</aqua></gray>");
        } else {
            material = Material.LIME_DYE;
            displayName = "<green>Cheque: DISPONIBLE</green>";
            lore.add("<green>¡Tu cheque semanal está listo!</green>");
            lore.add("<gray>Monto: <gold>$" + Text.formatMoney(amount) + "</gold></gray>");
            lore.add("");
            lore.add("<gray>Clic para ir a Banca</gray>");
        }

        ItemStack item = ItemParser.createItem(material, displayName, lore, null);
        setItem(22, item, event -> {
            plugin.getGuiManager().openBank(player);
        });
    }

    private void buildFoodStatus() {
        Map<String, FoodPackDefinition> packs = plugin.getFoodService().getAvailablePacks(player);

        List<String> lore = new ArrayList<>();
        Material material = Material.GRAY_DYE;
        String displayName = "<gray>Raciones</gray>";

        if (packs.isEmpty()) {
            lore.add("<gray>No tienes packs disponibles.</gray>");
        } else {
            int availableCount = 0;
            
            for (Map.Entry<String, FoodPackDefinition> entry : packs.entrySet()) {
                FoodService.PackStatus status = plugin.getFoodService().getPackStatus(player, entry.getKey());
                String packName = entry.getValue().getDisplayName();
                
                if (status == FoodService.PackStatus.READY) {
                    lore.add("<green>✓ " + packName + "</green>");
                    availableCount++;
                } else {
                    String timeStr = plugin.getFoodService().formatTimeUntilAvailable(player, entry.getKey());
                    lore.add("<red>✗ " + packName + " (" + timeStr + ")</red>");
                }
            }

            if (availableCount > 0) {
                material = Material.LIME_DYE;
                displayName = "<green>Raciones: " + availableCount + " DISPONIBLES</green>";
            } else {
                material = Material.RED_DYE;
                displayName = "<red>Raciones: EN ESPERA</red>";
            }
        }

        lore.add("");
        lore.add("<gray>Clic para ir a Raciones</gray>");

        ItemStack item = ItemParser.createItem(material, displayName, lore, null);
        setItem(24, item, event -> {
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
            displayName = "<gold>Pendientes: " + pendingCount + "</gold>";
            lore.add("<yellow>Tienes " + pendingCount + " entregas pendientes.</yellow>");
            lore.add("");
            lore.add("<gray>Clic para retirar</gray>");
        } else {
            material = Material.ENDER_CHEST;
            displayName = "<gray>Pendientes: 0</gray>";
            lore.add("<gray>No tienes entregas pendientes.</gray>");
        }

        ItemStack item = ItemParser.createItem(material, displayName, lore, null);
        setItem(31, item, event -> {
            if (pendingCount > 0) {
                plugin.getGuiManager().openPending(player);
            }
        });
    }

    @Override
    public void updateCountdowns() {
        // Update bank countdown
        boolean claimed = plugin.getBankService().hasClaimedWeekly(player);
        if (claimed) {
            double amount = plugin.getBankService().getWeeklyAmount(player);
            Duration timeUntil = plugin.getTimeService().getTimeUntilWeeklyReset();
            String timeStr = plugin.getTimeService().formatDuration(timeUntil);

            List<String> lore = new ArrayList<>();
            lore.add("<yellow>Ya reclamaste tu cheque esta semana.</yellow>");
            lore.add("");
            lore.add("<gray>Reinicia en: <aqua>" + timeStr + "</aqua></gray>");

            ItemStack item = ItemParser.createItem(
                Material.YELLOW_DYE,
                "<yellow>Cheque: RECLAMADO</yellow>",
                lore,
                null
            );
            inventory.setItem(22, item);
        }

        // Update food status
        buildFoodStatus();
    }
}
