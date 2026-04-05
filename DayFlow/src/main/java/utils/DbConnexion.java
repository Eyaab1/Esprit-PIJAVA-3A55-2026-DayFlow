package utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Pool de connexions JDBC : la configuration et le pool HikariCP sont créés une seule fois.
 * Chaque {@link #getConnection()} emprunte une connexion ; la fermer (ex. try-with-resources) la rend au pool.
 */
public final class DbConnexion {

    private static final String URL = "jdbc:postgresql://localhost:5432/pidev_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "admin";

    private static volatile HikariDataSource dataSource;

    private DbConnexion() {
    }

    private static HikariDataSource getOrCreateDataSource() {
        if (dataSource == null) {
            synchronized (DbConnexion.class) {
                if (dataSource == null) {
                    HikariConfig config = new HikariConfig();
                    config.setJdbcUrl(URL);
                    config.setUsername(USER);
                    config.setPassword(PASSWORD);
                    config.setPoolName("DayFlow-pool");
                    config.setMaximumPoolSize(10);
                    config.setMinimumIdle(1);
                    dataSource = new HikariDataSource(config);
                }
            }
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return getOrCreateDataSource().getConnection();
    }

    /** Ferme le pool (à appeler à l’arrêt de l’application). */
    public static void shutdown() {
        synchronized (DbConnexion.class) {
            if (dataSource != null) {
                dataSource.close();
                dataSource = null;
            }
        }
    }
}
