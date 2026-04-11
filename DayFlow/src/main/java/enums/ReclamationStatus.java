package enums;

/**
 * Équivalent de {@code ReclamationStatusEnum} côté Symfony / Doctrine.
 */
public enum ReclamationStatus {
    PENDING("PENDING"),
    IN_PROGRESS("IN_PROGRESS"),
    ANSWERED("ANSWERED"),
    RESOLVED("RESOLVED"),
    REJECTED("REJECTED");

    public final String value;

    ReclamationStatus(String value) {
        this.value = value;
    }

    public static ReclamationStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ReclamationStatus s : values()) {
            if (s.value.equalsIgnoreCase(value)) {
                return s;
            }
        }
        return null;
    }
}
