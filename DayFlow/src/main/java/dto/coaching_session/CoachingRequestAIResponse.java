package dto.coaching_session;

public record CoachingRequestAIResponse(
        boolean success,
        String detectedNeed,
        Integer recommendedCoach,
        String coachName,
        Integer compatibilityScore,
        String justification,
        boolean requestSaved,
        String error
) {
    public static CoachingRequestAIResponse success(
            String detectedNeed,
            Integer recommendedCoach,
            String coachName,
            Integer compatibilityScore,
            String justification
    ) {
        return new CoachingRequestAIResponse(
                true,
                detectedNeed,
                recommendedCoach,
                coachName,
                compatibilityScore,
                justification,
                true,
                null
        );
    }

    public static CoachingRequestAIResponse failure(String error) {
        return new CoachingRequestAIResponse(
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                error
        );
    }
}
