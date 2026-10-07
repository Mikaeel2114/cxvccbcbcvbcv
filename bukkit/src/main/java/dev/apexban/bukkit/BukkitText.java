package dev.apexban.bukkit;

import org.bukkit.command.CommandSender;

final class BukkitText {
    private BukkitText() {
    }

    static void send(CommandSender target, String text) {
        for (String line : text.split("\n", -1)) {
            target.sendMessage(line);
        }
    }
}
