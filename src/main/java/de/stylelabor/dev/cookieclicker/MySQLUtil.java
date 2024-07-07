package de.stylelabor.dev.cookieclicker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLUtil {
    private static Connection connection;

    public static synchronized Connection getConnection(CookieClicker plugin) {
        try {
            if (connection == null || connection.isClosed()) {
                String host = plugin.getConfig().getString("mysql.host");
                int port = plugin.getConfig().getInt("mysql.port");
                String database = plugin.getConfig().getString("mysql.database");
                String username = plugin.getConfig().getString("mysql.username");
                String password = plugin.getConfig().getString("mysql.password");
                String url = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false";

                // Log before attempting to connect
                // plugin.getLogger().info("Attempting to connect to MySQL server at " + url);
                connection = DriverManager.getConnection(url, username, password);
                // Log success of connection
                // plugin.getLogger().info("Successfully connected to MySQL server.");

                // Create database if it doesn't exist
                try (Statement statement = connection.createStatement()) {
                    // Log before attempting to create the database
                    // plugin.getLogger().info("Attempting to create the database if it doesn't exist.");
                    statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + database + "`");
                    // Log success of database creation
                    // plugin.getLogger().info("Database checked/created successfully.");
                }

                // Close the connection to reconnect with the database specified
                connection.close();

                // Reconnect with the database specified
                url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false";
                // Log before attempting to reconnect
                // plugin.getLogger().info("Reconnecting with the database specified.");
                connection = DriverManager.getConnection(url, username, password);
                // Log success of reconnection
                // plugin.getLogger().info("Successfully reconnected with the specified database.");

                // Create tables if they don't exist
                try (Statement statement = connection.createStatement()) {
                    // Log before attempting to create tables
                    plugin.getLogger().info("Attempting to create tables if they don't exist.");
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_cookies (" +
                            "uuid VARCHAR(36) NOT NULL," +
                            "cookies BIGINT NOT NULL," +
                            "PRIMARY KEY (uuid))");

                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS cookies_per_click (" +
                            "uuid VARCHAR(36) NOT NULL," +
                            "cookies_per_click INT NOT NULL," +
                            "PRIMARY KEY (uuid))");
                    // Log success of table creation
                    // plugin.getLogger().info("Tables checked/created successfully.");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not connect to MySQL database: " + e.getMessage());
            connection = null; // Ensure connection is null if failed to establish
        }
        return connection;
    }
}