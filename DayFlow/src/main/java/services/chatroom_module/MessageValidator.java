package services.chatroom_module;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validation et normalisation centralisées des messages.
 * Toutes les règles métier sont ici — jamais dans le controller.
 */
public final class MessageValidator {

    private static final int CONTENT_MIN = 1;
    private static final int CONTENT_MAX = 1000;
    private static final int SEARCH_MIN  = 2;
    private static final int SEARCH_MAX  = 50;

    /** Liste de toxicité (FR/EN) simple et extensible. */
    private static final List<String> TOXIC_WORDS = List.of(
        "insulte", "connard", "conne", "abruti", "abrutie", "idiot", "idiote",
        "stupide", "debile", "débile", "ta gueule", "ferme ta gueule",
        "fuck", "shit", "bitch", "asshole", "moron"
    );

    private static final Pattern URL_PATTERN =
            Pattern.compile("(?i)\\b(https?://|www\\.)\\S+");

    private MessageValidator() {}

    // ── Contenu message ───────────────────────────────────────────────────

    /**
     * Valide et normalise le contenu d'un message.
     * @return le contenu nettoyé
     * @throws IllegalArgumentException si invalide
     */
    public static String validateContent(String raw) {
        if (raw == null || raw.trim().isEmpty())
            throw new IllegalArgumentException("Le message ne peut pas être vide.");

        // Normalisation : trim + espaces multiples
        String content = raw.trim().replaceAll("\\s+", " ");

        if (content.length() < CONTENT_MIN)
            throw new IllegalArgumentException("Message trop court (min " + CONTENT_MIN + " caractère).");

        if (content.length() > CONTENT_MAX)
            throw new IllegalArgumentException("Message trop long (max " + CONTENT_MAX + " caractères).");

        ModerationResult moderation = moderateContent(content);
        if (moderation.blocked()) {
            throw new IllegalArgumentException(moderation.reason());
        }

        return content;
    }

    /**
     * Modération heuristique locale (sans API externe IA).
     * Bloque automatiquement le spam/toxicité manifeste.
     */
    public static ModerationResult moderateContent(String content) {
        String lower = content.toLowerCase(Locale.ROOT);

        for (String toxic : TOXIC_WORDS) {
            if (lower.contains(toxic.toLowerCase(Locale.ROOT))) {
                return new ModerationResult(true,
                        "Message bloqué: contenu toxique ou insultant détecté.");
            }
        }

        int spamScore = 0;
        if (content.length() > 350) spamScore += 1;
        if (countUrls(content) >= 3) spamScore += 2;
        if (hasRepeatedChars(lower, 6)) spamScore += 1;
        if (hasRepeatedWords(lower, 4)) spamScore += 2;
        if (isMostlyUppercase(content)) spamScore += 1;
        if (countSpecialChars(content) >= 12) spamScore += 1;

        if (spamScore >= 3) {
            return new ModerationResult(true,
                    "Message bloqué: spam ou contenu inapproprié détecté.");
        }

        return ModerationResult.allowed();
    }

    public record ModerationResult(boolean blocked, String reason) {
        public static ModerationResult allowed() {
            return new ModerationResult(false, "");
        }
    }

    private static int countUrls(String text) {
        var m = URL_PATTERN.matcher(text);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    private static boolean hasRepeatedChars(String text, int threshold) {
        if (text.isEmpty()) return false;
        int run = 1;
        for (int i = 1; i < text.length(); i++) {
            if (text.charAt(i) == text.charAt(i - 1)) {
                run++;
                if (run >= threshold) return true;
            } else {
                run = 1;
            }
        }
        return false;
    }

    private static boolean hasRepeatedWords(String text, int threshold) {
        String[] words = text.trim().split("\\s+");
        if (words.length == 0) return false;
        int run = 1;
        for (int i = 1; i < words.length; i++) {
            if (words[i].equals(words[i - 1])) {
                run++;
                if (run >= threshold) return true;
            } else {
                run = 1;
            }
        }
        return false;
    }

    private static boolean isMostlyUppercase(String text) {
        int letters = 0;
        int upper = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) upper++;
            }
        }
        return letters >= 12 && upper >= (int) (letters * 0.75);
    }

    private static int countSpecialChars(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) {
                count++;
            }
        }
        return count;
    }

    // ── Mot-clé de recherche ──────────────────────────────────────────────

    /**
     * Valide un mot-clé de recherche.
     * @return le mot-clé nettoyé
     */
    public static String validateSearch(String raw) {
        if (raw == null || raw.trim().isEmpty())
            throw new IllegalArgumentException("Champ de recherche vide.");

        String keyword = raw.trim();

        if (keyword.length() < SEARCH_MIN)
            throw new IllegalArgumentException("Minimum " + SEARCH_MIN + " caractères pour la recherche.");

        if (keyword.length() > SEARCH_MAX)
            throw new IllegalArgumentException("Recherche trop longue (max " + SEARCH_MAX + " caractères).");

        // Anti-injection SQL (PreparedStatement utilisé, mais double protection)
        if (keyword.matches(".*[<>\"';%()].*"))
            throw new IllegalArgumentException("Caractères non autorisés dans la recherche.");

        return keyword;
    }
}
