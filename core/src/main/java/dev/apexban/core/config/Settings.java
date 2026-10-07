package dev.apexban.core.config;

import dev.apexban.core.platform.PlatformLogger;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class Settings {
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^[A-Za-z0-9_]{0,32}$");
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public final String serverId;
    public final boolean sqlite;
    public final String sqliteFile;
    public final String mysqlHost;
    public final int mysqlPort;
    public final String mysqlDatabase;
    public final String mysqlUsername;
    public final String mysqlPassword;
    public final boolean mysqlSsl;
    public final int mysqlPoolSize;
    public final String tablePrefix;
    public final String defaultBanReason;
    public final String defaultMuteReason;
    public final String defaultKickReason;
    public final boolean announcePublic;
    public final boolean allowUnknownPlayers;
    public final boolean syncEnabled;
    public final int syncIntervalSeconds;
    public final boolean failOpen;
    public final boolean hexColors;
    public final Set<String> blockedCommands;
    private final Set<String> announceKinds;
    private final DateTimeFormatter dateFormatter;

    private Settings(YamlFile file, PlatformLogger logger) {
        this.serverId = trimToLength(file.getString("server-id", "main"), 64);
        this.sqlite = !file.getString("database.type", "SQLITE").trim().equalsIgnoreCase("MYSQL");
        this.sqliteFile = file.getString("database.sqlite.file", "apexban.db").trim();
        this.mysqlHost = file.getString("database.mysql.host", "127.0.0.1").trim();
        this.mysqlPort = file.getInt("database.mysql.port", 3306);
        this.mysqlDatabase = file.getString("database.mysql.database", "apexban").trim();
        this.mysqlUsername = file.getString("database.mysql.username", "root");
        this.mysqlPassword = file.getString("database.mysql.password", "");
        this.mysqlSsl = file.getBoolean("database.mysql.use-ssl", false);
        this.mysqlPoolSize = Math.max(2, Math.min(32, file.getInt("database.mysql.pool-size", 8)));

        String prefix = file.getString("database.table-prefix", "apexban_").trim();
        if (!PREFIX_PATTERN.matcher(prefix).matches()) {
            logger.warn("Invalid database.table-prefix '" + prefix + "', falling back to 'apexban_'.");
            prefix = "apexban_";
        }
        this.tablePrefix = prefix;

        this.defaultBanReason = file.getString("defaults.ban-reason", "You have been banned.");
        this.defaultMuteReason = file.getString("defaults.mute-reason", "You have been muted.");
        this.defaultKickReason = file.getString("defaults.kick-reason", "You have been kicked.");

        Set<String> kinds = new HashSet<String>();
        for (String kind : new String[]{"ban", "mute", "kick", "unban", "unmute"}) {
            if (file.getBoolean("announcements." + kind, true)) {
                kinds.add(kind);
            }
        }
        this.announceKinds = Collections.unmodifiableSet(kinds);
        this.announcePublic = file.getBoolean("announcements.public", false);

        this.allowUnknownPlayers = file.getBoolean("players.allow-unknown", false);
        this.syncEnabled = file.getBoolean("sync.enabled", true);
        this.syncIntervalSeconds = Math.max(5, file.getInt("sync.interval-seconds", 15));
        this.failOpen = file.getBoolean("login.fail-open", true);
        this.hexColors = file.getBoolean("formatting.hex-colors", false);

        Set<String> blocked = new HashSet<String>();
        for (String command : file.getStringList("mute.blocked-commands")) {
            String cleaned = command.trim().toLowerCase(Locale.ROOT);
            if (cleaned.startsWith("/")) {
                cleaned = cleaned.substring(1);
            }
            if (!cleaned.isEmpty()) {
                blocked.add(cleaned);
            }
        }
        this.blockedCommands = Collections.unmodifiableSet(blocked);

        this.dateFormatter = buildFormatter(file, logger);
    }

    public static Settings from(YamlFile file, PlatformLogger logger) {
        return new Settings(file, logger);
    }

    public boolean announces(String kind) {
        return announceKinds.contains(kind);
    }

    public String formatDate(long epochMillis) {
        return dateFormatter.format(Instant.ofEpochMilli(epochMillis));
    }

    private static DateTimeFormatter buildFormatter(YamlFile file, PlatformLogger logger) {
        String zoneName = file.getString("timezone", "UTC").trim();
        ZoneId zone;
        if (zoneName.equalsIgnoreCase("SYSTEM")) {
            zone = ZoneId.systemDefault();
        } else {
            try {
                zone = ZoneId.of(zoneName);
            } catch (RuntimeException ex) {
                logger.warn("Unknown timezone '" + zoneName + "', using UTC.");
                zone = ZoneId.of("UTC");
            }
        }
        String pattern = file.getString("date-format", DEFAULT_DATE_FORMAT);
        try {
            return DateTimeFormatter.ofPattern(pattern).withZone(zone);
        } catch (IllegalArgumentException ex) {
            logger.warn("Invalid date-format '" + pattern + "', using " + DEFAULT_DATE_FORMAT + ".");
            return DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT).withZone(zone);
        }
    }

    private static String trimToLength(String value, int max) {
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }
}
