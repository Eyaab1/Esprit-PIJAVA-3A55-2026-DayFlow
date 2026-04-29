package tests;

import model.notification.Notification;
import model.notification.Notification.NotificationType;
import model.notification.Notification.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationTest {

    private Notification testNotification;

    @BeforeEach
    public void setUp() {
        testNotification = new Notification(
                1,
                NotificationType.DEADLINE_24H,
                EntityType.GOAL,
                10,
                "Deadline approaching",
                "Your goal deadline is in 24 hours"
        );
    }

    @Test
    public void testNotificationCreation() {
        assertNotNull(testNotification);
        assertEquals(1, testNotification.getUserId());
        assertEquals(NotificationType.DEADLINE_24H.name(), testNotification.getType());
        assertEquals("goal", testNotification.getEntityType());
        assertEquals(10, testNotification.getEntityId());
        assertEquals("Deadline approaching", testNotification.getTitle());
        assertEquals("Your goal deadline is in 24 hours", testNotification.getMessage());
        assertFalse(testNotification.isRead());
        assertNotNull(testNotification.getCreatedAt());
    }

    @Test
    public void testDefaultConstructor() {
        Notification fresh = new Notification();
        assertFalse(fresh.isRead());
        assertNotNull(fresh.getCreatedAt());
        assertNull(fresh.getReadAt());
    }

    @Test
    public void testMarkAsRead() {
        assertFalse(testNotification.isRead());
        assertNull(testNotification.getReadAt());

        testNotification.setRead(true);

        assertTrue(testNotification.isRead());
        assertNotNull(testNotification.getReadAt());
    }

    @Test
    public void testReadAtNotOverwrittenOnSecondRead() {
        testNotification.setRead(true);
        LocalDateTime firstReadAt = testNotification.getReadAt();

        testNotification.setRead(true); // set again
        assertEquals(firstReadAt, testNotification.getReadAt());
    }

    @Test
    public void testAllNotificationTypes() {
        for (NotificationType type : NotificationType.values()) {
            testNotification.setType(type);
            assertEquals(type.name(), testNotification.getType());
            assertEquals(type, testNotification.getTypeEnum());
        }
    }

    @Test
    public void testAllEntityTypes() {
        for (EntityType entityType : EntityType.values()) {
            testNotification.setEntityType(entityType);
            assertEquals(entityType.name().toLowerCase(), testNotification.getEntityType());
            assertEquals(entityType, testNotification.getEntityTypeEnum());
        }
    }

    @Test
    public void testActionUrl() {
        testNotification.setActionUrl("/goals/10");
        assertEquals("/goals/10", testNotification.getActionUrl());
    }

    @Test
    public void testConstructorWithActionUrl() {
        Notification n = new Notification(
                1, NotificationType.STATUS_CHANGED, EntityType.ROUTINE,
                5, "Status changed", "Your routine is now active", "/routines/5"
        );
        assertEquals("/routines/5", n.getActionUrl());
    }
}
