package dev.apexban.core.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.apexban.core.config.Settings;
import dev.apexban.core.model.Punishment;
import dev.apexban.core.model.PunishmentType;
import dev.apexban.core.model.Target;
import dev.apexban.core.platform.PlatformLogger;

import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

/**
 * JDBC storage shared by every platform. All methods block and must be called off the main thread.
 */
public final class SqlStorage {
    private static final String COLUMNS =
            "id, ptype, target_uuid, target_name, operator_name, reason, created_at, expires_at, server_id";
    private static final int IN_CHUNK = 200;

    private final Settings settings;
    private final File dataFolder;
    private final PlatformLogger logger;
    private final String punishTable;
    private final String playerTable;
    private volatile ConnectionProvider provider;

    public SqlStorage(Settings settings, File dataFolder, PlatformLogger logger) {
        this.settings = settings;
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.punishTable = settings.tablePrefix + "punishments";
        this.playerTable = settings.tablePrefix + "players";
    }

    public void init() throws SQLException {
        provider = createProvider();
        try (Connection connection = provider.get(); Statement statement = connection.createStatement()) {
            for (String sql : schema()) {
                statement.executeUpdate(sql);
            }
        }
        logger.info("Database ready (" + (settings.sqlite ? "SQLite" : "MySQL") + ").");
    }

    public void close() {
        ConnectionProvider current = provider;
        provider = null;
        if (current != null) {
            current.close();
        }
    }

    private ConnectionProvider createProvider() throws SQLException {
        if (settings.sqlite) {
            return createSqliteProvider();
        }
        return createMysqlProvider();
    }

