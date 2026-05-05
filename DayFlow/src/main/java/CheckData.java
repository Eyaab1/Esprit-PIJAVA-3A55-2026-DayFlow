import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CheckData {
    
    private static final String URL = "jdbc:postgresql://localhost:5432/pidev_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "admin";

    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
            
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 Statement stmt = conn.createStatement()) {
                
                System.out.println("✓ Connected to database");
                
                // Check disponibilite table
                String query = "SELECT COUNT(*) as count FROM disponibilite";
                ResultSet rs = stmt.executeQuery(query);
                if (rs.next()) {
                    int count = rs.getInt("count");
                    System.out.println("✓ Disponibilite table has " + count + " rows");
                }
                
                // Check data for coach ID 1
                query = "SELECT COUNT(*) as count FROM disponibilite WHERE coach_id = 1";
                rs = stmt.executeQuery(query);
                if (rs.next()) {
                    int count = rs.getInt("count");
                    System.out.println("✓ Coach ID 1 has " + count + " slots");
                }
                
                // Show sample data
                query = "SELECT id, coach_id, date, heure_debut, heure_fin, statut FROM disponibilite LIMIT 5";
                rs = stmt.executeQuery(query);
                System.out.println("\nSample data:");
                while (rs.next()) {
                    System.out.println("  ID: " + rs.getInt("id") + 
                                     ", Coach: " + rs.getInt("coach_id") + 
                                     ", Date: " + rs.getDate("date") + 
                                     ", Time: " + rs.getTime("heure_debut") + " - " + rs.getTime("heure_fin") +
                                     ", Status: " + rs.getString("statut"));
                }
                
                // Check if coach 1 exists
                query = "SELECT COUNT(*) as count FROM \"user\" WHERE id = 1";
                rs = stmt.executeQuery(query);
                if (rs.next()) {
                    int count = rs.getInt("count");
                    System.out.println("\n✓ Coach ID 1 exists: " + (count > 0));
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
