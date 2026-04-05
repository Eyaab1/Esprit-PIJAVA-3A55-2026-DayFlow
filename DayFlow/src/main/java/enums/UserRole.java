package enums;

/**
 * Équivalent de {@code App\Enum\UserRole} (Symfony / PHP backed enum string).
 */
public enum UserRole {

    USER("ROLE_USER"),
    COACH("ROLE_COACH"),
    ADMIN("ROLE_ADMIN");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    /** Valeur persistée en base / Symfony ({@code ROLE_*}). */
    public String getValue() {
        return value;
    }

    /**
     * Résout une valeur {@code ROLE_*} venant de la base ou de l’API.
     *
     * @throws IllegalArgumentException si la chaîne ne correspond à aucun rôle
     */
    public static UserRole fromValue(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Rôle null ou vide");
        }
        for (UserRole r : values()) {
            if (r.value.equals(role)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Rôle inconnu: " + role);
    }
}
