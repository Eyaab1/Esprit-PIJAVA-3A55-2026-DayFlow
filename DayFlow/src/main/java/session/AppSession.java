package session;

import enums.UserRole;
import model.user.User;

import java.util.Optional;

/**
 * Utilisateur connecté courant (session applicative JavaFX).
 */
public final class AppSession {

    private static User currentUser;
    private static String sessionToken;
    private static String sessionDeviceLabel;

    private AppSession() {
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void setCurrentUser(User user, String currentSessionToken, String currentSessionDeviceLabel) {
        currentUser = user;
        sessionToken = currentSessionToken;
        sessionDeviceLabel = currentSessionDeviceLabel;
    }

    public static Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public static void clear() {
        currentUser = null;
        sessionToken = null;
        sessionDeviceLabel = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static Optional<String> getSessionToken() {
        return Optional.ofNullable(sessionToken);
    }

    public static Optional<String> getSessionDeviceLabel() {
        return Optional.ofNullable(sessionDeviceLabel);
    }

    /** Vrai si au moins un rôle coach est présent. */
    public static boolean isCoach() {
        return getCurrentUser()
                .map(u -> u.getRoles() != null
                        && u.getRoles().stream().anyMatch(UserRole.COACH.getValue()::equals))
                .orElse(false);
    }

    /** Vrai si l'utilisateur a le rôle application {@code ROLE_ADMIN}. */
    public static boolean isAdmin() {
        return getCurrentUser()
                .map(u -> u.getRoles() != null
                        && u.getRoles().stream().anyMatch(UserRole.ADMIN.getValue()::equals))
                .orElse(false);
    }
}
