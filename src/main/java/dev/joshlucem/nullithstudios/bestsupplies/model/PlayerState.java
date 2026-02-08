package dev.joshlucem.nullithstudios.bestsupplies.model;

public class PlayerState {
    
    private final String playerUuid;
    private int streak;
    private String lastDailyDate;
    private String lastSeenDate;
    private String lastRank;

    public PlayerState(String playerUuid) {
        this.playerUuid = playerUuid;
        this.streak = 0;
        this.lastDailyDate = null;
        this.lastSeenDate = null;
        this.lastRank = null;
    }

    public PlayerState(String playerUuid, int streak, String lastDailyDate, String lastSeenDate, String lastRank) {
        this.playerUuid = playerUuid;
        this.streak = streak;
        this.lastDailyDate = lastDailyDate;
        this.lastSeenDate = lastSeenDate;
        this.lastRank = lastRank;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = streak;
    }

    public void incrementStreak() {
        this.streak++;
    }

    public void resetStreak() {
        this.streak = 0;
    }

    public String getLastDailyDate() {
        return lastDailyDate;
    }

    public void setLastDailyDate(String lastDailyDate) {
        this.lastDailyDate = lastDailyDate;
    }

    public String getLastSeenDate() {
        return lastSeenDate;
    }

    public void setLastSeenDate(String lastSeenDate) {
        this.lastSeenDate = lastSeenDate;
    }

    public String getLastRank() {
        return lastRank;
    }

    public void setLastRank(String lastRank) {
        this.lastRank = lastRank;
    }
}
