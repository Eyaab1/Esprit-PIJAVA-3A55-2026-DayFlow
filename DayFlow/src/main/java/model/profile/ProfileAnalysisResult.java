package model.profile;

import java.util.ArrayList;
import java.util.List;

public class ProfileAnalysisResult {
    private final int score;
    private final List<String> recommendations;

    public ProfileAnalysisResult(int score, List<String> recommendations) {
        this.score = Math.max(0, Math.min(100, score));
        this.recommendations = recommendations == null ? new ArrayList<>() : new ArrayList<>(recommendations);
    }

    public int getScore() {
        return score;
    }

    public List<String> getRecommendations() {
        return new ArrayList<>(recommendations);
    }
}
