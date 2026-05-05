package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class to execute database migrations
 */
public class DatabaseMigration {

    /**
     * Execute SQL migration file
     */
    public static void executeMigration(String filePath) {
        try {
            String sql = new String(Files.readAllBytes(Paths.get(filePath)));
            executeSql(sql);
            System.out.println("✓ Migration executed successfully: " + filePath);
        } catch (IOException e) {
            System.err.println("✗ Error reading migration file: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("✗ Error executing migration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Execute SQL statements
     */
    public static void executeSql(String sql) throws SQLException {
        try (Connection conn = DbConnexion.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Split by semicolon and execute each statement
            String[] statements = sql.split(";");
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    try {
                        stmt.execute(trimmed);
                        System.out.println("✓ Executed: " + trimmed.substring(0, Math.min(50, trimmed.length())) + "...");
                    } catch (SQLException e) {
                        // Some statements might fail (like CREATE INDEX IF NOT EXISTS), continue
                        System.out.println("⚠ Statement skipped: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Main method to run migrations
     */
    public static void main(String[] args) {
        System.out.println("Starting database migrations...");
        
        // Execute create table migration
        executeMigration("database/migrations/create_disponibilite_table.sql");
        
        // Execute sample data migration
        executeMigration("database/migrations/insert_sample_disponibilite_data.sql");
        
        System.out.println("✓ All migrations completed!");
    }
}
