package dev.apexban.core.storage;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * DataSource that talks to a JDBC driver instance directly, so the driver does not have to be
 * registered with the JVM-wide DriverManager (which breaks across plugin class loaders).
 */
final class DriverDataSource implements DataSource {
    private final Driver driver;
    private final String url;
    private final Properties properties;

    DriverDataSource(Driver driver, String url, Properties properties) {
        this.driver = driver;
        this.url = url;
        this.properties = properties;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return open(properties);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Properties copy = new Properties();
        copy.putAll(properties);
        if (username != null) {
            copy.setProperty("user", username);
        }
        if (password != null) {
            copy.setProperty("password", password);
        }
        return open(copy);
    }

    private Connection open(Properties props) throws SQLException {
        Connection connection = driver.connect(url, props);
        if (connection == null) {
            throw new SQLException("JDBC driver rejected the connection URL");
        }
        return connection;
    }

    @Override
    public PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
    }

    @Override
    public void setLoginTimeout(int seconds) {
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Not a wrapper for " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }
}
