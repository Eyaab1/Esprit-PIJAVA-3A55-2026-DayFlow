package services.chatroom;

import java.util.List;

/**
 * Validation et normalisation centralisées des messages.
 * Toutes les règles métier sont ici — jamais dans le controller.
 */
public final class MessageValidator {

    private static final int CONTENT_MIN = 1;
    private static final int CONTENT_MAX = 1000;
    private static final int SEARCH_MIN  = 2;
    private static final int SEARCH_MAX  = 50;

    /** Mots interdits — à compléter selon les besoins du projet */
    private static final List<String> BANNED_WORDS = List.of(
        "spam", "badword", "insulte"
    );

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

        // Filtrage mots interdits
        String lower = content.toLowerCase();
        for (String banned : BANNED_WORDS) {
            if (lower.contains(banned.toLowerCase()))
                throw new IllegalArgumentException("Message contient un mot interdit.");
        }

        return content;
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
