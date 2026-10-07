package dev.apexban.core.platform;

import java.util.UUID;

public interface OnlinePlayer {
    UUID uuid();

    String name();

    boolean hasPermission(String permission);

    void sendMessage(String legacyText);

    void kick(String legacyText);
}
