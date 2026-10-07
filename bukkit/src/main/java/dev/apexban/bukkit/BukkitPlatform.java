package dev.apexban.bukkit;

import dev.apexban.core.platform.OnlinePlayer;
import dev.apexban.core.platform.Platform;
import dev.apexban.core.platform.PlatformLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

final class BukkitPlatform implements Platform {
    private final JavaPlugin plugin;
    private final PlatformLogger logger;

    BukkitPlatform(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = new BukkitLogger(plugin.getLogger());
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
        return "Bukkit (" + Bukkit.getName() + ")";
    }

    @Override
    public void runSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    @Override
    public OnlinePlayer findOnlinePlayer(String name) {
        Player player = Bukkit.getPlayerExact(name);
        return player == null ? null : new BukkitOnlinePlayer(player);
    }

    @Override
    public OnlinePlayer findOnlinePlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player == null ? null : new BukkitOnlinePlayer(player);
    }

    @Override
    public Collection<OnlinePlayer> onlinePlayers() {
        List<OnlinePlayer> players = new ArrayList<OnlinePlayer>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            players.add(new BukkitOnlinePlayer(player));
        }
        return players;
    }

    @Override
    public void broadcast(String legacyText, String permission) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (permission == null || player.hasPermission(permission)) {
                BukkitText.send(player, legacyText);
            }
        }
        BukkitText.send(Bukkit.getConsoleSender(), legacyText);
    }
}
