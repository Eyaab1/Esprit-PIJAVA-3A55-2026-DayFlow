package tests;

import model.goals_activity_management.Goal;
import model.goals_activity_management.Routine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class GoalTest {

    private Goal testGoal;

    @BeforeEach
    public void setUp() {
        testGoal = new Goal();
        testGoal.setId(1);
        testGoal.setTitle("Learn Java");
        testGoal.setStartDate(LocalDate.now());
        testGoal.setEndDate(LocalDate.now().plusDays(30));
        testGoal.setStatus("active");
    }

    @Test
    public void testDefaultValues() {
        Goal fresh = new Goal();
        assertEquals("draft", fresh.getStatus());
        assertFalse(fresh.isFavorite());
        assertEquals(0, fresh.getProgress());
        assertNotNull(fresh.getCreatedAt());
        assertTrue(fresh.getRoutines().isEmpty());
        assertTrue(fresh.getGoalParticipations().isEmpty());
    }

    @Test
    public void testTitleValidation() {
        assertThrows(IllegalArgumentException.class, () -> testGoal.setTitle(null));
        assertThrows(IllegalArgumentException.class, () -> testGoal.setTitle(""));
        assertThrows(IllegalArgumentException.class, () -> testGoal.setTitle("  "));
        assertThrows(IllegalArgumentException.class, () -> testGoal.setTitle("ab")); // < 3 chars
        assertThrows(IllegalArgumentException.class, () -> testGoal.setTitle("a".repeat(256)));
    }

    @Test
    public void testValidTitle() {
        assertDoesNotThrow(() -> testGoal.setTitle("Run"));
        assertDoesNotThrow(() -> testGoal.setTitle("Learn Java Programming"));
    }

    @Test
    public void testAllStatuses() {
        assertDoesNotThrow(() -> testGoal.setStatus("draft"));
        assertDoesNotThrow(() -> testGoal.setStatus("active"));
        assertDoesNotThrow(() -> testGoal.setStatus("paused"));
        assertDoesNotThrow(() -> testGoal.setStatus("completed"));
        assertDoesNotThrow(() -> testGoal.setStatus("failed"));
        assertDoesNotThrow(() -> testGoal.setStatus("archived"));
    }

    @Test
    public void testInvalidStatus() {
        assertThrows(IllegalArgumentException.class, () -> testGoal.setStatus("unknown"));
        assertThrows(IllegalArgumentException.class, () -> testGoal.setStatus(null));
    }

    @Test
    public void testPriorities() {
        assertDoesNotThrow(() -> testGoal.setPriority("low"));
        assertDoesNotThrow(() -> testGoal.setPriority("medium"));
        assertDoesNotThrow(() -> testGoal.setPriority("high"));
        assertDoesNotThrow(() -> testGoal.setPriority(null));
    }

    @Test
    public void testInvalidPriority() {
        assertThrows(IllegalArgumentException.class, () -> testGoal.setPriority("critical"));
    }

    @Test
    public void testDateRange() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);
        testGoal.setStartDate(start);
        testGoal.setEndDate(end);
        assertEquals(start, testGoal.getStartDate());
        assertEquals(end, testGoal.getEndDate());
        assertTrue(testGoal.getEndDate().isAfter(testGoal.getStartDate()));
    }

    @Test
    public void testAddRoutine() {
        Routine routine = new Routine();
        routine.setId(1);
        routine.setTitle("Morning Routine");
        routine.setVisibility("private");
        routine.setStatus("active");

        testGoal.getRoutines().add(routine);
        routine.setGoal(testGoal);

        assertEquals(1, testGoal.getRoutines().size());
        assertEquals(testGoal, routine.getGoal());
    }

    @Test
    public void testFavoriteFlag() {
        testGoal.setFavorite(true);
        assertTrue(testGoal.isFavorite());

        testGoal.setFavorite(false);
        assertFalse(testGoal.isFavorite());
    }

    @Test
    public void testDescriptionValidation() {
        assertThrows(IllegalArgumentException.class, () -> testGoal.setDescription("a".repeat(1001)));
        assertDoesNotThrow(() -> testGoal.setDescription("a".repeat(1000)));
        assertDoesNotThrow(() -> testGoal.setDescription(null));
    }
}
