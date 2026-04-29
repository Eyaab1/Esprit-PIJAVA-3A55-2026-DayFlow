package tests;

import enums.UserRole;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    private User testUser;

    @BeforeEach
    public void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setPassword("hashedPassword123");
        testUser.setRole(UserRole.USER);
        testUser.setStatus("active");
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    public void testUserCreation() {
        assertNotNull(testUser);
        assertEquals(1, testUser.getId());
        assertEquals("John", testUser.getFirstName());
        assertEquals("Doe", testUser.getLastName());
        assertEquals("john.doe@example.com", testUser.getEmail());
        assertEquals("active", testUser.getStatus());
    }

    @Test
    public void testDefaultStatus() {
        User fresh = new User();
        assertEquals("active", fresh.getStatus());
    }

    @Test
    public void testSetRole() {
        testUser.setRole(UserRole.ADMIN);
        assertTrue(testUser.hasRole(UserRole.ADMIN));

        testUser.setRole(UserRole.COACH);
        assertTrue(testUser.hasRole(UserRole.COACH));
        assertFalse(testUser.hasRole(UserRole.ADMIN));

        testUser.setRole(UserRole.USER);
        assertTrue(testUser.hasRole(UserRole.USER));
    }

    @Test
    public void testSetRoleNull() {
        testUser.setRole(null);
        assertFalse(testUser.hasRole(UserRole.USER));
        assertTrue(testUser.getRoles().isEmpty());
    }

    @Test
    public void testHasRole() {
        testUser.setRole(UserRole.USER);
        assertTrue(testUser.hasRole(UserRole.USER));
        assertFalse(testUser.hasRole(UserRole.ADMIN));
        assertFalse(testUser.hasRole(null));
    }

    @Test
    public void testBannedStatus() {
        testUser.setStatus("banned");
        testUser.setBanReason("Violation of terms");
        testUser.setBannedUntil(LocalDateTime.now().plusDays(7));

        assertEquals("banned", testUser.getStatus());
        assertEquals("Violation of terms", testUser.getBanReason());
        assertNotNull(testUser.getBannedUntil());
    }

    @Test
    public void testRolesUnique() {
        testUser.setRoles(List.of("ROLE_USER", "ROLE_USER", "ROLE_ADMIN"));
        assertEquals(2, testUser.getRoles().size());
    }

    @Test
    public void testEmailAndPassword() {
        testUser.setEmail("new@example.com");
        assertEquals("new@example.com", testUser.getEmail());

        testUser.setPassword("newHashedPassword");
        assertEquals("newHashedPassword", testUser.getPassword());
    }

    @Test
    public void testProfileFields() {
        testUser.setBio("I am a developer");
        testUser.setPhoneNumber("+33612345678");
        testUser.setAge(30);

        assertEquals("I am a developer", testUser.getBio());
        assertEquals("+33612345678", testUser.getPhoneNumber());
        assertEquals(30, testUser.getAge());
    }
}
