package tests;

import model.coaching_session.CoachingRequest;
import model.coaching_session.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class CoachingRequestTest {

    private CoachingRequest testRequest;

    @BeforeEach
    public void setUp() {
        testRequest = new CoachingRequest();
        testRequest.setId(1);
        testRequest.setUserId(1);
        testRequest.setCoachId(2);
        testRequest.setMessage("I need help with my productivity and focus goals");
        testRequest.setStatus(CoachingRequest.STATUS_PENDING);
        testRequest.setCreatedAt(new Date());
    }

    @Test
    public void testDefaultValues() {
        CoachingRequest fresh = new CoachingRequest();
        assertEquals(CoachingRequest.STATUS_PENDING, fresh.getStatus());
        assertEquals(CoachingRequest.PRIORITY_NORMAL, fresh.getPriority());
        assertNotNull(fresh.getCreatedAt());
    }

    @Test
    public void testMessageValidation() {
        assertThrows(IllegalArgumentException.class, () -> testRequest.setMessage(null));
        assertThrows(IllegalArgumentException.class, () -> testRequest.setMessage(""));
        assertThrows(IllegalArgumentException.class, () -> testRequest.setMessage("Too short"));
        assertThrows(IllegalArgumentException.class, () -> testRequest.setMessage("a".repeat(1001)));
    }

    @Test
    public void testValidMessage() {
        String validMessage = "I need help with my long-term productivity goals";
        assertDoesNotThrow(() -> testRequest.setMessage(validMessage));
        assertEquals(validMessage, testRequest.getMessage());
    }

    @Test
    public void testUserIdValidation() {
        assertThrows(IllegalArgumentException.class, () -> testRequest.setUserId(0));
        assertThrows(IllegalArgumentException.class, () -> testRequest.setUserId(-1));
    }

    @Test
    public void testCoachIdValidation() {
        assertThrows(IllegalArgumentException.class, () -> testRequest.setCoachId(0));
        assertThrows(IllegalArgumentException.class, () -> testRequest.setCoachId(-1));
    }

    @Test
    public void testAllStatuses() {
        assertDoesNotThrow(() -> testRequest.setStatus(CoachingRequest.STATUS_PENDING));
        assertDoesNotThrow(() -> testRequest.setStatus(CoachingRequest.STATUS_ACCEPTED));
        assertDoesNotThrow(() -> testRequest.setStatus(CoachingRequest.STATUS_PAID));
        assertDoesNotThrow(() -> testRequest.setStatus(CoachingRequest.STATUS_CONFIRMED));
        assertDoesNotThrow(() -> testRequest.setStatus(CoachingRequest.STATUS_COMPLETED));
        assertDoesNotThrow(() -> testRequest.setStatus(CoachingRequest.STATUS_CANCELLED));
        assertDoesNotThrow(() -> testRequest.setStatus(CoachingRequest.STATUS_DECLINED));
    }

    @Test
    public void testInvalidStatus() {
        assertThrows(IllegalArgumentException.class, () -> testRequest.setStatus("unknown"));
        assertThrows(IllegalArgumentException.class, () -> testRequest.setStatus(null));
    }

    @Test
    public void testPriorities() {
        assertDoesNotThrow(() -> testRequest.setPriority(CoachingRequest.PRIORITY_NORMAL));
        assertTrue(testRequest.isNormal());

        assertDoesNotThrow(() -> testRequest.setPriority(CoachingRequest.PRIORITY_MEDIUM));
        assertTrue(testRequest.isMedium());

        assertDoesNotThrow(() -> testRequest.setPriority(CoachingRequest.PRIORITY_URGENT));
        assertTrue(testRequest.isUrgent());
    }

    @Test
    public void testInvalidPriority() {
        assertThrows(IllegalArgumentException.class, () -> testRequest.setPriority("critical"));
    }

    @Test
    public void testBudgetValidation() {
        assertThrows(IllegalArgumentException.class, () -> testRequest.setBudget(-1.0));
        assertDoesNotThrow(() -> testRequest.setBudget(0.0));
        assertDoesNotThrow(() -> testRequest.setBudget(100.0));
        assertDoesNotThrow(() -> testRequest.setBudget(null));
    }

    @Test
    public void testCompatibilityScoreValidation() {
        assertThrows(IllegalArgumentException.class, () -> testRequest.setCompatibilityScore(-1));
        assertThrows(IllegalArgumentException.class, () -> testRequest.setCompatibilityScore(101));
        assertDoesNotThrow(() -> testRequest.setCompatibilityScore(0));
        assertDoesNotThrow(() -> testRequest.setCompatibilityScore(100));
        assertDoesNotThrow(() -> testRequest.setCompatibilityScore(75));
    }

    @Test
    public void testDetectPriorityUrgent() {
        testRequest.setMessage("This is urgent, I need help immediately with my crisis");
        testRequest.detectAndSetPriority();
        assertEquals(CoachingRequest.PRIORITY_URGENT, testRequest.getPriority());
    }

    @Test
    public void testDetectPriorityMedium() {
        testRequest.setMessage("This is important, I have a problème with my schedule");
        testRequest.detectAndSetPriority();
        assertEquals(CoachingRequest.PRIORITY_MEDIUM, testRequest.getPriority());
    }

    @Test
    public void testDetectPriorityNormal() {
        testRequest.setMessage("I would like to improve my daily habits over time");
        testRequest.detectAndSetPriority();
        assertEquals(CoachingRequest.PRIORITY_NORMAL, testRequest.getPriority());
    }

    @Test
    public void testSessionBidirectionalLink() {
        Session session = new Session();
        session.setId(1);
        session.setCoachingRequestId(1);

        testRequest.setSession(session);

        assertEquals(session, testRequest.getSession());
        assertEquals(testRequest, session.getCoachingRequest());
    }
}
