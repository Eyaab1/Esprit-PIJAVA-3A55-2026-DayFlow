package services.coaching_session;

/**
 * Paramètres équivalents à {@code CoachSearchController::search} Symfony.
 */
public record CoachSearchParams(
        String query,
        String speciality,
        Double minPrice,
        Double maxPrice,
        Double minRating,
        String availability,
        String coachingType,
        String sortBy,
        String sortOrder
) {
    public static CoachSearchParams defaults() {
        return new CoachSearchParams("", "", null, null, null, "", "", "rating", "desc");
    }
}
