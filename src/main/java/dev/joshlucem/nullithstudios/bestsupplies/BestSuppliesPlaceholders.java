package dev.joshlucem.nullithstudios.bestsupplies;

import dev.joshlucem.nullithstudios.balance.api.BalanceApi;
import dev.joshlucem.nullithstudios.bestsupplies.model.PlayerState;
import dev.joshlucem.nullithstudios.bestsupplies.model.RankDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BestSuppliesPlaceholders extends PlaceholderExpansion {

    private final BestSupplies plugin;

    public BestSuppliesPlaceholders(BestSupplies plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "bestsupplies";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().isEmpty()
                ? "NullithStudios"
                : plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null) {
            return "";
        }

        Player player = offlinePlayer.getPlayer();
        if (player == null) {
            return handleOfflinePlayer(offlinePlayer, params);
        }

        return switch (params.toLowerCase()) {
            case "streak" -> getStreak(player);
            case "rank" -> getRank(player);
            case "rank_display" -> getRankDisplay(player);
            case "rank_tag" -> getRankTag(player);
            case "daily_status" -> getDailyStatus(player);
            case "bank_status" -> getBankStatus(player);
            case "food_status" -> getFoodStatus(player);
            case "pending_count" -> getPendingCount(player);
            case "monthly_today_amount", "weekly_amount" -> getMonthlyTodayAmount(player);
            case "next_daily" -> getNextDaily();
            case "next_weekly" -> getNextWeekly();
            default -> null;
        };
    }

    private String handleOfflinePlayer(OfflinePlayer offlinePlayer, String params) {
        if (params.equalsIgnoreCase("streak")) {
            PlayerState state = plugin.getDatabase().getPlayerState(offlinePlayer.getUniqueId().toString());
            if (state != null) {
                return String.valueOf(state.getStreak());
            }
            return "0";
        }
        return "";
    }

    private String getStreak(Player player) {
        PlayerState state = plugin.getDailyService().getPlayerState(player);
        return String.valueOf(state.getStreak());
    }

    private String getRank(Player player) {
        RankDefinition rank = plugin.getRankService().detectRank(player);
        return rank != null ? rank.getId() : "default";
    }

    private String getRankDisplay(Player player) {
        return plugin.getRankService().getRankTag(player);
    }

    private String getRankTag(Player player) {
        return plugin.getRankService().getRankTag(player);
    }

    private String getDailyStatus(Player player) {
        if (plugin.getDailyService().hasClaimedToday(player)) {
            return plugin.getConfigManager().getMessage("placeholders.daily-claimed");
        }
        return plugin.getConfigManager().getMessage("placeholders.daily-pending");
    }

    private String getBankStatus(Player player) {
        if (plugin.getBankService().hasPendingMonthlyClaims(player)) {
            return plugin.getConfigManager().getMessage("placeholders.bank-pending");
        }
        return plugin.getConfigManager().getMessage("placeholders.bank-claimed");
    }

    private String getFoodStatus(Player player) {
        if (plugin.getFoodService().hasAnyReadyRation(player)) {
            return plugin.getConfigManager().getMessage("placeholders.food-pending");
        }
        return plugin.getConfigManager().getMessage("placeholders.food-claimed");
    }

    private String getPendingCount(Player player) {
        int count = plugin.getPendingService().getPendingCount(player);
        return String.valueOf(count);
    }

    private String getMonthlyTodayAmount(Player player) {
        java.time.LocalDate today = plugin.getTimeService().getCurrentDate();
        double amount = plugin.getBankService().getMonthlyAmount(player, today);
        return Text.formatCurrency(BalanceApi.CurrencyType.SILVER, amount);
    }

    private String getNextDaily() {
        java.time.Duration duration = plugin.getTimeService().getTimeUntil(plugin.getTimeService().getStartOfTomorrow());
        return plugin.getTimeService().formatDuration(duration);
    }

    private String getNextWeekly() {
        java.time.Duration duration = plugin.getTimeService().getTimeUntilWeeklyReset();
        return plugin.getTimeService().formatDuration(duration);
    }
}
