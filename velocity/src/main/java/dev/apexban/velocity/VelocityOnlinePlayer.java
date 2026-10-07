package dev.apexban.velocity;

import com.velocitypowered.api.proxy.Player;
import dev.apexban.core.platform.OnlinePlayer;

import java.util.UUID;

final class VelocityOnlinePlayer implements OnlinePlayer {
    private final Player player;

    VelocityOnlinePlayer(Player player) {
        this.player = player;
    }

    @Override
    public UUID uuid() {
        return player.getUniqueId();
    }

    @Override
    public String name() {
        return player.getUsername();
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public void sendMessage(String legacyText) {
        VelocityText.send(player, legacyText);
    }

    @Override
    public void kick(String legacyText) {
        player.disconnect(VelocityText.component(legacyText));
    }
}
