package services.interaction.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class OpenAiSemanticClient {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String OPENAI_EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";
    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_EMBEDDING_MODEL = "text-embedding-3-small";

    private final String provider;
    private final String apiKey;
    private final String chatModel;
    private final String ollamaBaseUrl;
    private final String ollamaEmbeddingModel;
    private final String ollamaChatModel;

    public OpenAiSemanticClient() {
        Properties properties = loadApplicationProperties();
        this.provider = properties.getProperty("app.ai.provider", "openai").trim().toLowerCase();
        this.apiKey = properties.getProperty("app.ai.openai.apiKey", "").trim();
        this.chatModel = properties.getProperty("app.ai.openai.model", "gpt-4o-mini").trim();
        this.ollamaBaseUrl = properties.getProperty("app.ai.ollama.baseUrl", "http://localhost:11434").trim();
        this.ollamaEmbeddingModel = properties.getProperty("app.ai.ollama.embeddingModel", "nomic-embed-text").trim();
        this.ollamaChatModel = properties.getProperty("app.ai.ollama.chatModel", "llama3.1:8b").trim();
    }

    public List<Double> embedding(String text) throws SQLException {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            return List.of();
        }
        if ("ollama".equals(provider)) {
            return ollamaEmbedding(normalized);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", OPENAI_EMBEDDING_MODEL);
        payload.put("input", normalized);

        JsonNode root = postOpenAiJson(OPENAI_EMBEDDINGS_URL, payload);
        JsonNode data = root.path("data");
        if (!data.isArray() || data.isEmpty()) {
            throw new SQLException("Embedding response missing data.");
        }
        JsonNode vectorNode = data.get(0).path("embedding");
        if (!vectorNode.isArray() || vectorNode.isEmpty()) {
            throw new SQLException("Embedding vector missing in response.");
        }
        List<Double> vector = new ArrayList<>(vectorNode.size());
        for (JsonNode n : vectorNode) {
            vector.add(n.asDouble());
        }
        return vector;
    }

    public List<String> suggestTagCandidates(String title, String content, int maxTags) throws SQLException {
        if ("ollama".equals(provider)) {
            return ollamaTagSuggestions(title, content, maxTags);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", chatModel);
        payload.put("temperature", 0.2);
        payload.put("response_format", Map.of("type", "json_object"));
        payload.put("messages", List.of(
                Map.of("role", "system", "content",
                        "You are a domain tag generator for wellbeing/productivity posts. " +
                                "Return concise professional concept tags only. " +
                                "Never return generic adjectives or filler words."),
                Map.of("role", "user", "content",
                        "Generate 1 or 2 high-quality semantic tags for the following post.\n" +
                                "Use title + content meaning, not surface keywords.\n" +
                                "Output JSON: {\"tags\":[\"Tag 1\",\"Tag 2\"]}\n" +
                                "Constraints:\n" +
                                "- 1 to " + Math.max(1, maxTags) + " tags only\n" +
                                "- each tag 1-3 words\n" +
                                "- professional concepts (e.g. Stress Management, Time Management, Burnout)\n" +
                                "- no generic words like best/good/help/what/less\n\n" +
                                "Title: " + (title == null ? "" : title.trim()) + "\n" +
                                "Content: " + (content == null ? "" : content.trim()))
        ));

        JsonNode root = postOpenAiJson(OPENAI_CHAT_URL, payload);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.isNull() || contentNode.asText().isBlank()) {
            throw new SQLException("Tag suggestion response is empty.");
        }

        try {
            JsonNode parsed = JSON.readTree(contentNode.asText());
            JsonNode tagsNode = parsed.path("tags");
            List<String> tags = new ArrayList<>();
            if (tagsNode.isArray()) {
                for (JsonNode n : tagsNode) {
                    String tag = n.asText("").trim();
                    if (!tag.isEmpty()) {
                        tags.add(tag);
                    }
                }
            }
            return tags;
        } catch (IOException e) {
            throw new SQLException("Unable to parse tag suggestion JSON.", e);
        }
    }

    private JsonNode postOpenAiJson(String url, Map<String, Object> payload) throws SQLException {
        if (apiKey.isBlank()) {
            throw new SQLException("OpenAI API key is missing.");
        }
        try {
            String body = JSON.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(25))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new SQLException("OpenAI API error (HTTP " + response.statusCode() + "): " + response.body());
            }
            JsonNode root = JSON.readTree(response.body());
            JsonNode errorNode = root.path("error");
            if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                throw new SQLException("OpenAI API error: " + errorNode.path("message").asText("Unknown error"));
            }
            return root;
        } catch (IOException e) {
            throw new SQLException("OpenAI API unavailable.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("OpenAI API call interrupted.", e);
        }
    }

    private List<Double> ollamaEmbedding(String text) throws SQLException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", ollamaEmbeddingModel);
        payload.put("prompt", text);
        JsonNode root = postOllamaJson("/api/embeddings", payload);
        JsonNode embeddingNode = root.path("embedding");
        if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
            throw new SQLException("Ollama embedding response missing vector.");
        }
        List<Double> vector = new ArrayList<>(embeddingNode.size());
        for (JsonNode n : embeddingNode) {
            vector.add(n.asDouble());
        }
        return vector;
    }

    private List<String> ollamaTagSuggestions(String title, String content, int maxTags) throws SQLException {
        String prompt = "Generate 1 or 2 professional semantic tags for this post.\n" +
                "Return STRICT JSON only in this format: {\"tags\":[\"Tag 1\",\"Tag 2\"]}\n" +
                "Constraints:\n" +
                "- 1 to " + Math.max(1, maxTags) + " tags\n" +
                "- each tag 1-3 words\n" +
                "- meaningful domain concepts\n" +
                "- no generic words like best/good/help/what/less\n\n" +
                "Title: " + (title == null ? "" : title.trim()) + "\n" +
                "Content: " + (content == null ? "" : content.trim());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", ollamaChatModel);
        payload.put("prompt", prompt);
        payload.put("stream", false);
        payload.put("format", "json");

        JsonNode root = postOllamaJson("/api/generate", payload);
        JsonNode responseNode = root.path("response");
        if (!responseNode.isTextual() || responseNode.asText().isBlank()) {
            throw new SQLException("Ollama tag suggestion response is empty.");
        }
        try {
            JsonNode parsed = JSON.readTree(responseNode.asText());
            JsonNode tagsNode = parsed.path("tags");
            List<String> tags = new ArrayList<>();
            if (tagsNode.isArray()) {
                for (JsonNode n : tagsNode) {
                    String tag = n.asText("").trim();
                    if (!tag.isEmpty()) {
                        tags.add(tag);
                    }
                }
            }
            return tags;
        } catch (IOException e) {
            throw new SQLException("Unable to parse Ollama tag JSON.", e);
        }
    }

    private JsonNode postOllamaJson(String path, Map<String, Object> payload) throws SQLException {
        String base = ollamaBaseUrl.endsWith("/") ? ollamaBaseUrl.substring(0, ollamaBaseUrl.length() - 1) : ollamaBaseUrl;
        String url = base + path;
        try {
            String body = JSON.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new SQLException("Ollama API error (HTTP " + response.statusCode() + "): " + response.body());
            }
            return JSON.readTree(response.body());
        } catch (IOException e) {
            throw new SQLException("Ollama unavailable. Ensure Ollama is installed and running on " + ollamaBaseUrl, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Ollama call interrupted.", e);
        }
    }

    private static Properties loadApplicationProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = OpenAiSemanticClient.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (inputStream == null) {
                throw new IllegalStateException("application.properties introuvable.");
            }
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger application.properties.", e);
        }
    }

}
