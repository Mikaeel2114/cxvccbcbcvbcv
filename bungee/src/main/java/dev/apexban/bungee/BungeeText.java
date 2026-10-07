package dev.apexban.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

final class BungeeText {
    private BungeeText() {
    }

    static void send(CommandSender target, String text) {
        for (String line : text.split("\n", -1)) {
            target.sendMessage(TextComponent.fromLegacyText(line));
        }
    }

    static BaseComponent[] component(String text) {
        return TextComponent.fromLegacyText(text);
    }
}
