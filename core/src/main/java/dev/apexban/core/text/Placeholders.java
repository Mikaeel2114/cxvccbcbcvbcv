package dev.apexban.core.text;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Placeholders {
    private static final Pattern PATTERN = Pattern.compile("%([A-Za-z0-9_\\-]+)%");

    private Placeholders() {
    }

    public static Map<String, String> of(String... keyValuePairs) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            map.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return map;
    }

    /**
     * Replaces %key% tokens in a single pass. Values are inserted verbatim (section signs removed) so
     * operator supplied text such as a ban reason can never inject formatting or further placeholders.
     */
    public static String apply(String text, Map<String, String> values) {
        if (text == null || values == null || values.isEmpty() || text.indexOf('%') < 0) {
            return text;
        }
        Matcher matcher = PATTERN.matcher(text);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            if (values.containsKey(key)) {
                matcher.appendReplacement(out, Matcher.quoteReplacement(sanitize(values.get(key))));
            } else {
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\u00a7", "").replace('\r', ' ');
    }
}
