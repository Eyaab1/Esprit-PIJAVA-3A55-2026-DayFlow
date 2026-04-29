package tests;

import model.goals_activity_management.Activity;
import model.goals_activity_management.Routine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

public class RoutineTest {

    private Routine testRoutine;

    @BeforeEach
    public void setUp() {
        testRoutine = new Routine();
        testRoutine.setId(1);
        testRoutine.setTitle("Morning Routine");
        testRoutine.setVisibility("private");
        testRoutine.setStatus("draft");
    }

    @Test
    public void testDefaultValues() {
        Routine fresh = new Routine();
        assertEquals("draft", fresh.getStatus());
        assertEquals("private", fresh.getVisibility());
        assertFalse(fresh.isFavorite());
        assertNotNull(fresh.getCreatedAt());
        assertTrue(fresh.getActivities().isEmpty());
    }

    @Test
    public void testTitleValidation() {
        assertThrows(IllegalArgumentException.class, () -> testRoutine.setTitle(null));
        assertThrows(IllegalArgumentException.class, () -> testRoutine.setTitle(""));
        assertThrows(IllegalArgumentException.class, () -> testRoutine.setTitle("ab")); // < 3 chars
        assertThrows(IllegalArgumentException.class, () -> testRoutine.setTitle("a".repeat(256)));
    }

    @Test
    public void testDescriptionValidation() {
        assertThrows(IllegalArgumentException.class, () -> testRoutine.setDescription("a".repeat(1001)));
        assertDoesNotThrow(() -> testRoutine.setDescription("a".repeat(1000)));
        assertDoesNotThrow(() -> testRoutine.setDescription(null));
    }

    @Test
    public void testVisibilityValidation() {
        assertThrows(IllegalArgumentException.class, () -> testRoutine.setVisibility(null));
        assertThrows(IllegalArgumentException.class, () -> testRoutine.setVisibility("hidden"));
        assertDoesNotThrow(() -> testRoutine.setVisibility("public"));
        assertDoesNotThrow(() -> testRoutine.setVisibility("private"));
    }

    @Test
    public void testAllStatuses() {
        assertDoesNotThrow(() -> testRoutine.setStatus("draft"));
        assertDoesNotThrow(() -> testRoutine.setStatus("active"));
        assertDoesNotThrow(() -> testRoutine.setStatus("paused"));
        assertDoesNotThrow(() -> testRoutine.setStatus("completed"));
        assertDoesNotThrow(() -> testRoutine.setStatus("skipped"));
    }

    @Test
    public void testInvalidStatus() {
        assertThrows(IllegalArgumentException.class, () -> testRoutine.setStatus("unknown"));
        assertThrows(IllegalArgumentException.class, () -> testRoutine.setStatus(null));
    }

    @Test
    public void testActivate() {
        testRoutine.setStatus("draft");
        testRoutine.activate();
        assertEquals("active", testRoutine.getStatus());

        testRoutine.setStatus("paused");
        testRoutine.activate();
        assertEquals("active", testRoutine.getStatus());
    }

    @Test
    public void testPause() {
        testRoutine.setStatus("active");
        testRoutine.pause();
        assertEquals("paused", testRoutine.getStatus());
    }

    @Test
    public void testAddActivity() {
        Activity activity = new Activity();
        activity.setId(1);
        activity.setTitle("Push-ups");
        activity.setStartTime(LocalDateTime.now());
        activity.setDuration(LocalTime.of(0, 15));
        activity.setStatus("pending");

        testRoutine.addActivity(activity);

        assertEquals(1, testRoutine.getActivities().size());
        assertEquals(testRoutine, activity.getRoutine());
    }

    @Test
    public void testRemoveActivity() {
        Activity activity = new Activity();
        activity.setId(1);
        activity.setTitle("Push-ups");
        activity.setStartTime(LocalDateTime.now());
        activity.setDuration(LocalTime.of(0, 15));
        activity.setStatus("pending");

        testRoutine.addActivity(activity);
        assertEquals(1, testRoutine.getActivities().size());

        testRoutine.removeActivity(activity);
        assertEquals(0, testRoutine.getActivities().size());
        assertNull(activity.getRoutine());
    }

    @Test
    public void testUpdateAutoStatusWhenAllCompleted() {
        Activity a1 = new Activity();
        a1.setTitle("Act 1");
        a1.setStartTime(LocalDateTime.now());
        a1.setDuration(LocalTime.of(0, 10));
        a1.setStatus("completed");

        Activity a2 = new Activity();
        a2.setTitle("Act 2");
        a2.setStartTime(LocalDateTime.now());
        a2.setDuration(LocalTime.of(0, 10));
        a2.setStatus("completed");

        testRoutine.addActivity(a1);
        testRoutine.addActivity(a2);
        testRoutine.updateAutoStatus();

        assertEquals("completed", testRoutine.getStatus());
    }
}
