package tests;

import model.reclamation.Reclamation;
import model.reclamation.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class ResponseTest {

    private Response testResponse;

    @BeforeEach
    public void setUp() {
        testResponse = new Response();
        testResponse.setId(1);
        testResponse.setContent("This is a test response");
        testResponse.setCreatedAt(LocalDateTime.now());
    }

    @Test
    public void testResponseCreation() {
        assertNotNull(testResponse);
        assertEquals(1, testResponse.getId());
        assertEquals("This is a test response", testResponse.getContent());
        assertNotNull(testResponse.getCreatedAt());
    }

    @Test
    public void testContentValidation() {
        assertThrows(IllegalArgumentException.class, () -> testResponse.setContent(null));
        assertThrows(IllegalArgumentException.class, () -> testResponse.setContent(""));
        assertThrows(IllegalArgumentException.class, () -> testResponse.setContent("   "));
        assertThrows(IllegalArgumentException.class, () -> testResponse.setContent("Hi"));
    }

    @Test
    public void testValidContent() {
        String validContent = "Valid response content";
        assertDoesNotThrow(() -> testResponse.setContent(validContent));
        assertEquals(validContent, testResponse.getContent());
    }

    @Test
    public void testCreatedAtValidation() {
        assertThrows(IllegalArgumentException.class, () -> testResponse.setCreatedAt(null));
    }

    @Test
    public void testReclamationAssociation() {
        Reclamation reclamation = new Reclamation();
        reclamation.setId(1);

        testResponse.setReclamation(reclamation);

        assertEquals(reclamation, testResponse.getReclamation());
        assertEquals(1, testResponse.getReclamationId());
    }

    @Test
    public void testReclamationIdWhenNoReclamation() {
        testResponse.setReclamation(null);
        assertNull(testResponse.getReclamationId());
    }
}
