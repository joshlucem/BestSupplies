package dev.joshlucem.nullithstudios.bestsupplies.model;

import java.util.UUID;

public class ChequeData {
    
    private final String chequeId;
    private final String playerUuid;
    private final String weekKey;
    private final double amount;
    private boolean redeemed;
    private long redeemedAt;

    public ChequeData(String chequeId, String playerUuid, String weekKey, double amount) {
        this.chequeId = chequeId;
        this.playerUuid = playerUuid;
        this.weekKey = weekKey;
        this.amount = amount;
        this.redeemed = false;
        this.redeemedAt = 0;
    }

    public ChequeData(String chequeId, String playerUuid, String weekKey, double amount, boolean redeemed, long redeemedAt) {
        this.chequeId = chequeId;
        this.playerUuid = playerUuid;
        this.weekKey = weekKey;
        this.amount = amount;
        this.redeemed = redeemed;
        this.redeemedAt = redeemedAt;
    }

    public String getChequeId() {
        return chequeId;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getWeekKey() {
        return weekKey;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isRedeemed() {
        return redeemed;
    }

    public void setRedeemed(boolean redeemed) {
        this.redeemed = redeemed;
    }

    public long getRedeemedAt() {
        return redeemedAt;
    }

    public void setRedeemedAt(long redeemedAt) {
        this.redeemedAt = redeemedAt;
    }

    public static String generateChequeId() {
        return UUID.randomUUID().toString();
    }
}
