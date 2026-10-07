package dev.apexban.bukkit;

import dev.apexban.core.platform.OnlinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

final class BukkitOnlinePlayer implements OnlinePlayer {
    private final Player player;

    BukkitOnlinePlayer(Player player) {
        this.player = player;
    }

    @Override
    public UUID uuid() {
        return player.getUniqueId();
    }

    @Override
    public String name() {
        return player.getName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public void sendMessage(String legacyText) {
        BukkitText.send(player, legacyText);
    }

    @Override
    public void kick(String legacyText) {
        player.kickPlayer(legacyText);
    }
}
