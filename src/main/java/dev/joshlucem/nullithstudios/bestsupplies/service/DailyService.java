package dev.joshlucem.nullithstudios.bestsupplies.service;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.config.ConfigManager;
import dev.joshlucem.nullithstudios.bestsupplies.model.DailyRewardDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.model.PlayerState;
import dev.joshlucem.nullithstudios.bestsupplies.storage.Database;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import org.bukkit.entity.Player;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class DailyService {

    private final BestSupplies plugin;
    private final Database database;
    private final ConfigManager configManager;
    private final TimeService timeService;
    private final RewardService rewardService;

    public DailyService(BestSupplies plugin, Database database, ConfigManager configManager,
                        TimeService timeService, RewardService rewardService) {
        this.plugin = plugin;
        this.database = database;
        this.configManager = configManager;
        this.timeService = timeService;
        this.rewardService = rewardService;
    }

    /**
     * Get the player's current state
     */
    public PlayerState getPlayerState(Player player) {
        return database.getPlayerState(player.getUniqueId().toString());
    }

    /**
     * Save the player's state
     */
    public void savePlayerState(PlayerState state) {
        database.savePlayerState(state);
    }

    /**
     * Get the player's current streak
     */
    public int getStreak(Player player) {
        return getPlayerState(player).getStreak();
    }

    /**
     * Check if player has claimed today's reward
     */
    public boolean hasClaimedToday(Player player) {
        String today = timeService.getTodayKey();
        return database.hasDailyClaim(player.getUniqueId().toString(), today);
    }

    /**
     * Check if a specific day's reward is available
     * Only today's reward can be claimed
     */
    public DailyStatus getDayStatus(Player player, DayOfWeek day) {
        DayOfWeek today = timeService.getCurrentDayOfWeek();
        
        if (day == today) {
            // Today
            if (hasClaimedToday(player)) {
                return DailyStatus.CLAIMED;
            }
            return DailyStatus.AVAILABLE;
        } else if (day.getValue() < today.getValue()) {
            // Past day this week
            return DailyStatus.EXPIRED;
        } else {
            // Future day
            return DailyStatus.LOCKED;
        }
    }

    /**
     * Check and update streak based on last claim date
     * Called when player joins or opens daily GUI
     */
    public void checkAndUpdateStreak(Player player) {
        PlayerState state = getPlayerState(player);
        String todayKey = timeService.getTodayKey();
        String lastDailyDate = state.getLastDailyDate();

        // If never claimed, streak stays at 0
        if (lastDailyDate == null) {
            return;
        }

        // If last claim was today, streak is already current
        if (lastDailyDate.equals(todayKey)) {
            return;
        }

        // If last claim was yesterday, streak continues
        if (timeService.wasYesterday(lastDailyDate)) {
            // Streak continues when they claim today
            return;
        }

        // More than one day has passed, reset streak
        if (state.getStreak() > 0) {
            plugin.debug("Racha perdida para " + player.getName() + ": más de un día sin reclamar");
            state.resetStreak();
            state.setLastSeenDate(todayKey);
            savePlayerState(state);
            Text.sendPrefixed(player, configManager.getMessage("daily.streak-lost"), configManager);
        }
    }

    /**
     * Claim today's daily reward
     */
    public ClaimResult claimDaily(Player player) {
        DayOfWeek today = timeService.getCurrentDayOfWeek();
        
        // Check if already claimed
        if (hasClaimedToday(player)) {
            return ClaimResult.ALREADY_CLAIMED;
        }

        // Get reward for today
        DailyRewardDefinition reward = configManager.getDailyReward(today);
        if (reward == null) {
            plugin.getLogger().warning("No hay recompensa configurada para " + today);
            return ClaimResult.NO_REWARD;
        }

        // Get and update player state
        PlayerState state = getPlayerState(player);
        String todayKey = timeService.getTodayKey();
        String lastDailyDate = state.getLastDailyDate();

        // Check if streak continues
        boolean streakContinues = lastDailyDate != null && 
                (lastDailyDate.equals(todayKey) || timeService.wasYesterday(lastDailyDate));

        if (streakContinues || lastDailyDate != null && lastDailyDate.equals(todayKey)) {
            // Claiming same day (shouldn't happen) or continuing streak
            state.incrementStreak();
        } else if (lastDailyDate == null) {
            // First ever claim
            state.setStreak(1);
        } else {
            // Streak broken, start new
            state.setStreak(1);
        }

        // Update state
        state.setLastDailyDate(todayKey);
        state.setLastSeenDate(todayKey);
        savePlayerState(state);

        // Mark as claimed in DB
        database.setDailyClaim(player.getUniqueId().toString(), todayKey, true);

        // Give rewards with streak bonus
        rewardService.giveDailyReward(player, reward, state.getStreak());

        plugin.debug("Diaria reclamada por " + player.getName() + " - Racha: " + state.getStreak());

        return ClaimResult.SUCCESS;
    }

    /**
     * Reset a player's daily claim for today (admin)
     */
    public void resetDailyToday(Player target) {
        String today = timeService.getTodayKey();
        database.setDailyClaim(target.getUniqueId().toString(), today, false);
        plugin.debug("Diaria reseteada para " + target.getName());
    }

    /**
     * Reset a player's streak (admin)
     */
    public void resetStreak(Player target) {
        PlayerState state = getPlayerState(target);
        state.resetStreak();
        state.setLastDailyDate(null);
        savePlayerState(state);
        plugin.debug("Racha reseteada para " + target.getName());
    }

    /**
     * Get the date for a day of the current week
     */
    public LocalDate getDateForDay(DayOfWeek day) {
        return timeService.getDateForDayOfWeek(day);
    }

    /**
     * Get today's reward definition
     */
    public DailyRewardDefinition getTodayReward() {
        return configManager.getDailyReward(timeService.getCurrentDayOfWeek());
    }

    /**
     * Possible statuses for daily rewards
     */
    public enum DailyStatus {
        AVAILABLE,    // Can claim now (today and not claimed)
        CLAIMED,      // Already claimed (today)
        EXPIRED,      // Past day, no longer claimable
        LOCKED        // Future day, not yet available
    }

    /**
     * Result of claiming a daily reward
     */
    public enum ClaimResult {
        SUCCESS,
        ALREADY_CLAIMED,
        NO_REWARD,
        ERROR
    }
}
