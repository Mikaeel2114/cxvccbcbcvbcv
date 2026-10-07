package dev.apexban.bungee;

import dev.apexban.core.platform.OnlinePlayer;
import dev.apexban.core.platform.Platform;
import dev.apexban.core.platform.PlatformLogger;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

final class BungeePlatform implements Platform {
    private final Plugin plugin;
    private final PlatformLogger logger;

    BungeePlatform(Plugin plugin) {
        this.plugin = plugin;
        this.logger = new BungeeLogger(plugin.getLogger());
    }

    @Override
    public PlatformLogger logger() {
        return logger;
    }

    @Override
    public File dataFolder() {
        return plugin.getDataFolder();
    }

    @Override
    public String platformName() {
        return "BungeeCord (" + ProxyServer.getInstance().getName() + ")";
    }

    @Override
    public void runSync(Runnable task) {
        task.run();
    }

    @Override
    public OnlinePlayer findOnlinePlayer(String name) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(name);
        return player == null ? null : new BungeeOnlinePlayer(player);
    }

    @Override
    public OnlinePlayer findOnlinePlayer(UUID uuid) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        return player == null ? null : new BungeeOnlinePlayer(player);
    }

    @Override
    public Collection<OnlinePlayer> onlinePlayers() {
        List<OnlinePlayer> players = new ArrayList<OnlinePlayer>();
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            players.add(new BungeeOnlinePlayer(player));
        }
        return players;
    }

    @Override
    public void broadcast(String legacyText, String permission) {
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (permission == null || player.hasPermission(permission)) {
                BungeeText.send(player, legacyText);
            }
        }
        BungeeText.send(ProxyServer.getInstance().getConsole(), legacyText);
    }
}
