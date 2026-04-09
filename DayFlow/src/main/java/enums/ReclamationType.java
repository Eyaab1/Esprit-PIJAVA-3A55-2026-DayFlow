package enums;

/**
 * Équivalent de {@code ReclamationTypeEnum} côté Symfony / Doctrine.
 */
public enum ReclamationType {
    BUG("bug"),
    FEATURE("feature"),
    ACCOUNT("account"),
    PAYMENT("payment"),
    OTHER("other");

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
