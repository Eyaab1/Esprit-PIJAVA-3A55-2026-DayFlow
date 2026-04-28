package services.profile;

import model.profile.ProfileAnalysisResult;
import model.profile.ProfileSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileScoreService {

    public ProfileAnalysisResult analyze(ProfileSnapshot snapshot) {
        int score = 0;
        List<String> recommendations = new ArrayList<>();

        String bio = snapshot.bio() == null ? "" : snapshot.bio().trim();
        int bioLength = bio.length();
        if (bioLength >= 120) {
            score += 30;
        } else if (bioLength >= 60) {
            score += 20;
            recommendations.add("Expand your bio to at least 120 characters for a stronger profile.");
        } else if (bioLength > 0) {
            score += 10;
            recommendations.add("Your bio is short. Explain goals, experience, and motivation.");
        } else {
            recommendations.add("Add a bio so others understand your profile.");
        }

        int skillsCount = snapshot.skills() == null ? 0 : snapshot.skills().stream()
                .filter(s -> s != null && !s.isBlank())
                .toList()
                .size();
        if (skillsCount >= 5) {
            score += 30;
        } else if (skillsCount >= 3) {
            score += 20;
            recommendations.add("Add 2 more skills to increase profile relevance.");
        } else if (skillsCount > 0) {
            score += 10;
            recommendations.add("Add more skills (target at least 5).");
        } else {
            recommendations.add("Add skills to improve matching and visibility.");
        }

        int preferencesCount = snapshot.preferences() == null ? 0 : snapshot.preferences().stream()
                .filter(p -> p != null && !p.isBlank())
                .toList()
                .size();
        if (preferencesCount >= 3) {
            score += 20;
        } else if (preferencesCount > 0) {
            score += 10;
            recommendations.add("Add more preferences for better personalization.");
        } else {
            recommendations.add("Add preferences to personalize recommendations.");
        }

        int weeklyActivity = Math.max(0, snapshot.weeklyActivityCount());
        if (weeklyActivity >= 5) {
            score += 20;
        } else if (weeklyActivity >= 2) {
            score += 10;
            recommendations.add("Increase weekly activity to improve profile quality.");
        } else {
            recommendations.add("Profile activity is low. Engage at least 2 times per week.");
        }

        if (score >= 85) {
            recommendations.add("Great profile. Keep activity consistent to maintain quality.");
        }

        return new ProfileAnalysisResult(score, recommendations);
    }
}
