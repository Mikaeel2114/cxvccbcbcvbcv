package dev.apexban.core.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    private static final long MAX_MILLIS = 100L * 365L * 24L * 60L * 60L * 1000L;
    private static final Pattern SHAPE = Pattern.compile("^(\\d{1,9}[a-zA-Z]+)+$");
    private static final Pattern TOKEN = Pattern.compile("(\\d{1,9})([a-zA-Z]+)");
    private static final Set<String> PERMANENT_KEYWORDS = new HashSet<String>(
            Arrays.asList("perm", "permanent", "permanently", "forever"));

    private DurationParser() {
    }

    public static boolean isPermanentKeyword(String token) {
        return token != null && PERMANENT_KEYWORDS.contains(token.toLowerCase(Locale.ROOT));
    }

    /**
     * True when the token has the shape of a duration such as "7d" or "1h30m", even if a unit is unknown.
     */
    public static boolean looksLikeDuration(String token) {
        return token != null && SHAPE.matcher(token).matches();
    }

    public static boolean isDuration(String token) {
        if (!looksLikeDuration(token)) {
            return false;
        }
        try {
            parse(token);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Parses values like 30m, 2h, 7d, 1w, 2mo, 1y or combinations such as 1d12h30m into milliseconds.
     *
     * @throws IllegalArgumentException when the value is malformed, uses an unknown unit, is zero or too large
     */
    public static long parse(String input) {
        if (!looksLikeDuration(input)) {
            throw new IllegalArgumentException("Not a duration: " + input);
        }
        Matcher matcher = TOKEN.matcher(input);
        long total = 0L;
        int expectedStart = 0;
        while (matcher.find()) {
            if (matcher.start() != expectedStart) {
                throw new IllegalArgumentException("Malformed duration: " + input);
            }
            expectedStart = matcher.end();
            long amount = Long.parseLong(matcher.group(1));
            long unit = unitMillis(matcher.group(2));
            if (unit <= 0L) {
                throw new IllegalArgumentException("Unknown unit: " + matcher.group(2));
            }
            try {
                total = Math.addExact(total, Math.multiplyExact(amount, unit));
            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException("Duration too large: " + input);
            }
            if (total > MAX_MILLIS) {
                throw new IllegalArgumentException("Duration too large: " + input);
            }
        }
        if (expectedStart != input.length() || total <= 0L) {
            throw new IllegalArgumentException("Malformed duration: " + input);
        }
        return total;
    }

    private static long unitMillis(String unit) {
        switch (unit.toLowerCase(Locale.ROOT)) {
            case "s":
            case "sec":
            case "secs":
            case "second":
            case "seconds":
                return 1000L;
            case "m":
            case "min":
            case "mins":
            case "minute":
            case "minutes":
                return 60L * 1000L;
            case "h":
            case "hr":
            case "hrs":
            case "hour":
            case "hours":
                return 60L * 60L * 1000L;
            case "d":
            case "day":
            case "days":
                return 24L * 60L * 60L * 1000L;
            case "w":
            case "wk":
            case "wks":
            case "week":
            case "weeks":
                return 7L * 24L * 60L * 60L * 1000L;
            case "mo":
            case "mon":
            case "month":
            case "months":
                return 30L * 24L * 60L * 60L * 1000L;
            case "y":
            case "yr":
            case "yrs":
            case "year":
            case "years":
                return 365L * 24L * 60L * 60L * 1000L;
            default:
                return -1L;
        }
    }
}
