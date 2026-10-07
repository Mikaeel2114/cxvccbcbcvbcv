package dev.apexban.core.model;

import java.util.UUID;

public final class Target {
    private final UUID uuid;
    private final String name;

    public Target(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }
}
