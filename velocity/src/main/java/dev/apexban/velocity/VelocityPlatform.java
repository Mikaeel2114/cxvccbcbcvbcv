package dev.apexban.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.apexban.core.platform.OnlinePlayer;
import dev.apexban.core.platform.Platform;
import dev.apexban.core.platform.PlatformLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class VelocityPlatform implements Platform {
    private final ProxyServer proxy;
    private final File dataFolder;
    private final PlatformLogger logger;

    VelocityPlatform(ProxyServer proxy, File dataFolder, PlatformLogger logger) {
        this.proxy = proxy;
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    @Override
    public PlatformLogger logger() {
        return logger;
    }

    @Override
    public File dataFolder() {
        return dataFolder;
    }

    @Override
    public String platformName() {
        return "Velocity (" + proxy.getVersion().getName() + ")";
    }

    @Override
    public void runSync(Runnable task) {
        task.run();
    }

    @Override
    public OnlinePlayer findOnlinePlayer(String name) {
        Optional<Player> player = proxy.getPlayer(name);
        return player.isPresent() ? new VelocityOnlinePlayer(player.get()) : null;
    }

    @Override
    public OnlinePlayer findOnlinePlayer(UUID uuid) {
        Optional<Player> player = proxy.getPlayer(uuid);
        return player.isPresent() ? new VelocityOnlinePlayer(player.get()) : null;
    }

    @Override
    public Collection<OnlinePlayer> onlinePlayers() {
        List<OnlinePlayer> players = new ArrayList<OnlinePlayer>();
        for (Player player : proxy.getAllPlayers()) {
            players.add(new VelocityOnlinePlayer(player));
        }
        return players;
    }

    @Override
    public void broadcast(String legacyText, String permission) {
        for (Player player : proxy.getAllPlayers()) {
            if (permission == null || player.hasPermission(permission)) {
                VelocityText.send(player, legacyText);
            }
        }
        VelocityText.send(proxy.getConsoleCommandSource(), legacyText);
    }
}
