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

    public RankDefinition detectRank(Player player) {
        List<String> priority = configManager.getRankPriority();

        for (String rankId : priority) {
            RankDefinition rank = configManager.getRank(rankId);
            if (rank == null) {
                continue;
            }

            if (!rank.hasPermission()) {
                continue;
            }

            for (String permission : rank.getPermissions()) {
                if (permission != null && !permission.isEmpty() && player.hasPermission(permission)) {
                    plugin.debug("Jugador " + player.getName() + " tiene rango: " + rankId + " (permiso: " + permission + ")");
                    return rank;
                }
            }
        }

        RankDefinition defaultRank = configManager.getRank("default");
        if (defaultRank != null) {
            plugin.debug("Jugador " + player.getName() + " tiene rango: default");
            return defaultRank;
        }

        if (!priority.isEmpty()) {
            String lastRankId = priority.get(priority.size() - 1);
            return configManager.getRank(lastRankId);
        }

        return null;
    }

    public String detectRankId(Player player) {
        RankDefinition rank = detectRank(player);
        return rank != null ? rank.getId() : "default";
    }

    public RankDefinition getRank(String rankId) {
        return configManager.getRank(rankId);
    }

    public String getRankDisplayName(Player player) {
        RankDefinition rank = detectRank(player);
        return rank != null ? rank.getDisplayName() : "Aventurero";
    }

    public String getRankTag(Player player) {
        RankDefinition rank = detectRank(player);
        String rankId = rank != null ? rank.getId() : "default";
        return configManager.getRankTag(rankId);
    }

    public double getWeeklyMoney(Player player) {
        RankDefinition rank = detectRank(player);
        return rank != null ? rank.getWeeklyMoney() : 0;
    }

    public long getFoodCooldownMs(Player player) {
        RankDefinition rank = detectRank(player);
        return rank != null ? rank.getFoodCooldownMs() : 24 * 60 * 60 * 1000L;
    }

    public boolean hasRank(Player player, String rankId) {
        RankDefinition targetRank = configManager.getRank(rankId);
        if (targetRank == null) {
            return false;
        }

        if (!targetRank.hasPermission()) {
            return true;
        }

        for (String permission : targetRank.getPermissions()) {
            if (permission != null && !permission.isEmpty() && player.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAtLeastRank(Player player, String requiredMinRank) {
        String playerRankId = detectRankId(player);
        return isAtLeastRank(playerRankId, requiredMinRank);
    }

    public boolean isAtLeastRank(String playerRankId, String requiredMinRank) {
        if (requiredMinRank == null || requiredMinRank.isBlank()) {
            return true;
        }

        List<String> priority = configManager.getRankPriority();
        int playerIndex = priority.indexOf(playerRankId.toLowerCase());
        int requiredIndex = priority.indexOf(requiredMinRank.toLowerCase());

        if (playerIndex == -1 || requiredIndex == -1) {
            return false;
        }

        return playerIndex <= requiredIndex;
    }
}
