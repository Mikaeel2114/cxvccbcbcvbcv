package dev.apexban.core.platform;

import java.io.File;
import java.util.Collection;
import java.util.UUID;

public interface Platform {
    PlatformLogger logger();

    File dataFolder();

    String platformName();

    /**
     * Runs the task on the platform's main thread. Proxies without a main thread run it immediately.
     */
    void runSync(Runnable task);

    OnlinePlayer findOnlinePlayer(String name);

    OnlinePlayer findOnlinePlayer(UUID uuid);

    Collection<OnlinePlayer> onlinePlayers();

    /**
     * Sends the text to every online player holding the permission (everyone when the permission is null)
     * and always to the console.
     */
    void broadcast(String legacyText, String permission);
}
