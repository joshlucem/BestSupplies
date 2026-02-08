package dev.joshlucem.nullithstudios.bestsupplies.service;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.config.ConfigManager;
import dev.joshlucem.nullithstudios.bestsupplies.model.RankDefinition;
import org.bukkit.entity.Player;

import java.util.List;

public class RankService {

    private final BestSupplies plugin;
    private final ConfigManager configManager;

    public RankService(BestSupplies plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Detect the highest rank a player has based on permission priority
     * Returns the first matching rank from the priority list (highest to lowest)
     * Supports multiple permissions per rank (player needs ANY of them)
     */
    public RankDefinition detectRank(Player player) {
        List<String> priority = configManager.getRankPriority();
        
        for (String rankId : priority) {
            RankDefinition rank = configManager.getRank(rankId);
            if (rank == null) {
                continue;
            }
            
            // Default rank has no permission requirement
            if (!rank.hasPermission()) {
                // This is the default rank, return it if no other matches
                continue;
            }
            
            // Check if player has ANY of the permissions for this rank
            for (String permission : rank.getPermissions()) {
                if (permission != null && !permission.isEmpty() && player.hasPermission(permission)) {
                    plugin.debug("Jugador " + player.getName() + " tiene rango: " + rankId + " (permiso: " + permission + ")");
                    return rank;
                }
            }
        }
        
        // No rank matched, return default
        RankDefinition defaultRank = configManager.getRank("default");
        if (defaultRank != null) {
            plugin.debug("Jugador " + player.getName() + " tiene rango: default");
            return defaultRank;
        }
        
        // Fallback: return first rank in priority (should be the lowest)
        if (!priority.isEmpty()) {
            String lastRankId = priority.get(priority.size() - 1);
            return configManager.getRank(lastRankId);
        }
        
        return null;
    }

    /**
     * Get the rank ID for a player
     */
    public String detectRankId(Player player) {
        RankDefinition rank = detectRank(player);
        return rank != null ? rank.getId() : "default";
    }

    /**
     * Get a specific rank by ID
     */
    public RankDefinition getRank(String rankId) {
        return configManager.getRank(rankId);
    }

    /**
     * Get the display name of a player's rank
     */
    public String getRankDisplayName(Player player) {
        RankDefinition rank = detectRank(player);
        return rank != null ? rank.getDisplayName() : "Aventurero";
    }

    /**
     * Get the weekly money amount for a player's rank
     */
    public double getWeeklyMoney(Player player) {
        RankDefinition rank = detectRank(player);
        return rank != null ? rank.getWeeklyMoney() : 0;
    }

    /**
     * Get the food cooldown in milliseconds for a player's rank
     */
    public long getFoodCooldownMs(Player player) {
        RankDefinition rank = detectRank(player);
        return rank != null ? rank.getFoodCooldownMs() : 24 * 60 * 60 * 1000L;
    }

    /**
     * Check if player has access to a specific rank
     */
    public boolean hasRank(Player player, String rankId) {
        RankDefinition targetRank = configManager.getRank(rankId);
        if (targetRank == null) {
            return false;
        }
        
        if (!targetRank.hasPermission()) {
            return true; // Default rank accessible by everyone
        }
        
        // Check if player has ANY of the permissions
        for (String permission : targetRank.getPermissions()) {
            if (permission != null && !permission.isEmpty() && player.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }
}
