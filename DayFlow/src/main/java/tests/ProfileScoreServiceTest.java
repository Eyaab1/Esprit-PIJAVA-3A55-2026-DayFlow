package tests;

import model.profile.ProfileAnalysisResult;
import model.profile.ProfileSnapshot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import services.profile.ProfileScoreService;

import java.util.List;

public class ProfileScoreServiceTest {

    private final ProfileScoreService service = new ProfileScoreService();

    @Test
    void shouldGiveHighScoreForCompleteProfile() {
        ProfileSnapshot snapshot = new ProfileSnapshot(
                "Experienced wellness coach helping users build long-term healthy habits with practical routines.",
                List.of("Nutrition", "Fitness", "Mindfulness", "Sleep", "Time management"),
                List.of("Morning routine", "Home workout", "Weekly plan"),
                6
        );

        ProfileAnalysisResult result = service.analyze(snapshot);

        Assertions.assertTrue(result.getScore() >= 90);
        Assertions.assertFalse(result.getRecommendations().isEmpty());
    }

    @Test
    void shouldGiveLowScoreForEmptyProfile() {
        ProfileSnapshot snapshot = new ProfileSnapshot("", List.of(), List.of(), 0);

        ProfileAnalysisResult result = service.analyze(snapshot);

        Assertions.assertTrue(result.getScore() <= 20);
        Assertions.assertTrue(result.getRecommendations().size() >= 3);
    }
}
