package dev.joshlucem.nullithstudios.bestsupplies.model;

public class PendingEntry {
    
    public enum PendingType {
        ITEM,
        CHEQUE
    }
    
    private final int id;
    private final String playerUuid;
    private final PendingType type;
    private final String payload;
    private final long createdAt;

    public PendingEntry(int id, String playerUuid, PendingType type, String payload, long createdAt) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.type = type;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public PendingType getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
