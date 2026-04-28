package services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import config.AppConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Service IA de motivation — répond aux questions sur la motivation,
 * la concentration et la procrastination via OpenAI ou Ollama.
 */
public class MotivationAIService {

    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";

    private static final String SYSTEM_PROMPT = """
            Tu es Moti, un assistant IA bienveillant et motivant intégré dans l'application DayFlow.
            Ton rôle est d'aider l'utilisateur à :
            - Rester motivé et positif dans ses objectifs personnels
            - Améliorer sa concentration et sa productivité
            - Surmonter la procrastination avec des techniques concrètes
            - Gérer son stress et son énergie au quotidien

            Règles importantes :
            - Réponds toujours en français, de façon chaleureuse et encourageante
            - Sois concis (3-5 phrases max par réponse)
            - Propose des conseils pratiques et actionnables
            - Utilise des emojis avec modération pour rendre la conversation vivante
            - Si l'utilisateur semble découragé, commence par valider son ressenti avant de proposer des solutions
            - Ne parle que de motivation, productivité, concentration, bien-être et développement personnel
            - Si la question est hors sujet, redirige gentiment vers ces thèmes
            """;

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http;
    private final String provider;
    private final String openAiKey;
    private final String openAiModel;
    private final String ollamaBaseUrl;
    private final String ollamaModel;

    public MotivationAIService() {
        Properties props = loadProperties();
        this.provider = props.getProperty("app.ai.provider", "openai").trim().toLowerCase();
        this.openAiKey = props.getProperty("app.ai.openai.apiKey", "").trim();
        this.openAiModel = props.getProperty("app.ai.openai.model", "gpt-4o-mini").trim();
        this.ollamaBaseUrl = props.getProperty("app.ai.ollama.baseUrl", "http://localhost:11434").trim();
        this.ollamaModel = props.getProperty("app.ai.ollama.chatModel", "phi3:mini").trim();
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /**
     * Envoie un message avec l'historique de conversation et retourne la réponse de Moti.
     *
     * @param history liste de messages [{role, content}, ...]
     * @param userMessage nouveau message de l'utilisateur
     * @return réponse de l'assistant
     */
    public String chat(List<Map<String, String>> history, String userMessage) throws Exception {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.addAll(history);
        messages.add(Map.of("role", "user", "content", userMessage));

        if ("ollama".equals(provider)) {
            return chatOllama(messages);
        }
        return chatOpenAI(messages);
    }

    private String chatOpenAI(List<Map<String, String>> messages) throws Exception {
        if (openAiKey.isBlank()) {
            throw new IllegalStateException("Clé OpenAI manquante dans application.properties");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", openAiModel);
        payload.put("messages", messages);
        payload.put("temperature", 0.8);
        payload.put("max_tokens", 300);

        String body = json.writeValueAsString(payload);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_CHAT_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openAiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IllegalStateException("OpenAI error " + resp.statusCode() + ": " + resp.body());
        }
        JsonNode root = json.readTree(resp.body());
        return root.path("choices").path(0).path("message").path("content").asText("").trim();
    }

    private String chatOllama(List<Map<String, String>> messages) throws Exception {
        // Ollama /api/chat endpoint
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", ollamaModel);
        payload.put("messages", messages);
        payload.put("stream", false);

        String base = ollamaBaseUrl.endsWith("/") ? ollamaBaseUrl.substring(0, ollamaBaseUrl.length() - 1) : ollamaBaseUrl;
        String body = json.writeValueAsString(payload);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base + "/api/chat"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IllegalStateException("Ollama error " + resp.statusCode());
        }
        JsonNode root = json.readTree(resp.body());
        return root.path("message").path("content").asText("").trim();
    }

    private static Properties loadProperties() {
        Properties p = new Properties();
        try (InputStream is = MotivationAIService.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) p.load(is);
        } catch (IOException ignored) {}
        return p;
    }
}
