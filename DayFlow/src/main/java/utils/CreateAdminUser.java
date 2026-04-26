package utils;

import enums.UserRole;
import services.account.UserService;
import model.user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Utility to create an admin user in the database.
 * 
 * Usage:
 * 1. Run this class directly (it has a main method)
 * 2. Or call createAdmin() from your code
 * 
 * Default credentials:
 * Email: admin@dayflow.com
 * Password: Admin123!
 */
public class CreateAdminUser {

    public static void main(String[] args) {
        System.out.println("=== DayFlow Admin User Creator ===\n");
        
        try {
            // Option 1: Create with default credentials
            User admin = createAdmin(
                "admin@dayflow.com",
                "Admin123!",
                "Admin",
                "DayFlow"
            );
            
            System.out.println("✅ Admin user created successfully!");
            System.out.println("\nCredentials:");
            System.out.println("  Email: " + admin.getEmail());
            System.out.println("  Password: Admin123!");
            System.out.println("  ID: " + admin.getId());
            System.out.println("\n⚠️  IMPORTANT: Change the password after first login!");
            
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error: " + e.getMessage());
            if (e.getMessage().contains("déjà utilisé")) {
                System.out.println("\n✅ Admin user already exists. You can login with:");
                System.out.println("  Email: admin@dayflow.com");
                System.out.println("  Password: Admin123! (if not changed)");
            }
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DbConnexion.shutdown();
        }
    }

    /**
     * Creates an admin user with the specified credentials.
     * 
     * @param email Admin email
     * @param password Admin password (will be hashed)
     * @param firstName First name
     * @param lastName Last name
     * @return The created User object
     * @throws SQLException If database error occurs
     */
    public static User createAdmin(String email, String password, String firstName, String lastName) 
            throws SQLException {
        
        // Validate inputs
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name cannot be empty");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name cannot be empty");
        }

        // Check if user already exists
        UserService userService = new UserService();
        if (userService.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà.");
        }

        // Create user with admin role directly in database
        String hashedPassword = PasswordHasher.hash(password);
        
        String sql = """
            INSERT INTO "user" (
                first_name, last_name, email, password, roles,
                phone_number, age, status, review_count,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id
            """;
        
        Connection conn = DbConnexion.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName.trim());
            ps.setString(2, lastName.trim());
            ps.setString(3, email.trim().toLowerCase());
            ps.setString(4, hashedPassword);
            ps.setString(5, "[\"ROLE_ADMIN\"]");
            ps.setString(6, "+33612345678"); // Default phone
            ps.setInt(7, 30); // Default age
            ps.setString(8, "active");
            ps.setInt(9, 0);
            
            var rs = ps.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt(1);
                User admin = new User();
                admin.setId(userId);
                admin.setFirstName(firstName.trim());
                admin.setLastName(lastName.trim());
                admin.setEmail(email.trim().toLowerCase());
                admin.setRoles(List.of(UserRole.ADMIN.getValue()));
                admin.setStatus("active");
                return admin;
            }
            throw new SQLException("Failed to create admin user");
        }
    }

    /**
     * Creates an admin user with custom details.
     */
    public static User createCustomAdmin(
            String email,
            String password,
            String firstName,
            String lastName,
            String phoneNumber,
            Integer age
    ) throws SQLException {
        
        String hashedPassword = PasswordHasher.hash(password);
        
        String sql = """
            INSERT INTO "user" (
                first_name, last_name, email, password, roles,
                phone_number, age, status, review_count,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id
            """;
        
        Connection conn = DbConnexion.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName.trim());
            ps.setString(2, lastName.trim());
            ps.setString(3, email.trim().toLowerCase());
            ps.setString(4, hashedPassword);
            ps.setString(5, "[\"ROLE_ADMIN\"]");
            ps.setString(6, phoneNumber != null ? phoneNumber : "+33612345678");
            ps.setInt(7, age != null ? age : 30);
            ps.setString(8, "active");
            ps.setInt(9, 0);
            
            var rs = ps.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt(1);
                User admin = new User();
                admin.setId(userId);
                admin.setFirstName(firstName.trim());
                admin.setLastName(lastName.trim());
                admin.setEmail(email.trim().toLowerCase());
                admin.setRoles(List.of(UserRole.ADMIN.getValue()));
                admin.setPhoneNumber(phoneNumber);
                admin.setAge(age);
                admin.setStatus("active");
                return admin;
            }
            throw new SQLException("Failed to create admin user");
        }
    }
}
