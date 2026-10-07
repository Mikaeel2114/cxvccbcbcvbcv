package dev.apexban.core.storage;

import java.sql.Connection;
import java.sql.SQLException;

interface ConnectionProvider {
    Connection get() throws SQLException;

    void close();
}
