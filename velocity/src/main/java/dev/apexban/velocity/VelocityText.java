package dev.apexban.velocity;

import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

final class VelocityText {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('\u00a7')
            .hexColors()
            .build();

    private VelocityText() {
    }

    static Component component(String legacyText) {
        return LEGACY.deserialize(legacyText);
    }

    static void send(CommandSource target, String text) {
        for (String line : text.split("\n", -1)) {
            target.sendMessage(component(line));
        }
    }
}
