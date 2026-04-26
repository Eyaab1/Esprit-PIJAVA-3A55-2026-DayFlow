package enums;

/**
 * Statuts possibles pour un paiement.
 */
public enum PaymentStatus {
    /**
     * Paiement en attente (créé mais pas encore traité).
     */
    PENDING("pending", "En attente"),

    /**
     * Paiement en cours de traitement par Stripe.
     */
    PROCESSING("processing", "En cours"),

    /**
     * Paiement réussi et confirmé.
     */
    SUCCEEDED("succeeded", "Réussi"),

    /**
     * Paiement échoué.
     */
    FAILED("failed", "Échoué"),

    /**
     * Paiement annulé par l'utilisateur.
     */
    CANCELLED("cancelled", "Annulé"),

    /**
     * Paiement remboursé.
     */
    REFUNDED("refunded", "Remboursé");

    private final String value;
    private final String displayName;

    PaymentStatus(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Convertit une chaîne en PaymentStatus.
     *
     * @param value la valeur string
     * @return le PaymentStatus correspondant
     * @throws IllegalArgumentException si la valeur n'existe pas
     */
    public static PaymentStatus fromValue(String value) {
        if (value == null) {
            return PENDING;
        }
        for (PaymentStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Statut de paiement invalide : " + value);
    }

    /**
     * Vérifie si le paiement est dans un état final (ne peut plus changer).
     *
     * @return true si le statut est final
     */
    public boolean isFinal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED || this == REFUNDED;
    }

    /**
     * Vérifie si le paiement est réussi.
     *
     * @return true si le paiement est réussi
     */
    public boolean isSuccessful() {
        return this == SUCCEEDED;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
