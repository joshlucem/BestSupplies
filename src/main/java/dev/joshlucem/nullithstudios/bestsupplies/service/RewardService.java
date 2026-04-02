package dev.joshlucem.nullithstudios.bestsupplies.service;

import dev.joshlucem.nullithstudios.balance.api.BalanceApi;
import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.config.ConfigManager;
import dev.joshlucem.nullithstudios.bestsupplies.model.DailyRewardDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.model.StreakMilestone;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RewardService {

    private final BestSupplies plugin;
    private final ConfigManager configManager;
    private final PendingService pendingService;

    public RewardService(BestSupplies plugin, ConfigManager configManager, PendingService pendingService) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.pendingService = pendingService;
    }

    /**
     * Give a daily reward to a player with streak bonus applied
     */
    public void giveDailyReward(Player player, DailyRewardDefinition reward, int streak) {
        if (reward == null) {
            return;
        }

        double bonusMultiplier = getStreakBonusMultiplier(streak);
        int itemBonus = getStreakItemBonus(streak);

        // Silver is the regular currency used for daily payouts.
        if (reward.hasSilver()) {
            double silver = reward.getSilverAmount();
            double bonusSilver = silver * bonusMultiplier;
            double totalSilver = silver + bonusSilver;

            if (plugin.getEconomyService().depositSilver(player, totalSilver, "daily-reward")) {
                sendCurrencyRewardMessage(player, BalanceApi.CurrencyType.SILVER, totalSilver);
                if (bonusSilver > 0) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("%bonus%", String.valueOf((int) (bonusMultiplier * 100)));
                    Text.sendPrefixed(player, configManager.getMessage("daily.streak-bonus", placeholders), configManager);
                }
            } else {
                Text.sendPrefixed(player, configManager.getMessage("general.economy-error"), configManager);
            }
        }

        // Direct gold rewards should remain rare and intentionally configured.
        if (reward.hasGold()) {
            if (plugin.getEconomyService().depositGold(player, reward.getGoldAmount(), "daily-reward-gold")) {
                sendCurrencyRewardMessage(player, BalanceApi.CurrencyType.GOLD, reward.getGoldAmount());
            } else {
                Text.sendPrefixed(player, configManager.getMessage("general.economy-error"), configManager);
            }
        }

        // Give items with bonus
        if (reward.hasItems()) {
            List<ItemStack> items = ItemParser.parseItems(reward.getItems());
            
            // Apply item bonus - add extra items
            if (itemBonus > 0 && !items.isEmpty()) {
                // Add bonus copies of random items from the reward
                for (int i = 0; i < itemBonus; i++) {
                    ItemStack bonusItem = items.get(i % items.size()).clone();
                    bonusItem.setAmount(1);
                    items.add(bonusItem);
                }
            }
            
            giveItemsOrPending(player, items);
        }

        // Execute commands
        if (reward.hasCommands()) {
            for (String command : reward.getCommands()) {
                String processedCommand = command.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
            }
        }

        // Check for streak milestone bonus
        StreakMilestone milestone = configManager.getStreakMilestone(streak);
        if (milestone != null) {
            giveStreakMilestoneBonus(player, milestone);
        }

        Text.sendPrefixed(player, configManager.getMessage("daily.claim-success"), configManager);
    }

    /**
     * Give streak milestone bonus
     */
    private void giveStreakMilestoneBonus(Player player, StreakMilestone milestone) {
        plugin.debug("Entregando bonus de milestone para racha " + milestone.getStreakDay());

        if (milestone.hasBonusSilver()) {
            if (plugin.getEconomyService().depositSilver(player, milestone.getBonusSilver(), "daily-streak-milestone-silver")) {
                sendCurrencyRewardMessage(player, BalanceApi.CurrencyType.SILVER, milestone.getBonusSilver());
            } else {
                Text.sendPrefixed(player, configManager.getMessage("general.economy-error"), configManager);
            }
        }

        if (milestone.hasBonusGold()) {
            if (plugin.getEconomyService().depositGold(player, milestone.getBonusGold(), "daily-streak-milestone-gold")) {
                sendCurrencyRewardMessage(player, BalanceApi.CurrencyType.GOLD, milestone.getBonusGold());
            } else {
                Text.sendPrefixed(player, configManager.getMessage("general.economy-error"), configManager);
            }
        }

        // Give bonus items
        if (milestone.hasBonusItems()) {
            List<ItemStack> items = ItemParser.parseItems(milestone.getBonusItems());
            giveItemsOrPending(player, items);
        }

        // Send milestone message
        if (milestone.getMessage() != null && !milestone.getMessage().isEmpty()) {
            Text.send(player, milestone.getMessage());
        } else {
            Text.sendPrefixed(player, configManager.getMessage("daily.streak-milestone"), configManager);
        }
    }

    /**
     * Get the bonus multiplier based on streak
     * 0-2: 0% (base)
     * 3-6: 5%
     * 7-13: 10%
     * 14+: 15%
     */
    public double getStreakBonusMultiplier(int streak) {
        if (streak >= 14) {
            return 0.15;
        } else if (streak >= 7) {
            return 0.10;
        } else if (streak >= 3) {
            return 0.05;
        }
        return 0.0;
    }

    /**
     * Get the bonus items count based on streak
     * 0-2: 0
     * 3-6: 1
     * 7-13: 2
     * 14+: 3
     */
    public int getStreakItemBonus(int streak) {
        if (streak >= 14) {
            return 3;
        } else if (streak >= 7) {
            return 2;
        } else if (streak >= 3) {
            return 1;
        }
        return 0;
    }

    /**
     * Get bonus percentage for display
     */
    public int getStreakBonusPercent(int streak) {
        return (int) (getStreakBonusMultiplier(streak) * 100);
    }

    /**
     * Give items to player, saving to pending if inventory is full
     */
    public void giveItemsOrPending(Player player, List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<ItemStack> notGiven = ItemParser.giveItems(player, items);
        
        if (!notGiven.isEmpty()) {
            pendingService.saveItemsToPending(player, notGiven);
        }
    }

    /**
     * Give a single item, saving to pending if inventory is full
     */
    public void giveItemOrPending(Player player, ItemStack item) {
        if (item == null) {
            return;
        }

        List<ItemStack> notGiven = new ArrayList<>();
        var leftover = player.getInventory().addItem(item.clone());
        if (!leftover.isEmpty()) {
            notGiven.addAll(leftover.values());
        }

        if (!notGiven.isEmpty()) {
            pendingService.saveItemsToPending(player, notGiven);
        }
    }

    /**
     * Give food pack items to a player
     */
    public void giveFoodPackItems(Player player, List<String> itemStrings) {
        if (itemStrings == null || itemStrings.isEmpty()) {
            return;
        }

        List<ItemStack> items = ItemParser.parseItems(itemStrings);
        giveItemsOrPending(player, items);
    }

    private void sendCurrencyRewardMessage(Player player, BalanceApi.CurrencyType currency, double amount) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%amount%", Text.formatCurrency(currency, amount));
        Text.sendPrefixed(player, configManager.getMessage("daily.claim-money", placeholders), configManager);
    }
}
