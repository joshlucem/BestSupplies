package dev.joshlucem.nullithstudios.bestsupplies.model;

import java.util.List;

public class StreakMilestone {
    
    private final int streakDay;
    private final double bonusSilver;
    private final double bonusGold;
    private final List<String> bonusItems;
    private final String message;

    public StreakMilestone(int streakDay, double bonusSilver, double bonusGold, List<String> bonusItems, String message) {
        this.streakDay = streakDay;
        this.bonusSilver = bonusSilver;
        this.bonusGold = bonusGold;
        this.bonusItems = bonusItems;
        this.message = message;
    }

    public int getStreakDay() {
        return streakDay;
    }

    public double getBonusSilver() {
        return bonusSilver;
    }

    public double getBonusGold() {
        return bonusGold;
    }

    public List<String> getBonusItems() {
        return bonusItems;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasBonusSilver() {
        return bonusSilver > 0;
    }

    public boolean hasBonusGold() {
        return bonusGold > 0;
    }

    public boolean hasBonusItems() {
        return bonusItems != null && !bonusItems.isEmpty();
    }
}
