package dev.apexban.core.platform;

import java.util.UUID;

public interface CommandActor {
    String name();

    UUID uuid();

    boolean isConsole();

    boolean hasPermission(String permission);

    void sendMessage(String legacyText);
}
