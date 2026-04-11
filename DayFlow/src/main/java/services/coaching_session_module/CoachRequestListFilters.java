package services.coaching_session_module;

/**
 * Filtres équivalents à {@code index} du {@code CoachingRequestController} Symfony
 * (search, status, date_from, date_to, priority).
 */
public record CoachRequestListFilters(
        String search,
        String status,
        String dateFrom,
        String dateTo,
        String priority
) {
    public static CoachRequestListFilters empty() {
        return new CoachRequestListFilters("", "", "", "", "");
    }
}
