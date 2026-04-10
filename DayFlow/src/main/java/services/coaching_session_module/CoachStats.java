package services.coaching_session_module;

/**
 * Agrégats affichés sur l’index coach (équivalent tableau {@code stats} Symfony).
 */
public record CoachStats(
        int total,
        int pending,
        int accepted,
        int declined,
        int sessionsToday,
        double conversionRatePercent,
        int urgent,
        int medium,
        int normal
) {
}
