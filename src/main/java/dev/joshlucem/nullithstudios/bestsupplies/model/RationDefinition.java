package dev.joshlucem.nullithstudios.bestsupplies.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RationDefinition {

    private final String id;
    private final int order;
    private final String icon;
    private final String displayName;
    private final List<String> description;
    private final boolean oneTime;
    private final long cooldownMs;
    private final String requiredMinRank;
    private final List<String> baseRewards;
    private final Map<String, List<String>> rewardsByRank;
    private final List<String> randomRewards;
    private final Map<String, List<String>> randomRewardsByRank;
    private final int randomPicks;

    public RationDefinition(String id,
                            int order,
                            String icon,
                            String displayName,
                            List<String> description,
                            boolean oneTime,
                            long cooldownMs,
                            String requiredMinRank,
                            List<String> baseRewards,
                            Map<String, List<String>> rewardsByRank,
                            List<String> randomRewards,
                            Map<String, List<String>> randomRewardsByRank,
                            int randomPicks) {
        this.id = id;
        this.order = order;
        this.icon = icon;
        this.displayName = displayName;
        this.description = description != null ? description : List.of();
        this.oneTime = oneTime;
        this.cooldownMs = cooldownMs;
        this.requiredMinRank = requiredMinRank;
        this.baseRewards = baseRewards != null ? baseRewards : List.of();
        this.rewardsByRank = rewardsByRank != null ? rewardsByRank : Map.of();
        this.randomRewards = randomRewards != null ? randomRewards : List.of();
        this.randomRewardsByRank = randomRewardsByRank != null ? randomRewardsByRank : Map.of();
        this.randomPicks = Math.max(0, randomPicks);
    }

    public String getId() {
        return id;
    }

    public int getOrder() {
        return order;
    }

    public String getIcon() {
        return icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getDescription() {
        return Collections.unmodifiableList(description);
    }

    public boolean isOneTime() {
        return oneTime;
    }

    public long getCooldownMs() {
        return cooldownMs;
    }

    public String getRequiredMinRank() {
        return requiredMinRank;
    }

    public List<String> getBaseRewards() {
        return Collections.unmodifiableList(baseRewards);
    }

    public Map<String, List<String>> getRewardsByRank() {
        return Collections.unmodifiableMap(rewardsByRank);
    }

    public List<String> getRandomRewards() {
        return Collections.unmodifiableList(randomRewards);
    }

    public Map<String, List<String>> getRandomRewardsByRank() {
        return Collections.unmodifiableMap(randomRewardsByRank);
    }

    public int getRandomPicks() {
        return randomPicks;
    }

    public List<String> resolveRewards(String rankId) {
        if (rankId != null && rewardsByRank.containsKey(rankId)) {
            return rewardsByRank.get(rankId);
        }
        return baseRewards;
    }

    public List<String> resolveRandomRewards(String rankId) {
        if (rankId != null && randomRewardsByRank.containsKey(rankId)) {
            return randomRewardsByRank.get(rankId);
        }
        return randomRewards;
    }
}
