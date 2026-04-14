package session;

import enums.UserRole;
import model.user.User;

import java.util.Optional;

/**
 * Utilisateur connecté courant (session applicative JavaFX).
 */
public final class AppSession {

    private static User currentUser;

    private AppSession() {
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public static void clear() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
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
