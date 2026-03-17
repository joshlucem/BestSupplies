package dev.joshlucem.nullithstudios.bestsupplies.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RankDefinition {

    private final String id;
    private final List<String> permissions;
    private final String displayName;
    private final double weeklyMoney;
    private final long foodCooldownMs;
    private final Map<String, FoodPackDefinition> packs;

    private final String category;
    private final double monthlyBase;
    private final double monthlyStep;
    private final Map<Integer, Double> monthlyOverrides;

    public RankDefinition(String id,
                          List<String> permissions,
                          String displayName,
                          double weeklyMoney,
                          long foodCooldownMs,
                          Map<String, FoodPackDefinition> packs) {
        this(id, permissions, displayName, weeklyMoney, foodCooldownMs, packs, "user", 0, 0, Map.of());
    }

    public RankDefinition(String id,
                          List<String> permissions,
                          String displayName,
                          double weeklyMoney,
                          long foodCooldownMs,
                          Map<String, FoodPackDefinition> packs,
                          String category,
                          double monthlyBase,
                          double monthlyStep,
                          Map<Integer, Double> monthlyOverrides) {
        this.id = id;
        this.permissions = permissions != null ? permissions : new ArrayList<>();
        this.displayName = displayName;
        this.weeklyMoney = weeklyMoney;
        this.foodCooldownMs = foodCooldownMs;
        this.packs = packs != null ? packs : new HashMap<>();
        this.category = category != null ? category : "user";
        this.monthlyBase = monthlyBase;
        this.monthlyStep = monthlyStep;
        this.monthlyOverrides = monthlyOverrides != null ? monthlyOverrides : Map.of();
    }

    public String getId() {
        return id;
    }

    public String getPermission() {
        return permissions.isEmpty() ? "" : permissions.get(0);
    }

    public List<String> getPermissions() {
        return Collections.unmodifiableList(permissions);
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
        return Collections.unmodifiableMap(packs);
    }

    public FoodPackDefinition getPack(String packId) {
        return packs.get(packId);
    }

    public boolean hasPermission() {
        return !permissions.isEmpty() && permissions.stream().anyMatch(p -> p != null && !p.isEmpty());
    }

    public String getCategory() {
        return category;
    }

    public double getMonthlyBase() {
        return monthlyBase;
    }

    public double getMonthlyStep() {
        return monthlyStep;
    }

    public Map<Integer, Double> getMonthlyOverrides() {
        return Collections.unmodifiableMap(monthlyOverrides);
    }

    public double getMonthlyAmount(int dayOfMonth) {
        if (dayOfMonth <= 0) {
            return 0;
        }
        if (monthlyOverrides.containsKey(dayOfMonth)) {
            return monthlyOverrides.get(dayOfMonth);
        }
        return monthlyBase + ((dayOfMonth - 1) * monthlyStep);
    }
}
