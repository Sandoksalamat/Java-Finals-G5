package com.eas.config;

import java.sql.*;

public final class Database {

    private Database() {}

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException(
                "MySQL Connector/J driver was not found. " +
                "Put mysql-connector-j-8.4.0.jar in the lib folder.",
                ex
            );
        }

        return DriverManager.getConnection(
            AppConfig.get("db.url"),
            AppConfig.get("db.user"),
            AppConfig.get("db.password")
        );
    }

    public static boolean testConnection() throws SQLException {
        try (Connection c = getConnection()) {
            return c.isValid(4);
        }
    }
}