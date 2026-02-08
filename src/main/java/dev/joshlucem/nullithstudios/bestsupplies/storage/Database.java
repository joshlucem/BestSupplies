package dev.joshlucem.nullithstudios.bestsupplies.storage;

import dev.joshlucem.nullithstudios.bestsupplies.model.ChequeData;
import dev.joshlucem.nullithstudios.bestsupplies.model.PendingEntry;
import dev.joshlucem.nullithstudios.bestsupplies.model.PlayerState;

import java.util.List;

public interface Database {
    
    void initialize();
    
    void close();
    
    // Player State
    PlayerState getPlayerState(String playerUuid);
    void savePlayerState(PlayerState state);
    
    // Daily Claims
    boolean hasDailyClaim(String playerUuid, String date);
    void setDailyClaim(String playerUuid, String date, boolean claimed);
    
    // Weekly Claims
    boolean hasWeeklyClaim(String playerUuid, String weekKey);
    void setWeeklyClaim(String playerUuid, String weekKey, boolean claimed);
    void resetWeeklyClaim(String playerUuid, String weekKey);
    
    // Food Claims
    long getFoodClaimNextAt(String playerUuid, String packId);
    void setFoodClaimNextAt(String playerUuid, String packId, long nextClaimAt);
    void resetFoodClaim(String playerUuid, String packId);
    void resetAllFoodClaims(String playerUuid);
    
    // Cheques
    void saveCheque(ChequeData cheque);
    ChequeData getCheque(String chequeId);
    void redeemCheque(String chequeId, long redeemedAt);
    
    // Pending
    void addPending(String playerUuid, PendingEntry.PendingType type, String payload);
    List<PendingEntry> getPendingEntries(String playerUuid);
    void removePending(int id);
    int getPendingCount(String playerUuid);
}
