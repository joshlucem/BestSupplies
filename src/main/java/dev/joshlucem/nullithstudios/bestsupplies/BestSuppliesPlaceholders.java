package dev.joshlucem.nullithstudios.bestsupplies;

import dev.joshlucem.nullithstudios.bestsupplies.model.PlayerState;
import dev.joshlucem.nullithstudios.bestsupplies.model.RankDefinition;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for BestSupplies.
 * 
 * Available placeholders:
 * - %bestsupplies_streak% - Current daily streak
 * - %bestsupplies_rank% - Current player rank
 * - %bestsupplies_rank_display% - Rank display name
 * - %bestsupplies_daily_status% - Daily claim status (available/claimed/expired)
 * - %bestsupplies_bank_status% - Bank cheque status (available/claimed)
 * - %bestsupplies_food_status% - Food rations status (available/cooldown)
 * - %bestsupplies_pending_count% - Number of pending items
 * - %bestsupplies_weekly_amount% - Weekly cheque amount for player's rank
 * - %bestsupplies_next_daily% - Time until next daily reset
 * - %bestsupplies_next_weekly% - Time until next weekly reset
 */
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
            // Offline player - limited info available
            return handleOfflinePlayer(offlinePlayer, params);
        }

        return switch (params.toLowerCase()) {
            case "streak" -> getStreak(player);
            case "rank" -> getRank(player);
            case "rank_display" -> getRankDisplay(player);
            case "daily_status" -> getDailyStatus(player);
            case "bank_status" -> getBankStatus(player);
            case "food_status" -> getFoodStatus(player);
            case "pending_count" -> getPendingCount(player);
            case "weekly_amount" -> getWeeklyAmount(player);
            case "next_daily" -> getNextDaily();
            case "next_weekly" -> getNextWeekly();
            default -> null;
        };
    }

    private String handleOfflinePlayer(OfflinePlayer offlinePlayer, String params) {
        // Limited data for offline players from database
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
        RankDefinition rank = plugin.getRankService().detectRank(player);
        return rank != null ? rank.getDisplayName() : "Aventurero";
    }

    private String getDailyStatus(Player player) {
        if (plugin.getDailyService().hasClaimedToday(player)) {
            return plugin.getConfigManager().getMessage("placeholders.daily-claimed");
        }
        return plugin.getConfigManager().getMessage("placeholders.daily-available");
    }

    private String getBankStatus(Player player) {
        if (plugin.getBankService().hasClaimedWeekly(player)) {
            return plugin.getConfigManager().getMessage("placeholders.bank-claimed");
        }
        return plugin.getConfigManager().getMessage("placeholders.bank-available");
    }

    private String getFoodStatus(Player player) {
        RankDefinition rank = plugin.getRankService().detectRank(player);
        if (rank == null) {
            return plugin.getConfigManager().getMessage("placeholders.food-unavailable");
        }

        // Check if any pack is available
        boolean anyAvailable = rank.getPacks().keySet().stream()
                .anyMatch(packId -> plugin.getFoodService().isPackAvailable(player, packId));

        if (anyAvailable) {
            return plugin.getConfigManager().getMessage("placeholders.food-available");
        }
        return plugin.getConfigManager().getMessage("placeholders.food-cooldown");
    }

    private String getPendingCount(Player player) {
        int count = plugin.getPendingService().getPendingCount(player);
        return String.valueOf(count);
    }

    private String getWeeklyAmount(Player player) {
        RankDefinition rank = plugin.getRankService().detectRank(player);
        if (rank == null) {
            return "$0";
        }
        return "$" + String.format("%,.2f", rank.getWeeklyMoney());
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
