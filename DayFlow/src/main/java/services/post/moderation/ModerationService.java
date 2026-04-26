package services.post.moderation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.user.User;
import services.account.UserService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class ModerationService {

    private static final String API_URL = "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze";
    private static final List<String> REQUEST_ATTRIBUTES = List.of(
            "TOXICITY",
            "SEVERE_TOXICITY",
            "INSULT",
            "PROFANITY",
            "THREAT",
            "IDENTITY_ATTACK"
    );

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final UserService userService;
    private final ModerationLogService logService;
    private final String apiKey;
    private final double toxicityThreshold;

    public ModerationService() {
        this(new UserService(), new ModerationLogService());
    }

    public ModerationService(UserService userService, ModerationLogService logService) {
        this.userService = userService;
        this.logService = logService;

        Properties properties = loadApplicationProperties();
        this.apiKey = readRequiredProperty(properties, "PERSPECTIVE_API_KEY");
        this.toxicityThreshold = readThreshold(properties);
    }

    public double getToxicityThreshold() {
        return toxicityThreshold;
    }

    public void validatePostContent(Integer userId, String entityType, String title, String content) throws SQLException {
        ModerationResult titleResult = analyzeText(title, "post_title");
        ModerationResult contentResult = analyzeText(content, "post_content");
        ModerationResult mergedResult = ModerationResult.merge("post", toxicityThreshold, titleResult, contentResult);

        if (mergedResult.isRejected()) {
            User user = findUser(userId);
            logService.logRejectedAttempt(user, entityType, previewPost(title, content), mergedResult);
            throw new ModerationRejectedException(mergedResult.getUserMessage(), mergedResult);
        }
    }

    public void validateCommentContent(Integer userId, String entityType, String content) throws SQLException {
        ModerationResult result = analyzeText(content, "comment");
        if (result.isRejected()) {
            User user = findUser(userId);
            logService.logRejectedAttempt(user, entityType, preview(content), result);
            throw new ModerationRejectedException(result.getUserMessage(), result);
        }
    }

    private ModerationResult analyzeText(String text, String source) throws SQLException {
        String normalizedText = text != null ? text.trim() : "";
        if (normalizedText.isEmpty()) {
            return ModerationResult.fromScores(source, toxicityThreshold, Map.of());
        }

        HttpRequest request = buildRequest(normalizedText);
        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new SQLException("Le service de modération a refusé la requête (HTTP " + response.statusCode() + ").");
            }
            return parseResponse(source, response.body());
        } catch (IOException e) {
            throw new SQLException("Le service de modération est indisponible pour le moment.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("La vérification de modération a été interrompue.", e);
        }
    }

    private HttpRequest buildRequest(String text) throws SQLException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("comment", Map.of("text", text));
        payload.put("languages", List.of("fr", "en"));
        payload.put("doNotStore", true);

        Map<String, Object> attributes = new LinkedHashMap<>();
        for (String attribute : REQUEST_ATTRIBUTES) {
            attributes.put(attribute, Map.of());
        }
        payload.put("requestedAttributes", attributes);

        try {
            String requestBody = JSON.writeValueAsString(payload);
            return HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "?key=" + apiKey))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();
        } catch (IOException e) {
            throw new SQLException("Impossible de préparer la requête de modération.", e);
        }
    }

    private ModerationResult parseResponse(String source, String responseBody) throws SQLException {
        try {
            JsonNode root = JSON.readTree(responseBody);
            JsonNode errorNode = root.path("error");
            if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                String errorMessage = errorNode.path("message").asText("Erreur inconnue du service de modération.");
                throw new SQLException("Le service de modération a retourné une erreur : " + errorMessage);
            }
            JsonNode scoresNode = root.path("attributeScores");
            Map<String, Double> scores = new LinkedHashMap<>();

            for (String attribute : REQUEST_ATTRIBUTES) {
                JsonNode valueNode = scoresNode.path(attribute).path("summaryScore").path("value");
                if (valueNode.isNumber()) {
                    scores.put(attribute, valueNode.asDouble());
                }
            }

            if (scores.isEmpty()) {
                throw new SQLException("Aucun score de modération n'a été retourné par le service.");
            }

            return ModerationResult.fromScores(source, toxicityThreshold, scores);
        } catch (IOException e) {
            throw new SQLException("Réponse de modération invalide.", e);
        }
    }

    private User findUser(Integer userId) {
        if (userId == null) {
            return null;
        }
        try {
            Optional<User> found = userService.findById(userId);
            return found.orElse(null);
        } catch (SQLException e) {
            return null;
        }
    }

    private static Properties loadApplicationProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = ModerationService.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (inputStream == null) {
                throw new IllegalStateException("application.properties introuvable.");
            }
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger application.properties.", e);
        }
    }

    private static String readRequiredProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("La propriété " + key + " est obligatoire.");
        }
        return value.trim();
    }

    private static double readThreshold(Properties properties) {
        String value = properties.getProperty("PERSPECTIVE_TOXICITY_THRESHOLD", "0.7");
        try {
            double threshold = Double.parseDouble(value.trim());
            if (threshold < 0.0d || threshold > 1.0d) {
                throw new IllegalStateException("PERSPECTIVE_TOXICITY_THRESHOLD doit être compris entre 0 et 1.");
            }
            return threshold;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("PERSPECTIVE_TOXICITY_THRESHOLD invalide: " + value, e);
        }
    }

    private static String previewPost(String title, String content) {
        return preview("Titre: " + normalizeForPreview(title) + " | Contenu: " + normalizeForPreview(content));
    }

    private static String preview(String text) {
        String normalized = normalizeForPreview(text);
        return normalized.length() <= 220 ? normalized : normalized.substring(0, 220) + "...";
    }

    private static String normalizeForPreview(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }
}
