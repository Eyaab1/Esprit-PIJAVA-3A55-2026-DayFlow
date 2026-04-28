package services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.reclamation.Reclamation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

/**
 * Service pour générer des suggestions de réponses aux réclamations via Groq AI.
 */
public class GroqAIService {

    private static final String PROPERTIES_FILE = "/application.properties";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final int maxTokens;
    private final double temperature;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GroqAIService() {
        Properties props = loadProperties();
        this.apiKey = props.getProperty("groq.api.key", "");
        this.apiUrl = props.getProperty("groq.api.url", "https://api.groq.com/openai/v1/chat/completions");
        this.model = props.getProperty("groq.model", "llama-3.1-70b-versatile");
        this.maxTokens = Integer.parseInt(props.getProperty("groq.max.tokens", "500"));
        this.temperature = Double.parseDouble(props.getProperty("groq.temperature", "0.7"));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Génère une suggestion de réponse pour une réclamation donnée.
     *
     * @param reclamation La réclamation à traiter
     * @return La réponse suggérée en français
     * @throws IOException Si une erreur réseau ou API se produit
     */
    public String generateResponseSuggestion(Reclamation reclamation) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Clé API Groq non configurée. Vérifiez application.properties");
        }

        String prompt = buildPrompt(reclamation);
        String requestBody = buildRequestBody(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Erreur API Groq (code " + response.statusCode() + "): " + response.body());
        }

        return parseResponse(response.body());
    }

    /**
     * Construit le prompt pour l'IA en fonction de la réclamation.
     */
    private String buildPrompt(Reclamation reclamation) {
        String type = reclamation.getType() != null ? reclamation.getType().value : "Autre";
        String content = reclamation.getContent() != null ? stripHtml(reclamation.getContent()) : "";

        return String.format("""
                Tu es un assistant de support client professionnel et empathique pour l'application DayFlow.
                
                Une réclamation a été soumise par un utilisateur :
                
                Type de réclamation : %s
                Contenu : %s
                
                Génère une réponse professionnelle, empathique et utile en français pour répondre à cette réclamation.
                La réponse doit :
                - Être courtoise et empathique
                - Reconnaître le problème de l'utilisateur
                - Proposer une solution ou des étapes à suivre
                - Être concise (maximum 200 mots)
                - Utiliser un ton professionnel mais chaleureux
                
                Réponse :
                """, type, content);
    }

    /**
     * Construit le corps de la requête JSON pour l'API Groq.
     */
    private String buildRequestBody(String prompt) {
        return String.format("""
                {
                    "model": "%s",
                    "messages": [
                        {
                            "role": "user",
                            "content": %s
                        }
                    ],
                    "temperature": %.1f,
                    "max_tokens": %d
                }
                """, model, escapeJson(prompt), temperature, maxTokens);
    }

    /**
     * Parse la réponse JSON de l'API Groq pour extraire le texte généré.
     */
    private String parseResponse(String responseBody) throws IOException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                String content = message.path("content").asText();
                return content.trim();
            }
            throw new IOException("Format de réponse inattendu de l'API Groq");
        } catch (Exception e) {
            throw new IOException("Erreur lors du parsing de la réponse: " + e.getMessage(), e);
        }
    }

    /**
     * Charge les propriétés depuis application.properties.
     */
    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException e) {
            System.err.println("Impossible de charger application.properties: " + e.getMessage());
        }
        return props;
    }

    /**
     * Supprime les balises HTML du contenu.
     */
    private static String stripHtml(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw
                .replaceAll("(?i)<\\s*br\\s*/?>", "\n")
                .replaceAll("(?i)</p>\\s*", "\n")
                .replaceAll("(?i)<\\s*p[^>]*>", "");
        s = s.replaceAll("<[^>]+>", "");
        s = s.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"");
        return s.trim();
    }

    /**
     * Échappe les caractères spéciaux pour JSON.
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
     * Vérifie si le service est correctement configuré.
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
