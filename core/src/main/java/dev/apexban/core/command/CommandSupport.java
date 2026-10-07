package dev.apexban.core.command;

import dev.apexban.core.ApexCore;
import dev.apexban.core.platform.OnlinePlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class CommandSupport {
    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_.\\-]{1,32}$");
    private static final Pattern UNSAFE = Pattern.compile("[\\p{Cntrl}\u00a7]");
    private static final int MAX_REASON_LENGTH = 256;

    private CommandSupport() {
    }

    static boolean validName(String input) {
        return input != null && NAME.matcher(input).matches();
    }

    static String reason(String[] args, int from, String fallback) {
        if (args.length <= from) {
            return fallback;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        String cleaned = UNSAFE.matcher(builder.toString()).replaceAll("").trim();
        if (cleaned.isEmpty()) {
            return fallback;
        }
        return cleaned.length() > MAX_REASON_LENGTH ? cleaned.substring(0, MAX_REASON_LENGTH) : cleaned;
    }

    static List<String> onlineNames(ApexCore core, String prefix) {
        List<String> names = new ArrayList<String>();
        for (OnlinePlayer player : core.platform().onlinePlayers()) {
            names.add(player.name());
        }
        return filter(names, prefix);
    }

    static List<String> filter(Collection<String> options, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<String>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
