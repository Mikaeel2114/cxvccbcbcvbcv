package dev.apexban.core.model;

import java.util.UUID;

public final class Punishment {
    public static final long PERMANENT = -1L;

    private final long id;
    private final PunishmentType type;
    private final UUID uuid;
    private final String targetName;
    private final String operator;
    private final String reason;
    private final long createdAt;
    private final long expiresAt;
    private final String serverId;

    public Punishment(long id, PunishmentType type, UUID uuid, String targetName, String operator,
                      String reason, long createdAt, long expiresAt, String serverId) {
        this.id = id;
        this.type = type;
        this.uuid = uuid;
        this.targetName = targetName;
        this.operator = operator;
        this.reason = reason;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.serverId = serverId;
    }

    public long getId() {
        return id;
    }

    public PunishmentType getType() {
        return type;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getOperator() {
        return operator;
    }

    public String getReason() {
        return reason;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public String getServerId() {
        return serverId;
    }

    public boolean isPermanent() {
        return expiresAt == PERMANENT;
    }

    public boolean isExpired(long now) {
        return expiresAt != PERMANENT && expiresAt <= now;
    }

    public Punishment withId(long newId) {
        return new Punishment(newId, type, uuid, targetName, operator, reason, createdAt, expiresAt, serverId);
    }
}
