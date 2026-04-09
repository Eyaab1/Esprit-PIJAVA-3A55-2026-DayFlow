package enums;


public enum UserRole {

    USER("ROLE_USER"),
    COACH("ROLE_COACH"),
    ADMIN("ROLE_ADMIN");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

 
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
