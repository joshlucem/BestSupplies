package dev.joshlucem.nullithstudios.bestsupplies.service;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.config.ConfigManager;
import dev.joshlucem.nullithstudios.bestsupplies.model.FoodPackDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.model.RankDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.storage.Database;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class FoodService {

    private final BestSupplies plugin;
    private final Database database;
    private final ConfigManager configManager;
    private final TimeService timeService;
    private final RankService rankService;
    private final RewardService rewardService;

    public FoodService(BestSupplies plugin, Database database, ConfigManager configManager,
                       TimeService timeService, RankService rankService, RewardService rewardService) {
        this.plugin = plugin;
        this.database = database;
        this.configManager = configManager;
        this.timeService = timeService;
        this.rankService = rankService;
        this.rewardService = rewardService;
    }

    /**
     * Get the food packs available for a player based on their rank
     */
    public Map<String, FoodPackDefinition> getAvailablePacks(Player player) {
        RankDefinition rank = rankService.detectRank(player);
        if (rank == null) {
            return new HashMap<>();
        }
        return rank.getPacks();
    }

    /**
     * Get the cooldown duration for a player's rank
     */
    public Duration getCooldownDuration(Player player) {
        RankDefinition rank = rankService.detectRank(player);
        if (rank == null) {
            return Duration.ofHours(24);
        }
        return Duration.ofMillis(rank.getFoodCooldownMs());
    }

    /**
     * Check if a specific pack is available for claiming
     */
    public boolean isPackAvailable(Player player, String packId) {
        long nextClaimAt = database.getFoodClaimNextAt(player.getUniqueId().toString(), packId);
        return System.currentTimeMillis() >= nextClaimAt;
    }

    /**
     * Get time until pack is available (0 if already available)
     */
    public long getTimeUntilAvailable(Player player, String packId) {
        long nextClaimAt = database.getFoodClaimNextAt(player.getUniqueId().toString(), packId);
        long now = System.currentTimeMillis();
        
        if (now >= nextClaimAt) {
            return 0;
        }
        
        return nextClaimAt - now;
    }

    /**
     * Get the status of a pack for a player
     */
    public PackStatus getPackStatus(Player player, String packId) {
        // Check if player has this pack available
        Map<String, FoodPackDefinition> packs = getAvailablePacks(player);
        if (!packs.containsKey(packId)) {
            return PackStatus.NOT_AVAILABLE;
        }

        if (isPackAvailable(player, packId)) {
            return PackStatus.READY;
        }

        return PackStatus.COOLDOWN;
    }

    /**
     * Claim a food pack
     */
    public ClaimResult claimPack(Player player, String packId) {
        // Check if player has this pack available
        Map<String, FoodPackDefinition> packs = getAvailablePacks(player);
        FoodPackDefinition pack = packs.get(packId);
        
        if (pack == null) {
            return ClaimResult.NOT_AVAILABLE;
        }

        // Check cooldown
        if (!isPackAvailable(player, packId)) {
            return ClaimResult.COOLDOWN;
        }

        // Set next claim time
        Duration cooldown = getCooldownDuration(player);
        long nextClaimAt = System.currentTimeMillis() + cooldown.toMillis();
        database.setFoodClaimNextAt(player.getUniqueId().toString(), packId, nextClaimAt);

        // Give items
        rewardService.giveFoodPackItems(player, pack.getItems());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%pack%", pack.getDisplayName());
        Text.sendPrefixed(player, configManager.getMessage("food.claim-success", placeholders), configManager);

        plugin.debug("Pack de comida reclamado por " + player.getName() + ": " + packId);

        return ClaimResult.SUCCESS;
    }

    /**
     * Reset food cooldown for a player (admin)
     */
    public void resetFoodCooldown(Player target, String packId) {
        if (packId == null || packId.isEmpty()) {
            // Reset all packs
            database.resetAllFoodClaims(target.getUniqueId().toString());
            plugin.debug("Todos los cooldowns de comida reseteados para " + target.getName());
        } else {
            // Reset specific pack
            database.resetFoodClaim(target.getUniqueId().toString(), packId);
            plugin.debug("Cooldown de comida reseteado para " + target.getName() + ": " + packId);
        }
    }

    /**
     * Format the cooldown time for display
     */
    public String formatCooldown(Player player) {
        Duration cooldown = getCooldownDuration(player);
        return timeService.formatDuration(cooldown);
    }

    /**
     * Format the time until available for a specific pack
     */
    public String formatTimeUntilAvailable(Player player, String packId) {
        long millis = getTimeUntilAvailable(player, packId);
        return timeService.formatMillis(millis);
    }

    /**
     * Pack status enum
     */
    public enum PackStatus {
        READY,          // Pack is available to claim
        COOLDOWN,       // Pack is on cooldown
        NOT_AVAILABLE   // Pack not available for this rank
    }

    /**
     * Claim result enum
     */
    public enum ClaimResult {
        SUCCESS,
        COOLDOWN,
        NOT_AVAILABLE
    }
}
