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

    public PlayerState getPlayerState(Player player) {
        return database.getPlayerState(player.getUniqueId().toString());
    }

    public void savePlayerState(PlayerState state) {
        database.savePlayerState(state);
    }

    public int getStreak(Player player) {
        return getPlayerState(player).getStreak();
    }

    public boolean hasClaimedToday(Player player) {
        return hasClaimedOnDate(player, timeService.getCurrentDate());
    }

    public boolean hasClaimedOnDate(Player player, LocalDate date) {
        String dateKey = timeService.getDateKey(date);
        return database.hasDailyClaim(player.getUniqueId().toString(), dateKey);
    }

    public DailyDateStatus getDateStatus(Player player, LocalDate date) {
        LocalDate today = timeService.getCurrentDate();

        if (date.isAfter(today)) {
            return DailyDateStatus.FUTURE_LOCKED;
        }

        boolean claimed = hasClaimedOnDate(player, date);
        if (date.isEqual(today)) {
            return claimed ? DailyDateStatus.TODAY_CLAIMED : DailyDateStatus.TODAY_AVAILABLE;
        }

        return claimed ? DailyDateStatus.PAST_CLAIMED : DailyDateStatus.PAST_MISSED;
    }

    public DailyStatus getDayStatus(Player player, DayOfWeek day) {
        LocalDate date = timeService.getDateForDayOfWeek(day);
        DailyDateStatus status = getDateStatus(player, date);

        return switch (status) {
            case TODAY_AVAILABLE -> DailyStatus.AVAILABLE;
            case TODAY_CLAIMED -> DailyStatus.CLAIMED;
            case PAST_CLAIMED, PAST_MISSED -> DailyStatus.EXPIRED;
            case FUTURE_LOCKED -> DailyStatus.LOCKED;
        };
    }

    public void checkAndUpdateStreak(Player player) {
        PlayerState state = getPlayerState(player);
        String todayKey = timeService.getTodayKey();
        String lastDailyDate = state.getLastDailyDate();

        if (lastDailyDate == null) {
            return;
        }

        if (lastDailyDate.equals(todayKey)) {
            return;
        }

        if (timeService.wasYesterday(lastDailyDate)) {
            return;
        }

        if (state.getStreak() > 0) {
            plugin.debug("Racha perdida para " + player.getName() + ": mas de un dia sin reclamar");
            state.resetStreak();
            state.setLastSeenDate(todayKey);
            savePlayerState(state);
            Text.sendPrefixed(player, configManager.getMessage("daily.streak-lost"), configManager);
        }
    }

    public ClaimResult claimDaily(Player player) {
        DayOfWeek today = timeService.getCurrentDayOfWeek();

        if (hasClaimedToday(player)) {
            return ClaimResult.ALREADY_CLAIMED;
        }

        DailyRewardDefinition reward = configManager.getDailyReward(today);
        if (reward == null) {
            plugin.getLogger().warning("No hay recompensa configurada para " + today);
            return ClaimResult.NO_REWARD;
        }

        PlayerState state = getPlayerState(player);
        String todayKey = timeService.getTodayKey();
        String lastDailyDate = state.getLastDailyDate();

        boolean streakContinues = lastDailyDate != null && timeService.wasYesterday(lastDailyDate);

        if (streakContinues) {
            state.incrementStreak();
        } else {
            state.setStreak(1);
        }

        state.setLastDailyDate(todayKey);
        state.setLastSeenDate(todayKey);
        savePlayerState(state);

        database.setDailyClaim(player.getUniqueId().toString(), todayKey, true);
        rewardService.giveDailyReward(player, reward, state.getStreak());

        plugin.debug("Diaria reclamada por " + player.getName() + " - Racha: " + state.getStreak());

        return ClaimResult.SUCCESS;
    }

    public void resetDailyToday(Player target) {
        String today = timeService.getTodayKey();
        database.setDailyClaim(target.getUniqueId().toString(), today, false);
        plugin.debug("Diaria reseteada para " + target.getName());
    }

    public void resetStreak(Player target) {
        PlayerState state = getPlayerState(target);
        state.resetStreak();
        state.setLastDailyDate(null);
        savePlayerState(state);
        plugin.debug("Racha reseteada para " + target.getName());
    }

    public LocalDate getDateForDay(DayOfWeek day) {
        return timeService.getDateForDayOfWeek(day);
    }

    public DailyRewardDefinition getTodayReward() {
        return configManager.getDailyReward(timeService.getCurrentDayOfWeek());
    }

    public enum DailyDateStatus {
        TODAY_AVAILABLE,
        TODAY_CLAIMED,
        PAST_CLAIMED,
        PAST_MISSED,
        FUTURE_LOCKED
    }

    public enum DailyStatus {
        AVAILABLE,
        CLAIMED,
        EXPIRED,
        LOCKED
    }

    public enum ClaimResult {
        SUCCESS,
        ALREADY_CLAIMED,
        NO_REWARD,
        ERROR
    }
}
