package dev.apexban.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.apexban.core.platform.CommandActor;

import java.util.UUID;

final class VelocityActor implements CommandActor {
    private final CommandSource source;

    VelocityActor(CommandSource source) {
        this.source = source;
    }

    @Override
    public String name() {
        return source instanceof Player ? ((Player) source).getUsername() : "Console";
    }

    @Override
    public UUID uuid() {
        return source instanceof Player ? ((Player) source).getUniqueId() : null;
    }

    @Override
    public boolean isConsole() {
        return !(source instanceof Player);
    }

    @Override
    public boolean hasPermission(String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public void sendMessage(String legacyText) {
        VelocityText.send(source, legacyText);
    }
}
