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
import java.util.HashMap;
import java.util.Map;

public class BankGui extends BaseGui {

    private static final int DEFAULT_RANK_SLOT = 13;
    private static final int DEFAULT_CHEQUE_SLOT = 22;
    private static final int DEFAULT_COUNTDOWN_SLOT = 31;
    private static final int DEFAULT_BACK_SLOT = 49;

    public BankGui(BestSupplies plugin, Player player) {
        super(plugin, player);
        init(plugin.getConfigManager().getGuiTitle("bank"), 6);
    }

    @Override
    protected void build() {
        fillBorder(plugin.getConfigManager().getDecorationBorder());
        fillEmpty(plugin.getConfigManager().getDecorationFiller());

        buildRankDisplay();
        buildChequeButton();
        buildCountdownDisplay();

        int backSlot = getSlot("bank", "back", DEFAULT_BACK_SLOT);
        setItem(backSlot, createBackButton(), event -> plugin.getGuiManager().openHub(player));
    }

    private void buildRankDisplay() {
        RankDefinition rank = plugin.getRankService().detectRank(player);
        String rankName = rank != null ? rank.getDisplayName() : "Aventurero";
        double weeklyAmount = rank != null ? rank.getWeeklyMoney() : 0;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%rank%", rankName);
        placeholders.put("%amount%", Text.formatMoney(weeklyAmount));

        ItemStack item = ItemParser.createItem(
            Material.NAME_TAG,
            plugin.getConfigManager().getMessage("gui.bank.rank-item"),
            getMessageList("gui.bank.rank-lore", placeholders),
            null
        );

        int rankSlot = getSlot("bank", "rank-display", DEFAULT_RANK_SLOT);
        setItem(rankSlot, item);
    }

    private void buildChequeButton() {
        boolean claimed = plugin.getBankService().hasClaimedWeekly(player);
        double amount = plugin.getBankService().getWeeklyAmount(player);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%amount%", Text.formatMoney(amount));

        ItemStack item;
        ClickAction action = null;

        if (!claimed) {
            item = ItemParser.createItem(
                Material.PAPER,
                plugin.getConfigManager().getMessage("gui.bank.cheque-available"),
                getMessageList("gui.bank.cheque-available-lore", placeholders),
                null
            );
            action = event -> claimCheque();
        } else {
            Duration timeUntil = plugin.getTimeService().getTimeUntilWeeklyReset();
            placeholders.put("%time%", plugin.getTimeService().formatDuration(timeUntil));

            item = ItemParser.createItem(
                Material.MAP,
                plugin.getConfigManager().getMessage("gui.bank.cheque-claimed"),
                getMessageList("gui.bank.cheque-claimed-lore", placeholders),
                null
            );
        }

        int chequeSlot = getSlot("bank", "cheque", DEFAULT_CHEQUE_SLOT);
        setItem(chequeSlot, item, action);
    }

    private void buildCountdownDisplay() {
        Duration timeUntil = plugin.getTimeService().getTimeUntilWeeklyReset();
        String timeStr = plugin.getTimeService().formatDuration(timeUntil);

        ItemStack item = ItemParser.createItem(
            Material.CLOCK,
            plugin.getConfigManager().getMessage("gui.bank.countdown-item"),
            getMessageList("gui.bank.countdown-lore", Map.of("%time%", timeStr)),
            null
        );

        int countdownSlot = getSlot("bank", "countdown", DEFAULT_COUNTDOWN_SLOT);
        setItem(countdownSlot, item);
    }

    private void claimCheque() {
        BankService.ClaimResult result = plugin.getBankService().claimWeekly(player);

        if (result == BankService.ClaimResult.SUCCESS) {
            rebuild();
        } else if (result == BankService.ClaimResult.ALREADY_CLAIMED) {
            Text.sendPrefixed(
                player,
                plugin.getConfigManager().getMessage("bank.already-claimed"),
                plugin.getConfigManager()
            );
        }
    }

    @Override
    public void updateCountdowns() {
        buildChequeButton();
        buildCountdownDisplay();
    }
}
