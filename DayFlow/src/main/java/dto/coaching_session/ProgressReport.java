package dto.coaching_session;

import java.util.List;

public record ProgressReport(
        int coachingRequestId,
        int userId,
        int coachId,
        int currentScore,
        int previousScore,
        int scoreChange,
        List<ProgressSnapshot> sessions,
        String overallRecommendation
) {
    public record ProgressSnapshot(
            int sessionId,
            String sessionStatus,
            Integer scoreBefore,
            Integer scoreAfter,
            Integer scoreChange,
            Integer coachRating,
            String userFeedback,
            String userComment,
            String coachRemarks,
            String coachRecommendations,
            String nextAction,
            String programAdjustment
    ) {
    }
}
