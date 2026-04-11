package enums;

/**
 * Aligné sur {@code App\Enum\ReclamationTypeEnum} (Symfony) — les {@link #value}
 * correspondent aux chaînes persistées en base.
 */
public enum ReclamationType {
    ACCOUNT("ACCOUNT"),
    BUG("Bug"),
    COACHING("Coaching"),
    PAYMENT("Payment"),
    OTHER("Other");

    public final String value;

    ReclamationType(String value) {
        this.value = value;
    }

    public static ReclamationType fromValue(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        for (ReclamationType t : values()) {
            if (t.value.equalsIgnoreCase(v)) {
                return t;
            }
        }
        return null;
    }
}
