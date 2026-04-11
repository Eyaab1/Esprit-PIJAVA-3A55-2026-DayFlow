package enums;

/**
 * Équivalent de {@code ReclamationTypeEnum} côté Symfony / Doctrine.
 */
public enum ReclamationType {
    BUG("BUG"),
    FEATURE("FEATURE"),
    ACCOUNT("ACCOUNT"),
    PAYMENT("PAYMENT"),
    OTHER("OTHER");

    public final String value;

    ReclamationType(String value) {
        this.value = value;
    }

    public static ReclamationType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ReclamationType t : values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }
        return null;
    }
}
