package dev.apexban.bukkit;

import dev.apexban.core.platform.CommandActor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

final class BukkitActor implements CommandActor {
    private final CommandSender sender;

    BukkitActor(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public String name() {
        return sender instanceof Player ? sender.getName() : "Console";
    }

    @Override
    public UUID uuid() {
        return sender instanceof Player ? ((Player) sender).getUniqueId() : null;
    }

    @Override
    public boolean isConsole() {
        return !(sender instanceof Player);
    }

    @Override
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }

    @Override
    public void sendMessage(String legacyText) {
        BukkitText.send(sender, legacyText);
    }
}
