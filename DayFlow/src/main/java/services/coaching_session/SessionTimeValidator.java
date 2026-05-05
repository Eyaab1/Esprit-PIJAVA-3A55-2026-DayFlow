package services.coaching_session;

import exceptions.PastSessionException;
import model.coaching_session.Disponibilite;
import model.coaching_session.Session;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

/**
 * Validateur pour les contraintes temporelles des sessions.
 * 
 * Responsabilités:
 * - Vérifier qu'une session n'est pas dans le passé
 * - Vérifier qu'un créneau n'est pas dans le passé
 * - Fournir des méthodes utilitaires pour la validation temporelle
 * 
 * Règles métier:
 * - Une session ne peut être réservée que si sa date/heure est dans le futur
 * - Un créneau ne peut être sélectionné que s'il est dans le futur
 * - La comparaison se fait avec la date/heure actuelle (LocalDateTime.now())
 */
public class SessionTimeValidator {

    /**
     * Vérifie si une session est dans le passé.
     * 
     * @param session La session à vérifier
     * @return true si la session est dans le passé, false sinon
     */
    public static boolean isSessionInPast(Session session) {
        if (session == null) {
            return false;
        }

        // Récupérer l'heure affichable (scheduledAt, proposedTimeByCoach, ou proposedTimeByUser)
        Date sessionTime = session.getDisplayTime();
        
        if (sessionTime == null) {
            return false;
        }

        // Convertir en LocalDateTime pour comparaison
        LocalDateTime sessionDateTime = convertToLocalDateTime(sessionTime);
        LocalDateTime now = LocalDateTime.now();

        return sessionDateTime.isBefore(now);
    }

    /**
     * Vérifie si un créneau (Disponibilite) est dans le passé.
     * 
     * @param slot Le créneau à vérifier
     * @return true si le créneau est dans le passé, false sinon
     */
    public static boolean isSlotInPast(Disponibilite slot) {
        if (slot == null || slot.getDate() == null || slot.getHeureDebut() == null) {
            return false;
        }

        // Combiner la date et l'heure de début du créneau
        LocalDateTime slotDateTime = LocalDateTime.of(slot.getDate(), slot.getHeureDebut());
        LocalDateTime now = LocalDateTime.now();

        return slotDateTime.isBefore(now);
    }

    /**
     * Valide qu'une session n'est pas dans le passé.
     * Lève une exception si la session est dans le passé.
     * 
     * @param session La session à valider
     * @throws PastSessionException Si la session est dans le passé
     */
    public static void validateSessionNotInPast(Session session) throws PastSessionException {
        if (isSessionInPast(session)) {
            Date sessionTime = session.getDisplayTime();
            String formattedTime = formatDate(sessionTime);
            throw new PastSessionException(
                "Impossible de réserver une session dans le passé. " +
                "Date/heure proposée: " + formattedTime
            );
        }
    }

    /**
     * Valide qu'un créneau n'est pas dans le passé.
     * Lève une exception si le créneau est dans le passé.
     * 
     * @param slot Le créneau à valider
     * @throws PastSessionException Si le créneau est dans le passé
     */
    public static void validateSlotNotInPast(Disponibilite slot) throws PastSessionException {
        if (isSlotInPast(slot)) {
            String formattedTime = formatSlot(slot);
            throw new PastSessionException(
                "Impossible de réserver un créneau dans le passé. " +
                "Créneau: " + formattedTime
            );
        }
    }

    /**
     * Valide qu'une date et heure ne sont pas dans le passé.
     * 
     * @param date La date à valider
     * @param time L'heure à valider
     * @throws PastSessionException Si la date/heure est dans le passé
     */
    public static void validateDateTimeNotInPast(LocalDate date, LocalTime time) throws PastSessionException {
        if (date == null || time == null) {
            return;
        }

        LocalDateTime dateTime = LocalDateTime.of(date, time);
        LocalDateTime now = LocalDateTime.now();

        if (dateTime.isBefore(now)) {
            throw new PastSessionException(
                "Impossible de réserver une session dans le passé. " +
                "Date/heure proposée: " + dateTime
            );
        }
    }

    /**
     * Vérifie si une date/heure est dans le passé.
     * 
     * @param date La date à vérifier
     * @param time L'heure à vérifier
     * @return true si la date/heure est dans le passé, false sinon
     */
    public static boolean isDateTimeInPast(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            return false;
        }

        LocalDateTime dateTime = LocalDateTime.of(date, time);
        LocalDateTime now = LocalDateTime.now();

        return dateTime.isBefore(now);
    }

    /**
     * Convertit une Date Java en LocalDateTime.
     * 
     * @param date La date à convertir
     * @return LocalDateTime correspondant
     */
    private static LocalDateTime convertToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * Formate une Date pour affichage.
     * 
     * @param date La date à formater
     * @return String formatée (ex: "2026-05-15 14:30")
     */
    private static String formatDate(Date date) {
        if (date == null) {
            return "N/A";
        }
        LocalDateTime ldt = convertToLocalDateTime(date);
        return String.format("%04d-%02d-%02d %02d:%02d",
                ldt.getYear(),
                ldt.getMonthValue(),
                ldt.getDayOfMonth(),
                ldt.getHour(),
                ldt.getMinute());
    }

    /**
     * Formate un créneau pour affichage.
     * 
     * @param slot Le créneau à formater
     * @return String formatée (ex: "2026-05-15 09:00 - 10:00")
     */
    private static String formatSlot(Disponibilite slot) {
        if (slot == null || slot.getDate() == null || slot.getHeureDebut() == null) {
            return "N/A";
        }
        return String.format("%s %02d:%02d - %02d:%02d",
                slot.getDate(),
                slot.getHeureDebut().getHour(),
                slot.getHeureDebut().getMinute(),
                slot.getHeureFin().getHour(),
                slot.getHeureFin().getMinute());
    }

    /**
     * Obtient le nombre de minutes jusqu'à une date/heure.
     * Retourne une valeur négative si la date/heure est dans le passé.
     * 
     * @param date La date à vérifier
     * @param time L'heure à vérifier
     * @return Nombre de minutes jusqu'à la date/heure (négatif si passé)
     */
    public static long getMinutesUntil(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            return 0;
        }

        LocalDateTime dateTime = LocalDateTime.of(date, time);
        LocalDateTime now = LocalDateTime.now();

        return java.time.temporal.ChronoUnit.MINUTES.between(now, dateTime);
    }

    /**
     * Vérifie si une date/heure est suffisamment dans le futur.
     * Utile pour imposer un délai minimum avant une réservation.
     * 
     * @param date La date à vérifier
     * @param time L'heure à vérifier
     * @param minimumMinutesInFuture Nombre minimum de minutes dans le futur
     * @return true si la date/heure est au moins minimumMinutesInFuture minutes dans le futur
     */
    public static boolean isSufficientlyInFuture(LocalDate date, LocalTime time, long minimumMinutesInFuture) {
        long minutesUntil = getMinutesUntil(date, time);
        return minutesUntil >= minimumMinutesInFuture;
    }
}
