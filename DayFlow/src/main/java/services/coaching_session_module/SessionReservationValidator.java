package services.coaching_session_module;

import exceptions.ReservationLimitExceededException;
import model.coaching_session.Session;
import utils.DbConnexion;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Validateur pour les règles métier de réservation de sessions.
 * 
 * Responsabilités:
 * - Compter les sessions futures d'un utilisateur
 * - Vérifier si un utilisateur peut réserver une nouvelle session
 * - Appliquer les règles de limitation de réservation
 * 
 * Règles métier:
 * - Limite maximale: 3 sessions futures par utilisateur
 * - Sessions futures: statut "confirmed" ou "scheduling" (propositions acceptées)
 * - Date future: date > aujourd'hui OU (date = aujourd'hui ET heure_debut > heure actuelle)
 */
public class SessionReservationValidator {
    
    // Configuration
    private static final int MAX_FUTURE_SESSIONS = 3;
    private static final String[] COUNTED_STATUSES = {
        Session.STATUS_CONFIRMED,
        Session.STATUS_PROPOSED_BY_USER,
        Session.STATUS_PROPOSED_BY_COACH
    };
    
    /**
     * Compte le nombre de sessions futures pour un utilisateur.
     * 
     * Une session est considérée comme future si:
     * - Sa date est après la date actuelle OU
     * - Sa date est aujourd'hui avec une heure_debut > heure actuelle
     * 
     * Seules les sessions avec les statuts suivants sont comptées:
     * - confirmed
     * - proposed_by_user
     * - proposed_by_coach
     * 
     * @param userId ID de l'utilisateur
     * @return Nombre de sessions futures
     * @throws SQLException En cas d'erreur de base de données
     */
    public static int countFutureSessions(int userId) throws SQLException {
        System.out.println("[SessionReservationValidator] Counting future sessions for user " + userId);
        
        String sql = """
            SELECT COUNT(*) as count FROM session s
            INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
            WHERE cr.user_id = ?
              AND s.status IN (?, ?, ?)
              AND (
                CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) > CURRENT_DATE
                OR (
                  CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS date) = CURRENT_DATE
                  AND CAST(COALESCE(s.scheduled_at, s.proposed_time_by_coach, s.proposed_time_by_user) AS time) > CURRENT_TIME
                )
              )
            """;
        
        Connection cnx = DbConnexion.getConnection();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, Session.STATUS_CONFIRMED);
            ps.setString(3, Session.STATUS_PROPOSED_BY_USER);
            ps.setString(4, Session.STATUS_PROPOSED_BY_COACH);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    System.out.println("[SessionReservationValidator] User " + userId + " has " + count + " future sessions");
                    return count;
                }
            }
        } catch (SQLException e) {
            System.err.println("[SessionReservationValidator] Error counting future sessions: " + e.getMessage());
            throw e;
        }
        
        return 0;
    }

    /**
     * Vérifie si un utilisateur peut réserver une nouvelle session.
     * 
     * @param userId ID de l'utilisateur
     * @return true si l'utilisateur peut réserver, false sinon
     * @throws SQLException En cas d'erreur de base de données
     */
    public static boolean canBookSession(int userId) throws SQLException {
        int count = countFutureSessions(userId);
        boolean canBook = count < MAX_FUTURE_SESSIONS;
        
        System.out.println("[SessionReservationValidator] User " + userId + " can book: " + canBook + 
                         " (current: " + count + ", max: " + MAX_FUTURE_SESSIONS + ")");
        
        return canBook;
    }

    /**
     * Vérifie si un utilisateur peut réserver et lève une exception si non.
     * 
     * @param userId ID de l'utilisateur
     * @throws ReservationLimitExceededException Si la limite est atteinte
     * @throws SQLException En cas d'erreur de base de données
     */
    public static void validateReservation(int userId) throws ReservationLimitExceededException, SQLException {
        int currentCount = countFutureSessions(userId);
        
        if (currentCount >= MAX_FUTURE_SESSIONS) {
            System.err.println("[SessionReservationValidator] Reservation blocked for user " + userId + 
                             ": limit reached (" + currentCount + "/" + MAX_FUTURE_SESSIONS + ")");
            
            throw new ReservationLimitExceededException(userId, currentCount, MAX_FUTURE_SESSIONS);
        }
        
        System.out.println("[SessionReservationValidator] Reservation allowed for user " + userId + 
                         " (" + currentCount + "/" + MAX_FUTURE_SESSIONS + ")");
    }

    /**
     * Retourne le nombre de sessions que l'utilisateur peut encore réserver.
     * 
     * @param userId ID de l'utilisateur
     * @return Nombre de réservations restantes (0 si limite atteinte)
     * @throws SQLException En cas d'erreur de base de données
     */
    public static int getRemainingSlots(int userId) throws SQLException {
        int currentCount = countFutureSessions(userId);
        return Math.max(0, MAX_FUTURE_SESSIONS - currentCount);
    }

    /**
     * Retourne la limite maximale de sessions futures.
     * 
     * @return Limite maximale (actuellement 3)
     */
    public static int getMaxFutureSessions() {
        return MAX_FUTURE_SESSIONS;
    }

    /**
     * Retourne les statuts comptabilisés pour les sessions futures.
     * 
     * @return Tableau des statuts comptabilisés
     */
    public static String[] getCountedStatuses() {
        return COUNTED_STATUSES;
    }

    /**
     * Journalise un refus de réservation.
     * 
     * @param userId ID de l'utilisateur
     * @param reason Raison du refus
     */
    public static void logReservationRefusal(int userId, String reason) {
        System.err.println("[SessionReservationValidator] RESERVATION REFUSED - User: " + userId + 
                         ", Reason: " + reason + ", Timestamp: " + System.currentTimeMillis());
    }
}
