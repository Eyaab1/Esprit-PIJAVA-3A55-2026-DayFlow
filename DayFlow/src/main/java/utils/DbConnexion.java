package utils;

import config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DbConnexion {

    private static final String URL = AppConfig.dbUrl();
    private static final String USER = AppConfig.dbUser();
    private static final String PASSWORD = AppConfig.dbPassword();

    private static volatile DbConnexion instance;

    private final Connection cnx;

    private DbConnexion() {
        try {
            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new IllegalStateException("Échec connexion base de données", e);
        }
    }

    public static DbConnexion getInstance() {
        if (instance == null) {
            synchronized (DbConnexion.class) {
                if (instance == null) {
                    instance = new DbConnexion();
                }
            }
        }
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }

    /** Connexion unique partagée ; ne pas la fermer (fermer seulement les statements / result sets). */
    public static Connection getConnection() {
        return getInstance().getCnx();
    }

    public static void shutdown() {
        synchronized (DbConnexion.class) {
            if (instance != null) {
                try {
                    if (!instance.cnx.isClosed()) {
                        instance.cnx.close();
                    }
                } catch (SQLException ignored) {
                }
                instance = null;
            }
        }
    }
}
