package tests;

import model.goals_activity_management.Activity;
import model.goals_activity_management.Routine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

public class ActivityTest {

    private Activity testActivity;

    @BeforeEach
    public void setUp() {
        testActivity = new Activity();
        testActivity.setId(1);
        testActivity.setTitle("Morning Run");
        testActivity.setStartTime(LocalDateTime.now());
        testActivity.setDuration(LocalTime.of(0, 30));
        testActivity.setStatus("pending");
    }

    @Test
    public void testDefaultValues() {
        Activity fresh = new Activity();
        assertEquals("pending", fresh.getStatus());
        assertFalse(fresh.isHasReminder());
        assertNotNull(fresh.getCreatedAt());
    }

    @Test
    public void testTitleValidation() {
        assertThrows(IllegalArgumentException.class, () -> testActivity.setTitle(null));
        assertThrows(IllegalArgumentException.class, () -> testActivity.setTitle(""));
        assertThrows(IllegalArgumentException.class, () -> testActivity.setTitle("  "));
        assertThrows(IllegalArgumentException.class, () -> testActivity.setTitle("ab")); // < 3 chars
        assertThrows(IllegalArgumentException.class, () -> testActivity.setTitle("a".repeat(256)));
    }

    @Test
    public void testValidTitle() {
        assertDoesNotThrow(() -> testActivity.setTitle("Run"));
        assertDoesNotThrow(() -> testActivity.setTitle("Morning Yoga Session"));
    }

    @Test
    public void testStartTimeValidation() {
        assertThrows(IllegalArgumentException.class, () -> testActivity.setStartTime(null));
    }

    @Test
    public void testDurationValidation() {
        assertThrows(IllegalArgumentException.class, () -> testActivity.setDuration(null));
    }

    @Test
    public void testAllStatuses() {
        assertDoesNotThrow(() -> testActivity.setStatus("pending"));
        assertDoesNotThrow(() -> testActivity.setStatus("in_progress"));
        assertDoesNotThrow(() -> testActivity.setStatus("completed"));
        assertDoesNotThrow(() -> testActivity.setStatus("skipped"));
        assertDoesNotThrow(() -> testActivity.setStatus("cancelled"));
    }

    @Test
    public void testInvalidStatus() {
        assertThrows(IllegalArgumentException.class, () -> testActivity.setStatus("unknown"));
        assertThrows(IllegalArgumentException.class, () -> testActivity.setStatus(null));
    }

    @Test
    public void testPriorities() {
        assertDoesNotThrow(() -> testActivity.setPriority("low"));
        assertDoesNotThrow(() -> testActivity.setPriority("medium"));
        assertDoesNotThrow(() -> testActivity.setPriority("high"));
        assertDoesNotThrow(() -> testActivity.setPriority(null));
    }

    @Test
    public void testInvalidPriority() {
        assertThrows(IllegalArgumentException.class, () -> testActivity.setPriority("critical"));
    }

    @Test
    public void testReminderRequiresDate() {
        // Setting hasReminder=true without reminderAt should throw
        assertThrows(IllegalArgumentException.class, () -> testActivity.setHasReminder(true));
    }

    @Test
    public void testReminderWithDate() {
        testActivity.setReminderAt(LocalDateTime.now().plusHours(1));
        assertDoesNotThrow(() -> testActivity.setHasReminder(true));
        assertTrue(testActivity.isHasReminder());
    }

    @Test
    public void testGetDurationInMinutes() {
        testActivity.setDuration(LocalTime.of(1, 30)); // 1h30
        assertEquals(90, testActivity.getDurationInMinutes());

        testActivity.setDuration(LocalTime.of(0, 45));
        assertEquals(45, testActivity.getDurationInMinutes());
    }

    @Test
    public void testTimeEfficiency() {
        testActivity.setPlannedDurationMinutes(60);
        testActivity.setActualDurationMinutes(60);
        assertEquals(100.0, testActivity.getTimeEfficiency(), 0.01);

        testActivity.setActualDurationMinutes(50);
        assertTrue(testActivity.getTimeEfficiency() < 100.0);
    }

    @Test
    public void testRoutineAssociation() {
        Routine routine = new Routine();
        routine.setId(1);
        routine.setTitle("Morning Routine");
        routine.setVisibility("private");
        routine.setStatus("active");

        testActivity.setRoutine(routine);

        assertEquals(routine, testActivity.getRoutine());
        assertTrue(routine.getActivities().contains(testActivity));
    }
}
