package tests;

import model.goals_activity_management.Goal;
import model.goals_activity_management.GoalParticipation;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GoalParticipationTest {

    private GoalParticipation testParticipation;

    @BeforeEach
    public void setUp() {
        testParticipation = new GoalParticipation(1, 10);
    }

    @Test
    public void testDefaultValues() {
        assertEquals(GoalParticipation.ROLE_MEMBER, testParticipation.getRole());
        assertEquals(GoalParticipation.STATUS_APPROVED, testParticipation.getStatus());
        assertNotNull(testParticipation.getCreatedAt());
    }

    @Test
    public void testUserIdValidation() {
        assertThrows(IllegalArgumentException.class, () -> testParticipation.setUserId(0));
        assertThrows(IllegalArgumentException.class, () -> testParticipation.setUserId(-1));
    }

    @Test
    public void testGoalIdValidation() {
        assertThrows(IllegalArgumentException.class, () -> testParticipation.setGoalId(0));
        assertThrows(IllegalArgumentException.class, () -> testParticipation.setGoalId(-1));
    }

    @Test
    public void testAllRoles() {
        assertDoesNotThrow(() -> testParticipation.setRole(GoalParticipation.ROLE_MEMBER));
        assertDoesNotThrow(() -> testParticipation.setRole(GoalParticipation.ROLE_ADMIN));
        assertDoesNotThrow(() -> testParticipation.setRole(GoalParticipation.ROLE_OWNER));
    }

    @Test
    public void testInvalidRole() {
        assertThrows(IllegalArgumentException.class, () -> testParticipation.setRole("superuser"));
    }

    @Test
    public void testAllStatuses() {
        assertDoesNotThrow(() -> testParticipation.setStatus(GoalParticipation.STATUS_PENDING));
        assertDoesNotThrow(() -> testParticipation.setStatus(GoalParticipation.STATUS_APPROVED));
        assertDoesNotThrow(() -> testParticipation.setStatus(GoalParticipation.STATUS_REJECTED));
    }

    @Test
    public void testInvalidStatus() {
        assertThrows(IllegalArgumentException.class, () -> testParticipation.setStatus("banned"));
    }

    @Test
    public void testIsOwner() {
        testParticipation.setRole(GoalParticipation.ROLE_OWNER);
        assertTrue(testParticipation.isOwner());
        assertTrue(testParticipation.isAdmin()); // owner is also admin
    }

    @Test
    public void testIsAdmin() {
        testParticipation.setRole(GoalParticipation.ROLE_ADMIN);
        assertTrue(testParticipation.isAdmin());
        assertFalse(testParticipation.isOwner());
    }

    @Test
    public void testIsPending() {
        testParticipation.setStatus(GoalParticipation.STATUS_PENDING);
        assertTrue(testParticipation.isPending());

        testParticipation.setStatus(GoalParticipation.STATUS_APPROVED);
        assertFalse(testParticipation.isPending());
    }

    @Test
    public void testUserAssociationSyncsId() {
        User user = new User();
        user.setId(42);

        testParticipation.setUser(user);

        assertEquals(42, testParticipation.getUserId());
        assertTrue(user.getGoalParticipations().contains(testParticipation));
    }

    @Test
    public void testGoalAssociationSyncsId() {
        Goal goal = new Goal();
        goal.setId(99);

        testParticipation.setGoal(goal);

        assertEquals(99, testParticipation.getGoalId());
        assertTrue(goal.getGoalParticipations().contains(testParticipation));
    }
}
