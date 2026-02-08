package dev.joshlucem.nullithstudios.bestsupplies.gui;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.model.RankDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.service.BankService;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.*;

public class BankGui extends BaseGui {

    public BankGui(BestSupplies plugin, Player player) {
        super(plugin, player);
        init(plugin.getConfigManager().getGuiTitle("bank"), 6);
    }

    @Override
    protected void build() {
        fillBorder(plugin.getConfigManager().getDecorationBorder());
        fillEmpty(plugin.getConfigManager().getDecorationFiller());

        // Rank display (slot 13)
        buildRankDisplay();

        // Cheque button (slot 22)
        buildChequeButton();

        // Countdown display (slot 31)
        buildCountdownDisplay();

        // Back button (slot 49)
        setItem(49, createBackButton(), event -> {
            plugin.getGuiManager().openHub(player);
        });
    }

    private void buildRankDisplay() {
        RankDefinition rank = plugin.getRankService().detectRank(player);
        String rankName = rank != null ? rank.getDisplayName() : "Aventurero";
        double weeklyAmount = rank != null ? rank.getWeeklyMoney() : 0;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%rank%", rankName);
        placeholders.put("%amount%", Text.formatMoney(weeklyAmount));

        List<String> lore = new ArrayList<>();
        lore.add(plugin.getConfigManager().getMessage("bank.rank-display", placeholders));
        lore.add(plugin.getConfigManager().getMessage("bank.weekly-amount", placeholders));

        ItemStack item = ItemParser.createItem(
            Material.NAME_TAG,
            plugin.getConfigManager().getMessage("gui.bank.rank-item"),
            lore,
            null
        );

        setItem(13, item);
    }

    private void buildChequeButton() {
        boolean claimed = plugin.getBankService().hasClaimedWeekly(player);
        double amount = plugin.getBankService().getWeeklyAmount(player);

        ItemStack item;
        ClickAction action = null;

        if (!claimed) {
            // Available to claim
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Monto: <gold>$" + Text.formatMoney(amount) + "</gold></gray>");
            lore.add("");
            lore.add("<green>¡Clic para reclamar!</green>");

            item = ItemParser.createItem(
                Material.PAPER,
                plugin.getConfigManager().getMessage("gui.bank.cheque-available"),
                lore,
                null
            );

            action = event -> claimCheque();
        } else {
            // Already claimed
            Duration timeUntil = plugin.getTimeService().getTimeUntilWeeklyReset();
            String timeStr = plugin.getTimeService().formatDuration(timeUntil);

            List<String> lore = new ArrayList<>();
            lore.add("<yellow>Ya reclamaste tu cheque esta semana.</yellow>");
            lore.add("");
            lore.add("<gray>Reinicia en: " + timeStr + "</gray>");

            item = ItemParser.createItem(
                Material.MAP,
                plugin.getConfigManager().getMessage("gui.bank.cheque-claimed"),
                lore,
                null
            );
        }

        setItem(22, item, action);
    }

    private void buildCountdownDisplay() {
        Duration timeUntil = plugin.getTimeService().getTimeUntilWeeklyReset();
        String timeStr = plugin.getTimeService().formatDuration(timeUntil);

        List<String> lore = new ArrayList<>();
        lore.add("<gray>Tiempo restante: <aqua>" + timeStr + "</aqua></gray>");
        lore.add("");
        lore.add("<gray>Los cheques se reinician</gray>");
        lore.add("<gray>cada semana.</gray>");

        ItemStack item = ItemParser.createItem(
            Material.CLOCK,
            plugin.getConfigManager().getMessage("gui.bank.countdown-item"),
            lore,
            null
        );

        setItem(31, item);
    }

    private void claimCheque() {
        BankService.ClaimResult result = plugin.getBankService().claimWeekly(player);

        if (result == BankService.ClaimResult.SUCCESS) {
            // Refresh the GUI
            build();
        } else if (result == BankService.ClaimResult.ALREADY_CLAIMED) {
            Text.sendPrefixed(player, plugin.getConfigManager().getMessage("bank.already-claimed"),
                plugin.getConfigManager());
        }
    }

    @Override
    public void updateCountdowns() {
        // Update the countdown display
        Duration timeUntil = plugin.getTimeService().getTimeUntilWeeklyReset();
        String timeStr = plugin.getTimeService().formatDuration(timeUntil);

        List<String> lore = new ArrayList<>();
        lore.add("<gray>Tiempo restante: <aqua>" + timeStr + "</aqua></gray>");
        lore.add("");
        lore.add("<gray>Los cheques se reinician</gray>");
        lore.add("<gray>cada semana.</gray>");

        ItemStack countdownItem = ItemParser.createItem(
            Material.CLOCK,
            plugin.getConfigManager().getMessage("gui.bank.countdown-item"),
            lore,
            null
        );

        inventory.setItem(31, countdownItem);

        // Also update cheque button if claimed (to show updated countdown)
        if (plugin.getBankService().hasClaimedWeekly(player)) {
            List<String> chequeLore = new ArrayList<>();
            chequeLore.add("<yellow>Ya reclamaste tu cheque esta semana.</yellow>");
            chequeLore.add("");
            chequeLore.add("<gray>Reinicia en: " + timeStr + "</gray>");

            ItemStack chequeItem = ItemParser.createItem(
                Material.MAP,
                plugin.getConfigManager().getMessage("gui.bank.cheque-claimed"),
                chequeLore,
                null
            );

            inventory.setItem(22, chequeItem);
        }
    }
}
