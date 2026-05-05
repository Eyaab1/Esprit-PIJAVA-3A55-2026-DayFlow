package exceptions;

/**
 * Exception levée quand un utilisateur a atteint la limite de réservations futures.
 * 
 * Cette exception est utilisée pour bloquer les réservations quand l'utilisateur
 * a déjà 3 sessions futures (statut: planifiee ou confirmee).
 */
public class ReservationLimitExceededException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    private final int userId;
    private final int currentCount;
    private final int maxLimit;
    private final int remainingSlots;

    /**
     * Constructeur avec tous les détails de la limite dépassée.
     * 
     * @param userId ID de l'utilisateur
     * @param currentCount Nombre actuel de sessions futures
     * @param maxLimit Limite maximale de sessions
     */
    public ReservationLimitExceededException(int userId, int currentCount, int maxLimit) {
        super(String.format(
            "Utilisateur %d a atteint la limite de %d sessions futures (actuellement: %d)",
            userId, maxLimit, currentCount
        ));
        this.userId = userId;
        this.currentCount = currentCount;
        this.maxLimit = maxLimit;
        this.remainingSlots = Math.max(0, maxLimit - currentCount);
    }

    /**
     * Constructeur avec message personnalisé.
     * 
     * @param message Message d'erreur personnalisé
     * @param userId ID de l'utilisateur
     * @param currentCount Nombre actuel de sessions futures
     * @param maxLimit Limite maximale de sessions
     */
    public ReservationLimitExceededException(String message, int userId, int currentCount, int maxLimit) {
        super(message);
        this.userId = userId;
        this.currentCount = currentCount;
        this.maxLimit = maxLimit;
        this.remainingSlots = Math.max(0, maxLimit - currentCount);
    }

    // Getters
    public int getUserId() {
        return userId;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public int getMaxLimit() {
        return maxLimit;
    }

    public int getRemainingSlots() {
        return remainingSlots;
    }

    /**
     * Retourne un message utilisateur lisible.
     * 
     * @return Message à afficher à l'utilisateur
     */
    public String getUserFriendlyMessage() {
        return String.format(
            "Vous avez atteint la limite de %d sessions futures. " +
            "Veuillez terminer ou annuler une session avant de réserver à nouveau.",
            maxLimit
        );
    }
}
