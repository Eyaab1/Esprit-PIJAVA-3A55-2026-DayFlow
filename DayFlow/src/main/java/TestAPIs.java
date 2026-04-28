import services.ai.GroqAIService;
import services.ai.LibreTranslateService;
import services.ai.PerspectiveAPIService;
import model.reclamation.Reclamation;
import enums.ReclamationType;

/**
 * Standalone test for the new API integrations.
 * Run this to test Groq, Perspective, and LibreTranslate APIs.
 */
public class TestAPIs {

    public static void main(String[] args) {
        System.out.println("=== Testing DayFlow API Integrations ===\n");

        testGroqAI();
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        testPerspectiveAPI();
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        testLibreTranslate();
        
        System.out.println("\n=== All Tests Complete ===");
    }

    private static void testGroqAI() {
        System.out.println("📝 Testing Groq AI (Response Suggestions)...\n");
        
        try {
            GroqAIService groq = new GroqAIService();
            
            if (!groq.isConfigured()) {
                System.out.println("⚠️  Groq API not configured. Add groq.api.key to application.properties");
                return;
            }
            
            // Create a test reclamation
            Reclamation testReclamation = new Reclamation();
            testReclamation.setContent("L'application ne fonctionne pas correctement. Je ne peux pas me connecter.");
            testReclamation.setType(ReclamationType.BUG);
            
            System.out.println("Generating response for:");
            System.out.println("  Type: " + testReclamation.getType());
            System.out.println("  Content: " + testReclamation.getContent());
            System.out.println("\nGenerating...");
            
            String suggestion = groq.generateResponseSuggestion(testReclamation);
            
            System.out.println("\n✅ AI Suggestion Generated:");
            System.out.println("─".repeat(50));
            System.out.println(suggestion);
            System.out.println("─".repeat(50));
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testPerspectiveAPI() {
        System.out.println("🛡️  Testing Perspective API (Content Moderation)...\n");
        
        try {
            PerspectiveAPIService perspective = new PerspectiveAPIService();
            
            if (!perspective.isConfigured()) {
                System.out.println("⚠️  Perspective API not configured. Add perspective.api.key to application.properties");
                return;
            }
            
            // Test 1: Clean message
            System.out.println("Test 1: Clean message");
            String cleanText = "L'application ne fonctionne pas. Pouvez-vous m'aider?";
            System.out.println("  Text: " + cleanText);
            
            PerspectiveAPIService.ModerationResult result1 = perspective.analyzeText(cleanText);
            System.out.println("  Result: " + (result1.isHarmful() ? "❌ HARMFUL" : "✅ CLEAN"));
            System.out.println("  Score: " + String.format("%.0f%%", result1.getMaxScore() * 100));
            System.out.println("  Reason: " + result1.getReason());
            
            System.out.println();
            
            // Test 2: Toxic message
            System.out.println("Test 2: Toxic message");
            String toxicText = "Vous êtes des idiots! Cette application est nulle!";
            System.out.println("  Text: " + toxicText);
            
            PerspectiveAPIService.ModerationResult result2 = perspective.analyzeText(toxicText);
            System.out.println("  Result: " + (result2.isHarmful() ? "❌ HARMFUL" : "✅ CLEAN"));
            System.out.println("  Score: " + String.format("%.0f%%", result2.getMaxScore() * 100));
            System.out.println("  Reason: " + result2.getReason());
            
            if (result2.getScores() != null && !result2.getScores().isEmpty()) {
                System.out.println("  Detailed Scores:");
                result2.getScores().forEach((key, value) -> 
                    System.out.println("    " + key + ": " + String.format("%.0f%%", value * 100))
                );
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testLibreTranslate() {
        System.out.println("🌐 Testing LibreTranslate (Translation)...\n");
        
        try {
            LibreTranslateService translator = new LibreTranslateService();
            
            if (!translator.isConfigured()) {
                System.out.println("⚠️  LibreTranslate not configured.");
                return;
            }
            
            // Test 1: English to French
            System.out.println("Test 1: English → French");
            String englishText = "Hello, how are you? The application is working well.";
            System.out.println("  Original: " + englishText);
            
            String frenchText = translator.translateToFrench(englishText);
            System.out.println("  Translated: " + frenchText);
            
            System.out.println();
            
            // Test 2: French to English
            System.out.println("Test 2: French → English");
            String frenchOriginal = "Bonjour, comment allez-vous? L'application fonctionne bien.";
            System.out.println("  Original: " + frenchOriginal);
            
            String englishTranslated = translator.translateToEnglish(frenchOriginal);
            System.out.println("  Translated: " + englishTranslated);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
