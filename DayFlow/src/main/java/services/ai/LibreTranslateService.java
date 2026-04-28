package services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

/**
 * Service for translating text using MyMemory Translation API (free and reliable).
 * No API key required for basic usage (up to 1000 words/day).
 */
public class LibreTranslateService {

    private static final String API_URL = "https://api.mymemory.translated.net/get";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LibreTranslateService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Translates text from English to French.
     *
     * @param text Text to translate (in English)
     * @return Translated text in French
     * @throws IOException If translation fails
     */
    public String translateToFrench(String text) throws IOException, InterruptedException {
        return translate(text, "en", "fr");
    }

    /**
     * Translates text from French to English.
     *
     * @param text Text to translate (in French)
     * @return Translated text in English
     * @throws IOException If translation fails
     */
    public String translateToEnglish(String text) throws IOException, InterruptedException {
        return translate(text, "fr", "en");
    }

    /**
     * Translates text between languages using MyMemory API.
     *
     * @param text Text to translate
     * @param sourceLang Source language code (e.g., "en", "fr")
     * @param targetLang Target language code (e.g., "en", "fr")
     * @return Translated text
     * @throws IOException If translation fails
     */
    public String translate(String text, String sourceLang, String targetLang) 
            throws IOException, InterruptedException {
        
        if (text == null || text.isBlank()) {
            return text;
        }

        // If source and target are the same, return original text
        if (sourceLang.equals(targetLang)) {
            return text;
        }

        // Encode text for URL
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String langPair = sourceLang + "|" + targetLang;
        String encodedLangPair = URLEncoder.encode(langPair, StandardCharsets.UTF_8);
        
        // Build URL with query parameters
        String url = API_URL + "?q=" + encodedText + "&langpair=" + encodedLangPair;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("MyMemory API error (code " + response.statusCode() + "): " + response.body());
        }

        String translatedText = parseResponse(response.body());
        
        // If translation is null or empty, return original text
        if (translatedText == null || translatedText.isBlank()) {
            return text;
        }
        
        return translatedText;
    }

    /**
     * Parses the translation response from MyMemory API.
     */
    private String parseResponse(String responseBody) throws IOException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            
            // Check response status
            int responseStatus = root.path("responseStatus").asInt();
            if (responseStatus != 200) {
                String errorMsg = root.path("responseDetails").asText("Unknown error");
                throw new IOException("Translation failed: " + errorMsg);
            }
            
            // Get translated text
            String translatedText = root.path("responseData")
                    .path("translatedText")
                    .asText();
            
            // Check if translatedText is null or empty
            if (translatedText == null || translatedText.isBlank() || translatedText.equals("null")) {
                throw new IOException("No translation found in response");
            }
            
            return translatedText;
        } catch (Exception e) {
            throw new IOException("Error parsing translation response: " + e.getMessage() + " - Response: " + responseBody, e);
        }
    }

    /**
     * Checks if the service is configured.
     */
    public boolean isConfigured() {
        return true; // MyMemory doesn't require API key
    }
}
