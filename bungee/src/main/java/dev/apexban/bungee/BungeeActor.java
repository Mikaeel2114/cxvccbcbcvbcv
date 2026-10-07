package dev.apexban.bungee;

import dev.apexban.core.platform.CommandActor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

final class BungeeActor implements CommandActor {
    private final CommandSender sender;

    BungeeActor(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public String name() {
        return sender instanceof ProxiedPlayer ? sender.getName() : "Console";
    }

    @Override
    public UUID uuid() {
        return sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getUniqueId() : null;
    }

    @Override
    public boolean isConsole() {
        return !(sender instanceof ProxiedPlayer);
    }

    @Override
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }

    @Override
    public void sendMessage(String legacyText) {
        BungeeText.send(sender, legacyText);
    }
}
