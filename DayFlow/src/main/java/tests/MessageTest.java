package tests;

import model.chatroom.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class MessageTest {

    private Message testMessage;

    @BeforeEach
    public void setUp() {
        testMessage = new Message();
        testMessage.setId(1);
        testMessage.setContent("Test message content");
        testMessage.setChatroomId(1);
        testMessage.setAuthorId(1);
        testMessage.setCreatedAt(LocalDateTime.now());
        testMessage.setPinned(false);
        testMessage.setEdited(false);
    }

    @Test
    public void testMessageCreation() {
        assertNotNull(testMessage);
        assertEquals(1, testMessage.getId());
        assertEquals("Test message content", testMessage.getContent());
        assertEquals(1, testMessage.getChatroomId());
        assertEquals(1, testMessage.getAuthorId());
        assertFalse(testMessage.isPinned());
        assertFalse(testMessage.isEdited());
    }

    @Test
    public void testMessageConstructorWithParams() {
        Message message = new Message("Hello world", 5, 10);
        
        assertEquals("Hello world", message.getContent());
        assertEquals(5, message.getChatroomId());
        assertEquals(10, message.getAuthorId());
        assertNotNull(message.getCreatedAt());
        assertFalse(message.isPinned());
        assertFalse(message.isEdited());
    }

    @Test
    public void testContentTooLong() {
        String longContent = "a".repeat(1001);
        assertThrows(IllegalArgumentException.class, () -> testMessage.setContent(longContent));
    }

    @Test
    public void testContentMaxLength() {
        String maxContent = "a".repeat(1000);
        assertDoesNotThrow(() -> testMessage.setContent(maxContent));
        assertEquals(maxContent, testMessage.getContent());
    }

    @Test
    public void testEmptyContentAllowed() {
        assertDoesNotThrow(() -> testMessage.setContent(""));
        assertEquals("", testMessage.getContent());

        assertDoesNotThrow(() -> testMessage.setContent(null));
        assertEquals("", testMessage.getContent());
    }

    @Test
    public void testChatroomIdValidation() {
        assertThrows(IllegalArgumentException.class, () -> testMessage.setChatroomId(0));
        assertThrows(IllegalArgumentException.class, () -> testMessage.setChatroomId(-1));
    }

    @Test
    public void testAuthorIdValidation() {
        assertThrows(IllegalArgumentException.class, () -> testMessage.setAuthorId(0));
        assertThrows(IllegalArgumentException.class, () -> testMessage.setAuthorId(-1));
    }

    @Test
    public void testPinnedFlag() {
        testMessage.setPinned(true);
        assertTrue(testMessage.isPinned());

        testMessage.setPinned(false);
        assertFalse(testMessage.isPinned());
    }

    @Test
    public void testEditedFlag() {
        testMessage.setEdited(true);
        assertTrue(testMessage.isEdited());

        testMessage.setEdited(false);
        assertFalse(testMessage.isEdited());
    }

    @Test
    public void testReplyToId() {
        testMessage.setReplyToId(42);
        assertEquals(42, testMessage.getReplyToId());

        testMessage.setReplyToId(0);
        assertEquals(0, testMessage.getReplyToId());
    }
}
