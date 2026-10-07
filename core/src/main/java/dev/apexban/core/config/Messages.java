package dev.apexban.core.config;

import dev.apexban.core.text.Placeholders;
import dev.apexban.core.text.TextFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Messages {
    private final YamlFile file;
    private final TextFormatter formatter;

    public Messages(YamlFile file, TextFormatter formatter) {
        this.file = file;
        this.formatter = formatter;
    }

    public String get(String key) {
        return get(key, null);
    }

    /**
     * Returns the message formatted for display: %prefix% is expanded, colours are converted and the
     * placeholder values are inserted last so they are never interpreted as formatting.
     */
    public String get(String key, Map<String, String> placeholders) {
        String template = file.getString(key, "&cMissing message: " + key);
        template = template.replace("%prefix%", file.getString("prefix", ""));
        return Placeholders.apply(formatter.format(template), placeholders);
    }

    /**
     * Plain (unformatted) word from the words section such as "permanent" or "never".
     */
    public String word(String key) {
        return file.getString("words." + key, key);
    }

    public String formatDuration(long millis) {
        if (millis < 0L) {
            return word("permanent");
        }
        long seconds = Math.max(1L, (millis + 999L) / 1000L);
        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        seconds %= 60L;

        List<String> parts = new ArrayList<String>();
        if (days > 0L) {
            parts.add(unit("day", days));
        }
        if (hours > 0L) {
            parts.add(unit("hour", hours));
        }
        if (minutes > 0L) {
            parts.add(unit("minute", minutes));
        }
        if (seconds > 0L) {
            parts.add(unit("second", seconds));
        }
        String separator = file.getString("time.separator", ", ");
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(3, parts.size());
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                builder.append(separator);
            }
            builder.append(parts.get(i));
        }
        return builder.toString();
    }

    private String unit(String key, long amount) {
        String configured = file.getString("time." + key, key + "|" + key + "s");
        String[] forms = configured.split("\\|");
        String singular = forms.length > 0 ? forms[0] : key;
        String plural = forms.length > 1 ? forms[1] : singular;
        return amount + " " + (amount == 1L ? singular : plural);
    }
}
