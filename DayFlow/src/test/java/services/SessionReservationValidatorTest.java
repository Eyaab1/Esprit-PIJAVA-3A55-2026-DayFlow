package services;

import exceptions.ReservationLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import services.coaching_session_module.SessionReservationValidator;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour le validateur de réservation de sessions.
 * 
 * Tests:
 * - Comptage des sessions futures
 * - Vérification de la possibilité de réservation
 * - Validation avec exception
 * - Calcul des créneaux restants
 */
public class SessionReservationValidatorTest {

    private static final int TEST_USER_ID = 999;
    private static final int MAX_SESSIONS = 3;

    @BeforeEach
    public void setUp() {
        // Nettoyer les données de test avant chaque test
        // (À implémenter selon votre stratégie de test)
    }

    /**
     * Test: Compter les sessions futures quand l'utilisateur n'en a aucune.
     */
    @Test
    public void testCountFutureSessionsWhenNone() throws SQLException {
        int count = SessionReservationValidator.countFutureSessions(TEST_USER_ID);
        assertEquals(0, count, "Devrait retourner 0 quand l'utilisateur n'a pas de sessions");
    }

    /**
     * Test: Vérifier que l'utilisateur peut réserver quand il n'a pas atteint la limite.
     */
    @Test
    public void testCanBookSessionWhenUnderLimit() throws SQLException {
        boolean canBook = SessionReservationValidator.canBookSession(TEST_USER_ID);
        assertTrue(canBook, "Devrait pouvoir réserver quand sous la limite");
    }

    /**
     * Test: Vérifier que l'utilisateur ne peut pas réserver quand il a atteint la limite.
     */
    @Test
    public void testCannotBookSessionWhenLimitReached() throws SQLException {
        // Créer 3 sessions futures pour l'utilisateur
        // (À implémenter selon votre stratégie de test)
        
        boolean canBook = SessionReservationValidator.canBookSession(TEST_USER_ID);
        assertFalse(canBook, "Ne devrait pas pouvoir réserver quand la limite est atteinte");
    }

    /**
     * Test: Valider la réservation quand c'est possible.
     */
    @Test
    public void testValidateReservationWhenAllowed() throws SQLException, ReservationLimitExceededException {
        // Ne devrait pas lever d'exception
        assertDoesNotThrow(() -> {
            SessionReservationValidator.validateReservation(TEST_USER_ID);
        }, "Ne devrait pas lever d'exception quand la réservation est autorisée");
    }

    /**
     * Test: Valider la réservation quand la limite est atteinte.
     */
    @Test
    public void testValidateReservationWhenLimitReached() throws SQLException {
        // Créer 3 sessions futures pour l'utilisateur
        // (À implémenter selon votre stratégie de test)
        
        // Devrait lever une exception
        assertThrows(ReservationLimitExceededException.class, () -> {
            SessionReservationValidator.validateReservation(TEST_USER_ID);
        }, "Devrait lever une exception quand la limite est atteinte");
    }

    /**
     * Test: Calculer les créneaux restants.
     */
    @Test
    public void testGetRemainingSlots() throws SQLException {
        int remaining = SessionReservationValidator.getRemainingSlots(TEST_USER_ID);
        assertEquals(MAX_SESSIONS, remaining, "Devrait retourner 3 créneaux restants");
    }

    /**
     * Test: Vérifier le message d'exception.
     */
    @Test
    public void testExceptionMessage() {
        ReservationLimitExceededException exception = 
            new ReservationLimitExceededException(TEST_USER_ID, 3, MAX_SESSIONS);
        
        String message = exception.getUserFriendlyMessage();
        assertTrue(message.contains("3"), "Le message devrait contenir la limite");
        assertTrue(message.contains("sessions futures"), "Le message devrait mentionner les sessions");
    }

    /**
     * Test: Vérifier les propriétés de l'exception.
     */
    @Test
    public void testExceptionProperties() {
        ReservationLimitExceededException exception = 
            new ReservationLimitExceededException(TEST_USER_ID, 3, MAX_SESSIONS);
        
        assertEquals(TEST_USER_ID, exception.getUserId());
        assertEquals(3, exception.getCurrentCount());
        assertEquals(MAX_SESSIONS, exception.getMaxLimit());
        assertEquals(0, exception.getRemainingSlots());
    }

    /**
     * Test: Vérifier la limite maximale.
     */
    @Test
    public void testMaxFutureSessions() {
        int max = SessionReservationValidator.getMaxFutureSessions();
        assertEquals(MAX_SESSIONS, max, "La limite maximale devrait être 3");
    }

    /**
     * Test: Vérifier les statuts comptabilisés.
     */
    @Test
    public void testCountedStatuses() {
        String[] statuses = SessionReservationValidator.getCountedStatuses();
        assertNotNull(statuses, "Les statuts ne devraient pas être null");
        assertEquals(3, statuses.length, "Devrait avoir 3 statuts comptabilisés");
    }
}
