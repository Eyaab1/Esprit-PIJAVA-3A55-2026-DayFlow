import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CheckDatabase {
    
    private static final String URL = "jdbc:postgresql://localhost:5432/pidev_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "admin";

    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
            
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 Statement stmt = conn.createStatement()) {
                
                System.out.println("✓ Connected to database");
                
                // Check if table exists
                String query = "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'disponibilite')";
                ResultSet rs = stmt.executeQuery(query);
                if (rs.next()) {
                    boolean exists = rs.getBoolean(1);
                    System.out.println("Table 'disponibilite' exists: " + exists);
                }
                
                // List all tables
                System.out.println("\nAll tables in database:");
                query = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";
                rs = stmt.executeQuery(query);
                while (rs.next()) {
                    System.out.println("  - " + rs.getString(1));
                }
                
            }
        } catch (ClassNotFoundException e) {
            System.err.println("✗ PostgreSQL driver not found");
        } catch (SQLException e) {
            System.err.println("✗ Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