    private ConnectionProvider createSqliteProvider() throws SQLException {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new SQLException("Could not create data folder " + dataFolder.getAbsolutePath());
        }
        File database = new File(dataFolder, settings.sqliteFile);
        final String url = "jdbc:sqlite:" + database.getAbsolutePath();
        // NOTE: do NOT pass pragmas (journal_mode, synchronous...) as connection properties.
        // Legacy servers (Spigot/Paper 1.8.x) ship an ancient sqlite-jdbc that wins over the shaded one
        // and runs property pragmas through executeBatch(), which fails with
        // "batch entry 0: query returns results". Plain execute() after connecting works on every driver.
        final Properties properties = new Properties();
        final Driver driver = new org.sqlite.JDBC();
        return new ConnectionProvider() {
            @Override
            public Connection get() throws SQLException {
                Connection connection = driver.connect(url, properties);
                if (connection == null) {
                    throw new SQLException("SQLite driver rejected the connection URL");
                }
                applyPragma(connection, "PRAGMA busy_timeout=10000");
                applyPragma(connection, "PRAGMA journal_mode=WAL");
                applyPragma(connection, "PRAGMA synchronous=NORMAL");
                return connection;
            }

            @Override
            public void close() {
            }
        };
    }

    private void applyPragma(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ex) {
            logger.warn("SQLite pragma failed (" + sql + "): " + ex.getMessage());
        }
    }

    private ConnectionProvider createMysqlProvider() throws SQLException {
        String url = "jdbc:mysql://" + settings.mysqlHost + ":" + settings.mysqlPort + "/" + settings.mysqlDatabase;
        Properties properties = new Properties();
        properties.setProperty("user", settings.mysqlUsername);
        properties.setProperty("password", settings.mysqlPassword);
        properties.setProperty("sslMode", settings.mysqlSsl ? "REQUIRED" : "DISABLED");
        properties.setProperty("allowPublicKeyRetrieval", "true");
        properties.setProperty("characterEncoding", "UTF-8");
        properties.setProperty("createDatabaseIfNotExist", "true");
        properties.setProperty("tcpKeepAlive", "true");

        HikariConfig config = new HikariConfig();
        config.setPoolName("ApexBan-Pool");
        config.setDataSource(new DriverDataSource(new com.mysql.cj.jdbc.Driver(), url, properties));
        config.setMaximumPoolSize(settings.mysqlPoolSize);
        config.setMinimumIdle(Math.min(2, settings.mysqlPoolSize));
        config.setConnectionTimeout(10000L);
        config.setValidationTimeout(5000L);
        config.setMaxLifetime(1500000L);

        final HikariDataSource dataSource;
        try {
            dataSource = new HikariDataSource(config);
        } catch (RuntimeException ex) {
            throw new SQLException("Could not connect to MySQL: " + ex.getMessage(), ex);
        }
        return new ConnectionProvider() {
            @Override
            public Connection get() throws SQLException {
                return dataSource.getConnection();
            }

            @Override
            public void close() {
                dataSource.close();
            }
        };
    }

    private List<String> schema() {
        List<String> statements = new ArrayList<String>();
        if (settings.sqlite) {
            statements.add("CREATE TABLE IF NOT EXISTS " + punishTable + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "ptype VARCHAR(8) NOT NULL, "
                    + "target_uuid VARCHAR(36) NOT NULL, "
                    + "target_name VARCHAR(32) NOT NULL, "
                    + "operator_name VARCHAR(64) NOT NULL, "
                    + "reason VARCHAR(512) NOT NULL, "
                    + "created_at BIGINT NOT NULL, "
                    + "expires_at BIGINT NOT NULL, "
                    + "server_id VARCHAR(64) NOT NULL, "
                    + "active INTEGER NOT NULL DEFAULT 1, "
                    + "removed_by VARCHAR(64), "
                    + "removed_at BIGINT)");
            statements.add("CREATE INDEX IF NOT EXISTS " + punishTable + "_lookup ON " + punishTable
                    + " (target_uuid, ptype, active)");
            statements.add("CREATE TABLE IF NOT EXISTS " + playerTable + " ("
                    + "player_uuid VARCHAR(36) NOT NULL PRIMARY KEY, "
                    + "player_name VARCHAR(32) NOT NULL, "
                    + "name_lower VARCHAR(32) NOT NULL, "
                    + "last_seen BIGINT NOT NULL)");
            statements.add("CREATE INDEX IF NOT EXISTS " + playerTable + "_name ON " + playerTable
                    + " (name_lower)");
        } else {
            statements.add("CREATE TABLE IF NOT EXISTS " + punishTable + " ("
                    + "id INT NOT NULL AUTO_INCREMENT, "
                    + "ptype VARCHAR(8) NOT NULL, "
                    + "target_uuid VARCHAR(36) NOT NULL, "
                    + "target_name VARCHAR(32) NOT NULL, "
                    + "operator_name VARCHAR(64) NOT NULL, "
                    + "reason VARCHAR(512) NOT NULL, "
                    + "created_at BIGINT NOT NULL, "
                    + "expires_at BIGINT NOT NULL, "
                    + "server_id VARCHAR(64) NOT NULL, "
                    + "active TINYINT(1) NOT NULL DEFAULT 1, "
                    + "removed_by VARCHAR(64) NULL, "
                    + "removed_at BIGINT NULL, "
                    + "PRIMARY KEY (id), "
                    + "INDEX " + punishTable + "_lookup (target_uuid, ptype, active)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            statements.add("CREATE TABLE IF NOT EXISTS " + playerTable + " ("
                    + "player_uuid VARCHAR(36) NOT NULL, "
                    + "player_name VARCHAR(32) NOT NULL, "
                    + "name_lower VARCHAR(32) NOT NULL, "
                    + "last_seen BIGINT NOT NULL, "
                    + "PRIMARY KEY (player_uuid), "
                    + "INDEX " + playerTable + "_name (name_lower)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
        return statements;
    }

    public Punishment insert(Punishment punishment) throws SQLException {
        String sql = "INSERT INTO " + punishTable
                + " (ptype, target_uuid, target_name, operator_name, reason, created_at, expires_at, server_id, active)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = provider.get();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, punishment.getType().name());
            statement.setString(2, punishment.getUuid().toString());
            statement.setString(3, punishment.getTargetName());
            statement.setString(4, punishment.getOperator());
            statement.setString(5, punishment.getReason());
            statement.setLong(6, punishment.getCreatedAt());
            statement.setLong(7, punishment.getExpiresAt());
            statement.setString(8, punishment.getServerId());
            statement.setInt(9, punishment.getType() == PunishmentType.KICK ? 0 : 1);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys != null && keys.next()) {
                    return punishment.withId(keys.getLong(1));
                }
            } catch (SQLException ignored) {
                // legacy SQLite drivers: fall through to last_insert_rowid()
            }
            if (settings.sqlite) {
                try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        return punishment.withId(rs.getLong(1));
                    }
                }
            }
        }
        return punishment;
    }

    public int deactivate(UUID uuid, PunishmentType type, String removedBy, long now) throws SQLException {
        String sql = "UPDATE " + punishTable + " SET active=0, removed_by=?, removed_at=?"
                + " WHERE target_uuid=? AND ptype=? AND active=1";
        try (Connection connection = provider.get(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, removedBy);
            statement.setLong(2, now);
            statement.setString(3, uuid.toString());
            statement.setString(4, type.name());
            return statement.executeUpdate();
        }
    }

    public Punishment findActive(UUID uuid, PunishmentType type, long now) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM " + punishTable
                + " WHERE target_uuid=? AND ptype=? AND active=1 AND (expires_at=-1 OR expires_at>?)"
                + " ORDER BY id DESC LIMIT 1";
        try (Connection connection = provider.get(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, type.name());
            statement.setLong(3, now);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Punishment> findActiveFor(Collection<UUID> uuids, long now) throws SQLException {
        List<Punishment> result = new ArrayList<Punishment>();
        List<UUID> all = new ArrayList<UUID>(uuids);
        for (int from = 0; from < all.size(); from += IN_CHUNK) {
            List<UUID> part = all.subList(from, Math.min(all.size(), from + IN_CHUNK));
            StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS).append(" FROM ").append(punishTable)
                    .append(" WHERE active=1 AND ptype IN ('BAN','MUTE') AND (expires_at=-1 OR expires_at>?)")
                    .append(" AND target_uuid IN (");
            for (int i = 0; i < part.size(); i++) {
                sql.append(i == 0 ? "?" : ",?");
            }
            sql.append(") ORDER BY id ASC");
            try (Connection connection = provider.get();
                 PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                statement.setLong(1, now);
                for (int i = 0; i < part.size(); i++) {
                    statement.setString(i + 2, part.get(i).toString());
                }
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        result.add(map(rs));
                    }
                }
            }
        }
        return result;
    }

    public void upsertPlayer(UUID uuid, String name, long now) throws SQLException {
        try (Connection connection = provider.get()) {
            if (updatePlayer(connection, uuid, name, now) > 0) {
                return;
            }
            String sql = "INSERT INTO " + playerTable
                    + " (player_uuid, player_name, name_lower, last_seen) VALUES (?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, name);
                statement.setString(3, name.toLowerCase(Locale.ROOT));
                statement.setLong(4, now);
                statement.executeUpdate();
            } catch (SQLException duplicate) {
                if (updatePlayer(connection, uuid, name, now) == 0) {
                    throw duplicate;
                }
            }
        }
    }

    private int updatePlayer(Connection connection, UUID uuid, String name, long now) throws SQLException {
        String sql = "UPDATE " + playerTable + " SET player_name=?, name_lower=?, last_seen=? WHERE player_uuid=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, name.toLowerCase(Locale.ROOT));
            statement.setLong(3, now);
            statement.setString(4, uuid.toString());
            return statement.executeUpdate();
        }
    }

    public Target findPlayerByName(String name) throws SQLException {
        String sql = "SELECT player_uuid, player_name FROM " + playerTable
                + " WHERE name_lower=? ORDER BY last_seen DESC LIMIT 1";
        try (Connection connection = provider.get(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name.toLowerCase(Locale.ROOT));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new Target(UUID.fromString(rs.getString(1)), rs.getString(2));
                }
                return null;
            }
        }
    }

    private Punishment map(ResultSet rs) throws SQLException {
        return new Punishment(
                rs.getLong("id"),
                PunishmentType.valueOf(rs.getString("ptype")),
                UUID.fromString(rs.getString("target_uuid")),
                rs.getString("target_name"),
                rs.getString("operator_name"),
                rs.getString("reason"),
                rs.getLong("created_at"),
                rs.getLong("expires_at"),
                rs.getString("server_id"));
    }
}
