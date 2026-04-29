package tests;

import model.coaching_session.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class SessionTest {

    private Session testSession;

    @BeforeEach
    public void setUp() {
        testSession = new Session();
        testSession.setId(1);
        testSession.setCoachingRequestId(1);
        testSession.setStatus(Session.STATUS_SCHEDULING);
        testSession.setCreatedAt(new Date());
    }

    @Test
    public void testDefaultValues() {
        Session fresh = new Session();
        assertEquals(Session.STATUS_SCHEDULING, fresh.getStatus());
        assertEquals(Session.PAYMENT_STATUS_PENDING, fresh.getPaymentStatus());
        assertNotNull(fresh.getCreatedAt());
        assertFalse(fresh.isPaid());
    }

    @Test
    public void testCoachingRequestIdValidation() {
        assertThrows(IllegalArgumentException.class, () -> testSession.setCoachingRequestId(0));
        assertThrows(IllegalArgumentException.class, () -> testSession.setCoachingRequestId(-1));
    }

    @Test
    public void testAllStatuses() {
        assertDoesNotThrow(() -> testSession.setStatus(Session.STATUS_SCHEDULING));
        assertDoesNotThrow(() -> testSession.setStatus(Session.STATUS_PROPOSED_BY_USER));
        assertDoesNotThrow(() -> testSession.setStatus(Session.STATUS_PROPOSED_BY_COACH));
        assertDoesNotThrow(() -> testSession.setStatus(Session.STATUS_CONFIRMED));
        assertDoesNotThrow(() -> testSession.setStatus(Session.STATUS_COMPLETED));
        assertDoesNotThrow(() -> testSession.setStatus(Session.STATUS_CANCELLED));
    }

    @Test
    public void testInvalidStatus() {
        assertThrows(IllegalArgumentException.class, () -> testSession.setStatus("unknown"));
    }

    @Test
    public void testDurationValidation() {
        assertThrows(IllegalArgumentException.class, () -> testSession.setDuration(0));
        assertThrows(IllegalArgumentException.class, () -> testSession.setDuration(-1));
        assertDoesNotThrow(() -> testSession.setDuration(60));
        assertDoesNotThrow(() -> testSession.setDuration(null));
    }

    @Test
    public void testPriceValidation() {
        assertThrows(IllegalArgumentException.class, () -> testSession.setPrice(-1.0));
        assertDoesNotThrow(() -> testSession.setPrice(0.0));
        assertDoesNotThrow(() -> testSession.setPrice(99.99));
        assertDoesNotThrow(() -> testSession.setPrice(null));
    }

    @Test
    public void testPaymentStatus() {
        testSession.setPaymentStatus(Session.PAYMENT_STATUS_PENDING);
        assertFalse(testSession.isPaid());

        testSession.setPaymentStatus(Session.PAYMENT_STATUS_PAID);
        assertTrue(testSession.isPaid());
    }

    @Test
    public void testInvalidPaymentStatus() {
        assertThrows(IllegalArgumentException.class, () -> testSession.setPaymentStatus("unknown"));
    }

    @Test
    public void testPriorities() {
        assertDoesNotThrow(() -> testSession.setPriority(Session.PRIORITY_LOW));
        assertDoesNotThrow(() -> testSession.setPriority(Session.PRIORITY_MEDIUM));
        assertDoesNotThrow(() -> testSession.setPriority(Session.PRIORITY_HIGH));
        assertDoesNotThrow(() -> testSession.setPriority(null));
    }

    @Test
    public void testInvalidPriority() {
        assertThrows(IllegalArgumentException.class, () -> testSession.setPriority("critical"));
    }

    @Test
    public void testGetDisplayTimePreference() {
        Date scheduled = new Date();
        Date proposedByCoach = new Date(scheduled.getTime() - 1000);
        Date proposedByUser = new Date(scheduled.getTime() - 2000);

        testSession.setScheduledAt(scheduled);
        testSession.setProposedTimeByCoach(proposedByCoach);
        testSession.setProposedTimeByUser(proposedByUser);

        // scheduledAt takes priority
        assertEquals(scheduled, testSession.getDisplayTime());

        testSession.setScheduledAt(null);
        assertEquals(proposedByCoach, testSession.getDisplayTime());

        testSession.setProposedTimeByCoach(null);
        assertEquals(proposedByUser, testSession.getDisplayTime());
    }
}
