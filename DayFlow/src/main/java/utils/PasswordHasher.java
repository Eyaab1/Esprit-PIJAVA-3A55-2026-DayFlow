package utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt facteur 13 (aligné sur Symfony). Les hash {@code $2y$} PHP sont normalisés en {@code $2a$} pour la vérif Java.
 */
public final class PasswordHasher {

    private static final int BCRYPT_STRENGTH = 13;

    private PasswordHasher() {
    }

    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("plainPassword is required");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_STRENGTH));
    }

    public static boolean matches(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, normalizeSymfonyBcrypt(storedHash));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String normalizeSymfonyBcrypt(String hash) {
        if (hash.startsWith("$2y$")) {
            return "$2a$" + hash.substring(4);
        }
        return hash;
    }
}
