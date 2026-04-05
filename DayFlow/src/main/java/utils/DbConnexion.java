package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DbConnexion {

    private static final String URL = "jdbc:postgresql://localhost:5432/pidev_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "admin";

    private DbConnexion() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
