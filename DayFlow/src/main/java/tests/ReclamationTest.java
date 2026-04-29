package tests;

import enums.ReclamationStatus;
import enums.ReclamationType;
import model.reclamation.Reclamation;
import model.reclamation.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class ReclamationTest {

    private Reclamation testReclamation;

    @BeforeEach
    public void setUp() {
        testReclamation = new Reclamation();
        testReclamation.setId(1);
        testReclamation.setContent("This is a test reclamation with sufficient length");
        testReclamation.setType(ReclamationType.BUG);
        testReclamation.setStatus(ReclamationStatus.PENDING);
        testReclamation.setUserId(1);
        testReclamation.setCreatedAt(LocalDateTime.now());
    }

    @Test
    public void testReclamationCreation() {
        assertNotNull(testReclamation);
        assertEquals(1, testReclamation.getId());
        assertEquals(ReclamationType.BUG, testReclamation.getType());
        assertEquals(ReclamationStatus.PENDING, testReclamation.getStatus());
        assertEquals(1, testReclamation.getUserId());
    }

    @Test
    public void testContentValidation() {
        assertThrows(IllegalArgumentException.class, () -> testReclamation.setContent(null));
        assertThrows(IllegalArgumentException.class, () -> testReclamation.setContent(""));
        assertThrows(IllegalArgumentException.class, () -> testReclamation.setContent("   "));
        assertThrows(IllegalArgumentException.class, () -> testReclamation.setContent("Short"));
    }

    @Test
    public void testValidContent() {
        String validContent = "This is a valid reclamation content with more than 10 characters";
        assertDoesNotThrow(() -> testReclamation.setContent(validContent));
        assertEquals(validContent, testReclamation.getContent());
    }

    @Test
    public void testTypeValidation() {
        assertThrows(IllegalArgumentException.class, () -> testReclamation.setType(null));
    }

    @Test
    public void testStatusValidation() {
        assertThrows(IllegalArgumentException.class, () -> testReclamation.setStatus(null));
    }

    @Test
    public void testAllReclamationTypes() {
        testReclamation.setType(ReclamationType.ACCOUNT);
        assertEquals(ReclamationType.ACCOUNT, testReclamation.getType());

        testReclamation.setType(ReclamationType.BUG);
        assertEquals(ReclamationType.BUG, testReclamation.getType());

        testReclamation.setType(ReclamationType.COACHING);
        assertEquals(ReclamationType.COACHING, testReclamation.getType());

        testReclamation.setType(ReclamationType.PAYMENT);
        assertEquals(ReclamationType.PAYMENT, testReclamation.getType());

        testReclamation.setType(ReclamationType.OTHER);
        assertEquals(ReclamationType.OTHER, testReclamation.getType());
    }

    @Test
    public void testAllReclamationStatuses() {
        testReclamation.setStatus(ReclamationStatus.PENDING);
        assertEquals(ReclamationStatus.PENDING, testReclamation.getStatus());

        testReclamation.setStatus(ReclamationStatus.IN_PROGRESS);
        assertEquals(ReclamationStatus.IN_PROGRESS, testReclamation.getStatus());

        testReclamation.setStatus(ReclamationStatus.ANSWERED);
        assertEquals(ReclamationStatus.ANSWERED, testReclamation.getStatus());

        testReclamation.setStatus(ReclamationStatus.RESOLVED);
        assertEquals(ReclamationStatus.RESOLVED, testReclamation.getStatus());

        testReclamation.setStatus(ReclamationStatus.REJECTED);
        assertEquals(ReclamationStatus.REJECTED, testReclamation.getStatus());
    }

    @Test
    public void testAddResponse() {
        Response response = new Response();
        response.setId(1);
        response.setContent("This is a response to the reclamation");
        response.setCreatedAt(LocalDateTime.now());

        testReclamation.addResponse(response);

        assertEquals(1, testReclamation.getResponses().size());
        assertEquals(response, testReclamation.getResponses().get(0));
        assertEquals(testReclamation, response.getReclamation());
    }

    @Test
    public void testRemoveResponse() {
        Response response = new Response();
        response.setId(1);
        response.setContent("This is a response to the reclamation");
        response.setCreatedAt(LocalDateTime.now());

        testReclamation.addResponse(response);
        assertEquals(1, testReclamation.getResponses().size());

        testReclamation.removeResponse(response);
        assertEquals(0, testReclamation.getResponses().size());
    }

    @Test
    public void testPhotoPath() {
        String photoPath = "uploads/reclamations/test_image.jpg";
        testReclamation.setPhotoPath(photoPath);
        assertEquals(photoPath, testReclamation.getPhotoPath());
    }

    @Test
    public void testPostId() {
        testReclamation.setPostId(42);
        assertEquals(42, testReclamation.getPostId());

        testReclamation.setPostId(null);
        assertNull(testReclamation.getPostId());
    }
}
