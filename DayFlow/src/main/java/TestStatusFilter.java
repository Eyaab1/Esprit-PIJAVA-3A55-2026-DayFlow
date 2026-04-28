import enums.ReclamationStatus;
import enums.ReclamationType;
import model.reclamation.Reclamation;
import services.reclamation.ReclamationService;

import java.sql.SQLException;
import java.util.List;

/**
 * Test utility to debug status filter issues.
 * Run this to verify the filter works correctly.
 */
public class TestStatusFilter {
    
    public static void main(String[] args) {
        ReclamationService service = new ReclamationService();
        
        System.out.println("=== Testing Status Filter ===\n");
        
        try {
            // Test 1: Get all reclamations
            System.out.println("Test 1: All reclamations");
            List<Reclamation> all = service.findForAdmin(null, null, null, 100, 0);
            int totalCount = service.countForAdmin(null, null, null);
            System.out.println("Found: " + all.size() + " reclamations");
            System.out.println("Count: " + totalCount + " reclamations");
            System.out.println();
            
            // Test 2: Filter by PENDING status
            System.out.println("Test 2: PENDING status only");
            List<Reclamation> pending = service.findForAdmin(ReclamationStatus.PENDING, null, null, 100, 0);
            int pendingCount = service.countForAdmin(ReclamationStatus.PENDING, null, null);
            System.out.println("Found: " + pending.size() + " pending reclamations");
            System.out.println("Count: " + pendingCount + " pending reclamations");
            if (!pending.isEmpty()) {
                System.out.println("Sample: #" + pending.get(0).getId() + " - " + pending.get(0).getStatus());
            }
            System.out.println();
            
            // Test 3: Filter by ANSWERED status
            System.out.println("Test 3: ANSWERED status only");
            List<Reclamation> answered = service.findForAdmin(ReclamationStatus.ANSWERED, null, null, 100, 0);
            int answeredCount = service.countForAdmin(ReclamationStatus.ANSWERED, null, null);
            System.out.println("Found: " + answered.size() + " answered reclamations");
            System.out.println("Count: " + answeredCount + " answered reclamations");
            System.out.println();
            
            // Test 4: Filter by type
            System.out.println("Test 4: BUG type only");
            List<Reclamation> bugs = service.findForAdmin(null, ReclamationType.BUG, null, 100, 0);
            int bugsCount = service.countForAdmin(null, ReclamationType.BUG, null);
            System.out.println("Found: " + bugs.size() + " bug reclamations");
            System.out.println("Count: " + bugsCount + " bug reclamations");
            System.out.println();
            
            // Test 5: Combined filters
            System.out.println("Test 5: PENDING + BUG");
            List<Reclamation> pendingBugs = service.findForAdmin(
                    ReclamationStatus.PENDING, ReclamationType.BUG, null, 100, 0);
            int pendingBugsCount = service.countForAdmin(
                    ReclamationStatus.PENDING, ReclamationType.BUG, null);
            System.out.println("Found: " + pendingBugs.size() + " pending bug reclamations");
            System.out.println("Count: " + pendingBugsCount + " pending bug reclamations");
            System.out.println();
            
            // Test 6: Search
            System.out.println("Test 6: Search for 'test'");
            List<Reclamation> searched = service.findForAdmin(null, null, "test", 100, 0);
            int searchedCount = service.countForAdmin(null, null, "test");
            System.out.println("Found: " + searched.size() + " reclamations containing 'test'");
            System.out.println("Count: " + searchedCount + " reclamations containing 'test'");
            System.out.println();
            
            // Summary
            System.out.println("=== Summary ===");
            System.out.println("Total reclamations: " + totalCount);
            System.out.println("PENDING: " + pendingCount);
            System.out.println("ANSWERED: " + answeredCount);
            System.out.println("BUG type: " + bugsCount);
            System.out.println();
            
            if (pendingCount == 0) {
                System.out.println("⚠️  WARNING: No PENDING reclamations found!");
                System.out.println("   Check your database status values.");
                System.out.println("   Run: SELECT DISTINCT status FROM reclamation;");
            } else {
                System.out.println("✅ Status filter is working correctly!");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
