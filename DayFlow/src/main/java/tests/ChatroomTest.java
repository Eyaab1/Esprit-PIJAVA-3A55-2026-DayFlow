package tests;

import model.chatroom.Chatroom;
import model.goals_activity_management.Goal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class ChatroomTest {

    private Chatroom testChatroom;

    @BeforeEach
    public void setUp() {
        testChatroom = new Chatroom();
        testChatroom.setId(1);
        testChatroom.setGoalId(10);
        testChatroom.setState("active");
        testChatroom.setCreatedAt(LocalDateTime.now());
    }

    @Test
    public void testChatroomCreation() {
        assertNotNull(testChatroom);
        assertEquals(1, testChatroom.getId());
        assertEquals(10, testChatroom.getGoalId());
        assertEquals("active", testChatroom.getState());
        assertNotNull(testChatroom.getCreatedAt());
    }

    @Test
    public void testChatroomConstructorWithParams() {
        Chatroom chatroom = new Chatroom(5, "inactive");
        
        assertEquals(5, chatroom.getGoalId());
        assertEquals("inactive", chatroom.getState());
        assertNotNull(chatroom.getCreatedAt());
    }

    @Test
    public void testStateValidation() {
        assertThrows(IllegalArgumentException.class, () -> testChatroom.setState(null));
        assertThrows(IllegalArgumentException.class, () -> testChatroom.setState(""));
        assertThrows(IllegalArgumentException.class, () -> testChatroom.setState("   "));
        assertThrows(IllegalArgumentException.class, () -> testChatroom.setState("invalid"));
    }

    @Test
    public void testValidStates() {
        assertDoesNotThrow(() -> testChatroom.setState("active"));
        assertEquals("active", testChatroom.getState());

        assertDoesNotThrow(() -> testChatroom.setState("inactive"));
        assertEquals("inactive", testChatroom.getState());
    }

    @Test
    public void testGoalIdValidation() {
        assertThrows(IllegalArgumentException.class, () -> testChatroom.setGoalId(0));
        assertThrows(IllegalArgumentException.class, () -> testChatroom.setGoalId(-1));
    }

    @Test
    public void testGoalAssociation() {
        Goal goal = new Goal();
        goal.setId(10);

        testChatroom.setGoal(goal);

        assertEquals(goal, testChatroom.getGoal());
        assertEquals(10, testChatroom.getGoalId());
    }

    @Test
    public void testGoalIdUpdatesWhenGoalSet() {
        Goal goal = new Goal();
        goal.setId(20);

        testChatroom.setGoal(goal);

        assertEquals(20, testChatroom.getGoalId());
    }
}
