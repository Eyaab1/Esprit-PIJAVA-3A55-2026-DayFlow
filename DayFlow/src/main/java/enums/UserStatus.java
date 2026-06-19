package enums;

/**
 * Équivalent de {@code App\Enum\UserStatus} (Symfony / PHP backed enum string).
 */
public enum UserStatus {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    BANNED("BANNED"),
    PENDING("PENDING");

    public final String value;

    UserStatus(String value) {
        this.value = value;
    }

    public static UserStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (UserStatus s : values()) {
            if (s.value.equalsIgnoreCase(value)) {
                return s;
            }
        }
        return null;
    }
}
