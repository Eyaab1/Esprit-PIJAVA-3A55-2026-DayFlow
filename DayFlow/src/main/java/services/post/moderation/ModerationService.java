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
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

public class ModerationService {
    private static final Logger LOG = Logger.getLogger(ModerationService.class.getName());

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
    private final boolean failOpen;

    public ModerationService() {
        this(new UserService(), new ModerationLogService());
    }

    public ModerationService(UserService userService, ModerationLogService logService) {
        this.userService = userService;
        this.logService = logService;

        Properties properties = loadApplicationProperties();
        this.apiKey = readRequiredProperty(properties, "PERSPECTIVE_API_KEY");
        this.toxicityThreshold = readThreshold(properties);
        this.failOpen = readFailOpen(properties);
    }

    public double getToxicityThreshold() {
        return toxicityThreshold;
    }

    public void validatePostContent(Integer userId, String entityType, String title, String content) throws SQLException {
        User user = ensureUserCanInteract(userId);
        ModerationResult titleResult = analyzeText(title, "post_title");
        ModerationResult contentResult = analyzeText(content, "post_content");
        ModerationResult mergedResult = ModerationResult.merge("post", toxicityThreshold, titleResult, contentResult);

        if (mergedResult.isRejected()) {
            String exact = "Titre: " + normalizeForPreview(title) + "\nContenu: " + normalizeForPreview(content);
            logService.logRejectedAttempt(user, entityType, exact, previewPost(title, content), mergedResult);
            throw new ModerationRejectedException(mergedResult.getUserMessage(), mergedResult);
        }
    }

    public void validateCommentContent(Integer userId, String entityType, String content) throws SQLException {
        User user = ensureUserCanInteract(userId);
        ModerationResult result = analyzeText(content, "comment");
        if (result.isRejected()) {
            logService.logRejectedAttempt(user, entityType, normalizeForPreview(content), preview(content), result);
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
                String providerMessage = extractApiErrorMessage(response.body());
                String reason = "Le service de modération a refusé la requête (HTTP " + response.statusCode() + ").";
                if (!providerMessage.isBlank()) {
                    reason += " Détail: " + providerMessage;
                }
                return handleProviderFailure(source, reason, null);
            }
            return parseResponse(source, response.body());
        } catch (IOException e) {
            return handleProviderFailure(source, "Le service de modération est indisponible pour le moment.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return handleProviderFailure(source, "La vérification de modération a été interrompue.", e);
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

    private User ensureUserCanInteract(Integer userId) throws SQLException {
        User user = findUser(userId);
        if (user == null) {
            return null;
        }
        String status = user.getStatus() == null ? "" : user.getStatus().trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        if ("temp_banned".equals(status)) {
            LocalDateTime bannedUntil = user.getBannedUntil();
            if (bannedUntil == null || now.isBefore(bannedUntil)) {
                String untilText = bannedUntil != null ? (" jusqu'au " + bannedUntil) : "";
                throw new ModerationRejectedException(
                        "Votre compte est temporairement suspendu" + untilText + ".",
                        ModerationResult.fromScores("ban_status", toxicityThreshold, Map.of())
                );
            }
            userService.updateModerationStatus(user.getId(), "active", null, null);
            user.setStatus("active");
            user.setBannedUntil(null);
            user.setBanReason(null);
            return user;
        }

        if ("banned".equals(status) || "permanent_banned".equals(status)) {
            throw new ModerationRejectedException(
                    "Votre compte est banni définitivement.",
                    ModerationResult.fromScores("ban_status", toxicityThreshold, Map.of())
            );
        }
        return user;
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

    private static boolean readFailOpen(Properties properties) {
        String value = properties.getProperty("PERSPECTIVE_FAIL_OPEN", "true");
        return Boolean.parseBoolean(value.trim());
    }

    private ModerationResult handleProviderFailure(String source, String message, Exception cause) throws SQLException {
        if (!failOpen) {
            if (cause == null) {
                throw new SQLException(message);
            }
            throw new SQLException(message, cause);
        }
        if (cause == null) {
            LOG.warning("[Moderation] " + message + " -> fail-open active, message allowed.");
        } else {
            LOG.warning("[Moderation] " + message + " -> fail-open active, message allowed. cause=" + cause.getMessage());
        }
        return ModerationResult.fromScores(source, toxicityThreshold, Map.of());
    }

    private static String extractApiErrorMessage(String responseBody) {
        try {
            JsonNode root = JSON.readTree(responseBody);
            JsonNode msgNode = root.path("error").path("message");
            if (msgNode.isTextual()) {
                return msgNode.asText("").trim();
            }
            return "";
        } catch (Exception ignored) {
            return "";
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
