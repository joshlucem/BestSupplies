package dev.joshlucem.nullithstudios.bestsupplies.service;

import dev.joshlucem.nullithstudios.bestsupplies.BestSupplies;
import dev.joshlucem.nullithstudios.bestsupplies.config.ConfigManager;
import dev.joshlucem.nullithstudios.bestsupplies.model.RationDefinition;
import dev.joshlucem.nullithstudios.bestsupplies.storage.Database;
import dev.joshlucem.nullithstudios.bestsupplies.util.ItemParser;
import dev.joshlucem.nullithstudios.bestsupplies.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class FoodService {

    private static final long ONE_TIME_CLAIMED = Long.MAX_VALUE;

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

    public List<RationDefinition> getAllRations() {
        return configManager.getRations();
    }

    public RationDefinition getRation(String rationId) {
        return configManager.getRation(rationId);
    }

    public boolean canAccessRation(Player player, RationDefinition ration) {
        if (ration == null) {
            return false;
        }
        String requiredMinRank = ration.getRequiredMinRank();
        if (requiredMinRank == null || requiredMinRank.isBlank()) {
            return true;
        }
        return rankService.isAtLeastRank(player, requiredMinRank);
    }

    public boolean canAccessRation(Player player, String rationId) {
        RationDefinition ration = getRation(rationId);
        return canAccessRation(player, ration);
    }

    public RationStatus getRationStatus(Player player, String rationId) {
        RationDefinition ration = getRation(rationId);
        if (ration == null) {
            return RationStatus.LOCKED;
        }
        return getRationStatus(player, ration);
    }

    public RationStatus getRationStatus(Player player, RationDefinition ration) {
        if (ration == null) {
            return RationStatus.LOCKED;
        }

        if (!canAccessRation(player, ration)) {
            return RationStatus.LOCKED;
        }

        long nextClaimAt = database.getFoodClaimNextAt(player.getUniqueId().toString(), ration.getId());

        if (ration.isOneTime()) {
            return nextClaimAt == ONE_TIME_CLAIMED ? RationStatus.CLAIMED : RationStatus.READY;
        }

        return System.currentTimeMillis() >= nextClaimAt ? RationStatus.READY : RationStatus.COOLDOWN;
    }

    public long getTimeUntilAvailable(Player player, String rationId) {
        RationDefinition ration = getRation(rationId);
        if (ration == null) {
            return 0;
        }

        long nextClaimAt = database.getFoodClaimNextAt(player.getUniqueId().toString(), ration.getId());
        if (ration.isOneTime()) {
            return nextClaimAt == ONE_TIME_CLAIMED ? Long.MAX_VALUE : 0;
        }

        long now = System.currentTimeMillis();
        if (now >= nextClaimAt) {
            return 0;
        }
        return nextClaimAt - now;
    }

    public String formatTimeUntilAvailable(Player player, String rationId) {
        long millis = getTimeUntilAvailable(player, rationId);
        if (millis == Long.MAX_VALUE) {
            return "agotada";
        }
        return timeService.formatMillis(millis);
    }

    public boolean hasAnyReadyRation(Player player) {
        for (RationDefinition ration : getAllRations()) {
            if (getRationStatus(player, ration) == RationStatus.READY) {
                return true;
            }
        }
        return false;
    }

    public ClaimResult claimRation(Player player, String rationId) {
        RationDefinition ration = getRation(rationId);
        if (ration == null) {
            return ClaimResult.NOT_FOUND;
        }

        if (!canAccessRation(player, ration)) {
            return ClaimResult.LOCKED;
        }

        RationStatus status = getRationStatus(player, ration);
        if (status == RationStatus.CLAIMED) {
            return ClaimResult.ALREADY_CLAIMED;
        }
        if (status == RationStatus.COOLDOWN) {
            return ClaimResult.COOLDOWN;
        }
        if (status == RationStatus.LOCKED) {
            return ClaimResult.LOCKED;
        }

        String rankId = rankService.detectRankId(player);
        List<String> rewardStrings = buildRewardList(ration, rankId);
        List<ItemStack> rewards = ItemParser.parseItems(rewardStrings);
        rewardService.giveItemsOrPending(player, rewards);

        long nextClaimAt;
        if (ration.isOneTime()) {
            nextClaimAt = ONE_TIME_CLAIMED;
        } else {
            nextClaimAt = System.currentTimeMillis() + Math.max(0, ration.getCooldownMs());
        }

        database.setFoodClaimNextAt(player.getUniqueId().toString(), ration.getId(), nextClaimAt);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%pack%", ration.getDisplayName());
        Text.sendPrefixed(player, configManager.getMessage("food.claim-success", placeholders), configManager);

        plugin.debug("Racion reclamada por " + player.getName() + ": " + ration.getId());

        return ClaimResult.SUCCESS;
    }

    private List<String> buildRewardList(RationDefinition ration, String rankId) {
        List<String> rewards = new ArrayList<>();

        List<String> fixed = ration.resolveRewards(rankId);
        if (fixed != null) {
            rewards.addAll(fixed);
        }

        List<String> randomPool = ration.resolveRandomRewards(rankId);
        if (randomPool != null && !randomPool.isEmpty() && ration.getRandomPicks() > 0) {
            List<String> copy = new ArrayList<>(randomPool);
            Collections.shuffle(copy, ThreadLocalRandom.current());
            int picks = Math.min(ration.getRandomPicks(), copy.size());
            rewards.addAll(copy.subList(0, picks));
        }

        return rewards;
    }

    public void resetFoodCooldown(Player target, String rationId) {
        if (rationId == null || rationId.isEmpty()) {
            database.resetAllFoodClaims(target.getUniqueId().toString());
            plugin.debug("Todos los cooldowns de raciones reseteados para " + target.getName());
            return;
        }

        database.resetFoodClaim(target.getUniqueId().toString(), rationId);
        plugin.debug("Cooldown de racion reseteado para " + target.getName() + ": " + rationId);
    }

    public enum RationStatus {
        READY,
        COOLDOWN,
        LOCKED,
        CLAIMED
    }

    public enum ClaimResult {
        SUCCESS,
        COOLDOWN,
        LOCKED,
        ALREADY_CLAIMED,
        NOT_FOUND
    }
}
