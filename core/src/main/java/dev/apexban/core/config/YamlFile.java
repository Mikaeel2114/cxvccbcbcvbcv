package dev.apexban.core.config;

import dev.apexban.core.platform.PlatformLogger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Read-only YAML file that is copied from the jar on first start and falls back to the bundled
 * defaults for every key missing from the server owner's copy.
 */
public final class YamlFile {
    private final Map<String, Object> data;
    private final Map<String, Object> defaults;

    private YamlFile(Map<String, Object> data, Map<String, Object> defaults) {
        this.data = data;
        this.defaults = defaults;
    }

    public static YamlFile load(File folder, String resourceName, PlatformLogger logger) {
        Map<String, Object> defaults = readResource(resourceName, logger);
        if (!folder.exists() && !folder.mkdirs()) {
            logger.warn("Could not create data folder " + folder.getAbsolutePath());
        }
        File target = new File(folder, resourceName);
        if (!target.exists()) {
            copyResource(resourceName, target, logger);
        }
        Map<String, Object> data = Collections.emptyMap();
        if (target.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(target), StandardCharsets.UTF_8)) {
                Map<String, Object> parsed = parse(reader);
                if (parsed != null) {
                    data = parsed;
                }
            } catch (Exception ex) {
                logger.error("Could not read " + resourceName + ", using built-in defaults: " + ex.getMessage(), ex);
            }
        }
        return new YamlFile(data, defaults);
    }

    private static Map<String, Object> readResource(String name, PlatformLogger logger) {
        try (InputStream in = YamlFile.class.getResourceAsStream("/" + name)) {
            if (in == null) {
                return Collections.emptyMap();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Map<String, Object> parsed = parse(reader);
                return parsed == null ? Collections.<String, Object>emptyMap() : parsed;
            }
        } catch (Exception ex) {
            logger.error("Could not read bundled " + name + ": " + ex.getMessage(), ex);
            return Collections.emptyMap();
        }
    }

    private static void copyResource(String name, File target, PlatformLogger logger) {
        try (InputStream in = YamlFile.class.getResourceAsStream("/" + name)) {
            if (in == null) {
                logger.warn("Bundled resource " + name + " is missing from the jar.");
                return;
            }
            Files.copy(in, target.toPath());
        } catch (IOException ex) {
            logger.error("Could not save default " + name + ": " + ex.getMessage(), ex);
        }
    }

    private static Map<String, Object> parse(Reader reader) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Object loaded = yaml.load(reader);
        if (loaded instanceof Map) {
            return castMap(loaded);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static Object lookup(Map<String, Object> root, String path) {
        Object current = root;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private Object find(String path) {
        Object value = lookup(data, path);
        return value != null ? value : lookup(defaults, path);
    }

    public String getString(String path, String fallback) {
        Object value = find(path);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Collection) {
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (Object element : (Collection<?>) value) {
                if (!first) {
                    builder.append('\n');
                }
                builder.append(String.valueOf(element));
                first = false;
            }
            return builder.toString();
        }
        return String.valueOf(value);
    }

    public int getInt(String path, int fallback) {
        Object value = find(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public boolean getBoolean(String path, boolean fallback) {
        Object value = find(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            String text = String.valueOf(value).trim();
            if (text.equalsIgnoreCase("true")) {
                return true;
            }
            if (text.equalsIgnoreCase("false")) {
                return false;
            }
        }
        return fallback;
    }

    public List<String> getStringList(String path) {
        Object value = find(path);
        List<String> result = new ArrayList<String>();
        if (value instanceof Collection) {
            for (Object element : (Collection<?>) value) {
                result.add(String.valueOf(element));
            }
        } else if (value != null) {
            for (String line : String.valueOf(value).split("\\r?\\n")) {
                result.add(line);
            }
        }
        return result;
    }
}
