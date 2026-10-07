package dev.apexban.core.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts configuration text that mixes MiniMessage tags and legacy '&' codes into legacy
 * section-sign strings, which every supported platform (1.8 to latest) can display.
 */
public final class TextFormatter {
    private static final Pattern HEX = Pattern.compile("[&\u00a7]#([0-9a-fA-F]{6})");
    private static final Pattern LEGACY = Pattern.compile("[&\u00a7]([0-9a-fA-Fk-oK-OrR])");
    private static final Pattern LEGACY_FALLBACK = Pattern.compile("&([0-9a-fA-Fk-oK-OrR])");

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer serializer;

    public TextFormatter(boolean hexColors) {
        if (hexColors) {
            this.serializer = LegacyComponentSerializer.builder().character('\u00a7').hexColors().build();
        } else {
            this.serializer = LegacyComponentSerializer.legacySection();
        }
    }

    public String format(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        try {
            Component component = miniMessage.deserialize(convertLegacy(input));
            return serializer.serialize(component);
        } catch (RuntimeException ex) {
            return LEGACY_FALLBACK.matcher(input).replaceAll("\u00a7$1");
        }
    }

    private static String convertLegacy(String input) {
        Matcher hex = HEX.matcher(input);
        StringBuffer withHex = new StringBuffer();
        while (hex.find()) {
            hex.appendReplacement(withHex, Matcher.quoteReplacement("<reset><#" + hex.group(1) + ">"));
        }
        hex.appendTail(withHex);

        Matcher legacy = LEGACY.matcher(withHex.toString());
        StringBuffer out = new StringBuffer();
        while (legacy.find()) {
            char code = Character.toLowerCase(legacy.group(1).charAt(0));
            legacy.appendReplacement(out, Matcher.quoteReplacement(tagFor(code)));
        }
        legacy.appendTail(out);
        return out.toString();
    }

    private static String tagFor(char code) {
        switch (code) {
            case '0':
                return "<reset><black>";
            case '1':
                return "<reset><dark_blue>";
            case '2':
                return "<reset><dark_green>";
            case '3':
                return "<reset><dark_aqua>";
            case '4':
                return "<reset><dark_red>";
            case '5':
                return "<reset><dark_purple>";
            case '6':
                return "<reset><gold>";
            case '7':
                return "<reset><gray>";
            case '8':
                return "<reset><dark_gray>";
            case '9':
                return "<reset><blue>";
            case 'a':
                return "<reset><green>";
            case 'b':
                return "<reset><aqua>";
            case 'c':
                return "<reset><red>";
            case 'd':
                return "<reset><light_purple>";
            case 'e':
                return "<reset><yellow>";
            case 'f':
                return "<reset><white>";
            case 'k':
                return "<obfuscated>";
            case 'l':
                return "<bold>";
            case 'm':
                return "<strikethrough>";
            case 'n':
                return "<underlined>";
            case 'o':
                return "<italic>";
            default:
                return "<reset>";
        }
    }
}
