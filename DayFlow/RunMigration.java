import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Standalone migration runner
 */
public class RunMigration {
    
    private static final String URL = "jdbc:postgresql://localhost:5432/pidev_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "admin";

    public static void main(String[] args) {
        System.out.println("Starting database migrations...\n");
        
        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");
            
            // Execute migrations
            executeMigration("database/migrations/create_disponibilite_table.sql");
            System.out.println();
            executeMigration("database/migrations/insert_sample_disponibilite_data.sql");
            
            System.out.println("\n✓ All migrations completed successfully!");
        } catch (ClassNotFoundException e) {
            System.err.println("✗ PostgreSQL driver not found: " + e.getMessage());
        }
    }

    private static void executeMigration(String filePath) {
        try {
            String sql = new String(Files.readAllBytes(Paths.get(filePath)));
            System.out.println("Executing: " + filePath);
            executeSql(sql);
            System.out.println("✓ Migration completed: " + filePath);
        } catch (IOException e) {
            System.err.println("✗ Error reading file: " + filePath + " - " + e.getMessage());
        }
    }

    private static void executeSql(String sql) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            
            // Remove comments and split by semicolon
            String[] lines = sql.split("\n");
            StringBuilder currentStatement = new StringBuilder();
            
            for (String line : lines) {
                // Remove comments
                String trimmed = line.trim();
                if (trimmed.startsWith("--")) {
                    continue;
                }
                
                currentStatement.append(line).append("\n");
                
                // If line ends with semicolon, execute the statement
                if (trimmed.endsWith(";")) {
                    String statement = currentStatement.toString().trim();
                    if (!statement.isEmpty()) {
                        try {
                            String display = statement.substring(0, Math.min(70, statement.length())).replace("\n", " ");
                            System.out.println("  Executing: " + display + "...");
                            stmt.execute(statement);
                            System.out.println("  ✓ Success");
                        } catch (SQLException e) {
                            System.out.println("  ✗ Error: " + e.getMessage());
                        }
                    }
                    currentStatement = new StringBuilder();
                }
            }
            
            // Execute any remaining statement
            String statement = currentStatement.toString().trim();
            if (!statement.isEmpty()) {
                try {
                    String display = statement.substring(0, Math.min(70, statement.length())).replace("\n", " ");
                    System.out.println("  Executing: " + display + "...");
                    stmt.execute(statement);
                    System.out.println("  ✓ Success");
                } catch (SQLException e) {
                    System.out.println("  ✗ Error: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("✗ Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
