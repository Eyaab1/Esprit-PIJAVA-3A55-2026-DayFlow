package exceptions;

/**
 * Exception levée lorsqu'une tentative de réservation est faite pour une session dans le passé.
 * 
 * Cette exception empêche les utilisateurs de réserver des sessions avec une date/heure
 * antérieure à la date/heure actuelle.
 */
public class PastSessionException extends Exception {

    /**
     * Constructeur avec message personnalisé
     * 
     * @param message Le message d'erreur
     */
    public PastSessionException(String message) {
        super(message);
    }

    /**
     * Constructeur avec message et cause
     * 
     * @param message Le message d'erreur
     * @param cause La cause de l'exception
     */
    public PastSessionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructeur par défaut avec message standard en français
     */
    public PastSessionException() {
        super("Impossible de réserver une session dans le passé");
    }
}
