package dev.apexban.core.config;

import dev.apexban.core.text.Placeholders;
import dev.apexban.core.text.TextFormatter;

import java.util.List;
import java.util.Map;

public final class Layouts {
    private final YamlFile file;
    private final TextFormatter formatter;

    public Layouts(YamlFile file, TextFormatter formatter) {
        this.file = file;
        this.formatter = formatter;
    }

    /**
     * Renders a multi-line screen template. Lines are joined with newlines, formatted as one block so
     * colours carry across lines, and placeholders are applied afterwards.
     */
    public String render(String path, Map<String, String> placeholders) {
        List<String> lines = file.getStringList(path);
        if (lines.isEmpty()) {
            lines = java.util.Collections.singletonList("&cMissing layout: " + path);
        }
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                joined.append('\n');
            }
            joined.append(lines.get(i));
        }
        return Placeholders.apply(formatter.format(joined.toString()), placeholders);
    }
}
