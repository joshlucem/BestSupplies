package dev.joshlucem.nullithstudios.bestsupplies.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RankDefinition {
    
    private final String id;
    private final List<String> permissions;
    private final String displayName;
    private final double weeklyMoney;
    private final long foodCooldownMs;
    private final Map<String, FoodPackDefinition> packs;

    public RankDefinition(String id, List<String> permissions, String displayName, 
                          double weeklyMoney, long foodCooldownMs, Map<String, FoodPackDefinition> packs) {
        this.id = id;
        this.permissions = permissions != null ? permissions : new ArrayList<>();
        this.displayName = displayName;
        this.weeklyMoney = weeklyMoney;
        this.foodCooldownMs = foodCooldownMs;
        this.packs = packs;
    }

    public String getId() {
        return id;
    }

    /**
     * Get the first permission (for backwards compatibility)
     */
    public String getPermission() {
        return permissions.isEmpty() ? "" : permissions.get(0);
    }

    /**
     * Get all permissions (player needs ANY of these)
     */
    public List<String> getPermissions() {
        return permissions;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getWeeklyMoney() {
        return weeklyMoney;
    }

    public long getFoodCooldownMs() {
        return foodCooldownMs;
    }

    public Map<String, FoodPackDefinition> getPacks() {
        return packs;
    }

    public FoodPackDefinition getPack(String packId) {
        return packs.get(packId);
    }

    public boolean hasPermission() {
        return !permissions.isEmpty() && permissions.stream().anyMatch(p -> p != null && !p.isEmpty());
    }
}
