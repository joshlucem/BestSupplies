package dev.joshlucem.nullithstudios.bestsupplies.model;

import java.util.List;

public class StreakMilestone {
    
    private final int streakDay;
    private final double bonusMoney;
    private final List<String> bonusItems;
    private final String message;

    public StreakMilestone(int streakDay, double bonusMoney, List<String> bonusItems, String message) {
        this.streakDay = streakDay;
        this.bonusMoney = bonusMoney;
        this.bonusItems = bonusItems;
        this.message = message;
    }

    public int getStreakDay() {
        return streakDay;
    }

    public double getBonusMoney() {
        return bonusMoney;
    }

    public List<String> getBonusItems() {
        return bonusItems;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasBonusMoney() {
        return bonusMoney > 0;
    }

    public boolean hasBonusItems() {
        return bonusItems != null && !bonusItems.isEmpty();
    }
}
