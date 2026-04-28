package services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

/**
 * Service for translating text using LibreTranslate API (free and open-source).
 * Translates English to French for AI-generated responses.
 */
public class LibreTranslateService {

    private static final String PROPERTIES_FILE = "/application.properties";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final String apiUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LibreTranslateService() {
        Properties props = loadProperties();
        // Try alternative instances if main one fails
        String configuredUrl = props.getProperty("libretranslate.api.url", "https://libretranslate.com/translate");
        this.apiUrl = configuredUrl;
        this.apiKey = props.getProperty("libretranslate.api.key", ""); // Optional, can be empty for public instance
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
        // Use auto-detect for source language
        return translate(text, "auto", "fr");
    }

    /**
     * Translates text from French to English.
     *
     * @param text Text to translate (in French)
     * @return Translated text in English
     * @throws IOException If translation fails
     */
    public String translateToEnglish(String text) throws IOException, InterruptedException {
        // Use auto-detect for source language
        return translate(text, "auto", "en");
    }

    /**
     * Translates text between languages.
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

        String requestBody = buildRequestBody(text, sourceLang, targetLang);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        // Add API key if configured
        if (apiKey != null && !apiKey.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String errorBody = response.body();
            // Try to extract error message
            try {
                com.fasterxml.jackson.databind.JsonNode errorNode = objectMapper.readTree(errorBody);
                String errorMsg = errorNode.path("error").asText();
                if (errorMsg != null && !errorMsg.isBlank()) {
                    throw new IOException("LibreTranslate API error: " + errorMsg);
                }
            } catch (Exception e) {
                // Ignore JSON parsing error
            }
            throw new IOException("LibreTranslate API error (code " + response.statusCode() + "): " + errorBody);
        }

        String translatedText = parseResponse(response.body());
        
        // If translation is null or empty, return original text
        if (translatedText == null || translatedText.isBlank()) {
            return text;
        }
        
        return translatedText;
    }

    /**
     * Builds the JSON request body for LibreTranslate API.
     */
    private String buildRequestBody(String text, String sourceLang, String targetLang) {
        return String.format("""
                {
                    "q": %s,
                    "source": "%s",
                    "target": "%s",
                    "format": "text"
                }
                """, escapeJson(text), sourceLang, targetLang);
    }

    /**
     * Parses the translation response from LibreTranslate API.
     */
    private String parseResponse(String responseBody) throws IOException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String translatedText = root.path("translatedText").asText();
            
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
     * Loads properties from application.properties.
     */
    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException e) {
            System.err.println("Cannot load application.properties: " + e.getMessage());
        }
        return props;
    }

    /**
     * Escapes text for JSON.
     */
    private static String escapeJson(String text) {
        if (text == null) {
            return "\"\"";
        }
        String escaped = text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }

    /**
     * Checks if the service is configured.
     */
    public boolean isConfigured() {
        return apiUrl != null && !apiUrl.isBlank();
    }
}
