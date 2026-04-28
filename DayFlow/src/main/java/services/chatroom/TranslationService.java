package services.chatroom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service de traduction — utilise MyMemory API (gratuit, sans clé).
 * Fallback automatique si la traduction retourne le texte original.
 */
public class TranslationService {

    // MyMemory — API gratuite, 5000 chars/jour
    private static final String MYMEMORY_URL = "https://api.mymemory.translated.net/get";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    /** Langues disponibles : label -> code ISO */
    public static final Map<String, String> LANGUAGES = new LinkedHashMap<>();
    static {
        LANGUAGES.put("Français", "fr");
        LANGUAGES.put("English", "en");
        LANGUAGES.put("العربية", "ar");
        LANGUAGES.put("Español", "es");
        LANGUAGES.put("Deutsch", "de");
        LANGUAGES.put("Italiano", "it");
        LANGUAGES.put("Português", "pt");
        LANGUAGES.put("中文", "zh");
    }

    /**
     * Traduit un texte vers la langue cible.
     * Détecte automatiquement la langue source.
     */
    public String translate(String text, String targetLang) throws Exception {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Texte vide.");
        }
        if (text.length() > 500) {
            throw new IllegalArgumentException("Texte trop long (max 500 chars).");
        }
        if (!LANGUAGES.containsValue(targetLang)) {
            throw new IllegalArgumentException("Langue non supportée : " + targetLang);
        }

        String clean = text.trim();

        // Essayer plusieurs sources jusqu'a une vraie traduction.
        String[] candidates = buildSourceCandidates(clean, targetLang);
        for (String sourceLang : candidates) {
            try {
                String result = callMyMemory(clean, sourceLang, targetLang);
                if (result != null
                        && !result.isBlank()
                        && !result.equalsIgnoreCase(clean)
                        && !result.toLowerCase().contains("mymemory")) {
                    return result;
                }
            } catch (Exception ignored) {
                // Continue sur le fallback suivant.
            }
        }

        throw new Exception("Traduction indisponible pour ce texte. Réessayez avec une autre langue.");
    }

    private String callMyMemory(String text, String sourceLang, String targetLang) throws Exception {
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String pair = URLEncoder.encode(sourceLang + "|" + targetLang, StandardCharsets.UTF_8);
        String url = MYMEMORY_URL + "?q=" + encodedText + "&langpair=" + pair;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new Exception("HTTP " + resp.statusCode());
        }

        JsonNode root = JSON.readTree(resp.body());
        int status = root.path("responseStatus").asInt();
        if (status != 200) {
            String detail = root.path("responseDetails").asText();
            throw new Exception(detail);
        }

        return root.path("responseData").path("translatedText").asText();
    }

    /**
     * Construit la liste des langues sources a essayer dans l'ordre.
     */
    private String[] buildSourceCandidates(String text, String targetLang) {
        String detected = detectSourceLang(text);
        if (detected.equals(targetLang)) {
            return targetLang.equals("en")
                    ? new String[]{"fr", "es", "de", "it", "ar"}
                    : new String[]{"en", "fr", "es", "de", "it"};
        }
        return new String[]{detected, "en", "fr", "es"};
    }

    /**
     * Détection heuristique de la langue source.
     */
    private static String detectSourceLang(String text) {
        // Arabe
        if (text.chars().anyMatch(c -> c >= 0x0600 && c <= 0x06FF)) return "ar";
        // Chinois
        if (text.chars().anyMatch(c -> c >= 0x4E00 && c <= 0x9FFF)) return "zh";
        // Allemand
        if (text.matches("(?i).*\\b(ich|bin|ist|das|die|der|und|nicht|haben|sein)\\b.*")) return "de";
        // Espagnol
        if (text.matches("(?i).*\\b(soy|eres|hola|gracias|como|que|por|favor)\\b.*")) return "es";
        // Français
        if (text.matches("(?i).*\\b(je|tu|il|nous|vous|ils|est|sont|suis|bonjour|merci)\\b.*")) return "fr";
        if (text.matches(".*[àâäéèêëîïôùûüçœæ].*")) return "fr";
        // Portugais
        if (text.matches(".*[ãõç].*")) return "pt";
        // Anglais par défaut
        return "en";
    }
}
