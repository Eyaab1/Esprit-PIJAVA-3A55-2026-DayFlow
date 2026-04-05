package utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt facteur 13 (aligné sur Symfony). Les hash {@code $2y$} PHP sont normalisés en {@code $2a$} pour la vérif Java.
 */
public final class PasswordHasher {

    private static final int BCRYPT_STRENGTH = 13;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(BCRYPT_STRENGTH);

    private PasswordHasher() {
    }

    public static String hash(String plainPassword) {
        return ENCODER.encode(plainPassword);
    }

    public static boolean matches(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        return ENCODER.matches(plainPassword, normalizeSymfonyBcrypt(storedHash));
    }

    private static String normalizeSymfonyBcrypt(String hash) {
        if (hash.startsWith("$2y$")) {
            return "$2a$" + hash.substring(4);
        }
        return hash;
    }
}
