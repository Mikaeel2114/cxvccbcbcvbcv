package dev.apexban.bungee;

import dev.apexban.core.platform.OnlinePlayer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

final class BungeeOnlinePlayer implements OnlinePlayer {
    private final ProxiedPlayer player;

    BungeeOnlinePlayer(ProxiedPlayer player) {
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
        BungeeText.send(player, legacyText);
    }

    @Override
    public void kick(String legacyText) {
        player.disconnect(BungeeText.component(legacyText));
    }
}
