package services.coaching_session_module;

/**
 * Plage de prix des coachs (équivalent {@code UserRepository::getCoachPriceRange} Symfony).
 */
public record CoachPriceRange(Double min, Double max) {
}
