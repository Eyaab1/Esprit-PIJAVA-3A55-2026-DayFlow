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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Service for detecting toxic/harmful content using Google Perspective API.
 * Analyzes text for toxicity, threats, insults, profanity, etc.
 */
public class PerspectiveAPIService {

    private static final String PROPERTIES_FILE = "/application.properties";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    
    // Toxicity thresholds (0.0 to 1.0)
    private static final double TOXICITY_THRESHOLD = 0.7;
    private static final double SEVERE_TOXICITY_THRESHOLD = 0.5;
    private static final double THREAT_THRESHOLD = 0.7;
    private static final double INSULT_THRESHOLD = 0.7;
    private static final double PROFANITY_THRESHOLD = 0.7;

    private final String apiKey;
    private final String apiUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PerspectiveAPIService() {
        Properties props = loadProperties();
        this.apiKey = props.getProperty("perspective.api.key", "");
        this.apiUrl = "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Analyzes text for harmful content.
     *
     * @param text Text to analyze
     * @return ModerationResult with toxicity scores and decision
     * @throws IOException If API call fails
     */
    public ModerationResult analyzeText(String text) throws IOException, InterruptedException {
        if (text == null || text.isBlank()) {
            return new ModerationResult(false, 0.0, "Empty text");
        }

        if (!isConfigured()) {
            throw new IllegalStateException("Perspective API key not configured");
        }

        String requestBody = buildRequestBody(text);
        String urlWithKey = apiUrl + "?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlWithKey))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Perspective API error (code " + response.statusCode() + "): " + response.body());
        }

        return parseResponse(response.body());
    }

    /**
     * Builds the JSON request body for Perspective API.
     */
    private String buildRequestBody(String text) {
        return String.format("""
                {
                    "comment": {
                        "text": %s
                    },
                    "languages": ["fr", "en"],
                    "requestedAttributes": {
                        "TOXICITY": {},
                        "SEVERE_TOXICITY": {},
                        "IDENTITY_ATTACK": {},
                        "INSULT": {},
                        "PROFANITY": {},
                        "THREAT": {}
                    }
                }
                """, escapeJson(text));
    }

    /**
     * Parses the Perspective API response and determines if content is harmful.
     */
    private ModerationResult parseResponse(String responseBody) throws IOException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode attributes = root.path("attributeScores");

            Map<String, Double> scores = new HashMap<>();
            scores.put("TOXICITY", getScore(attributes, "TOXICITY"));
            scores.put("SEVERE_TOXICITY", getScore(attributes, "SEVERE_TOXICITY"));
            scores.put("IDENTITY_ATTACK", getScore(attributes, "IDENTITY_ATTACK"));
            scores.put("INSULT", getScore(attributes, "INSULT"));
            scores.put("PROFANITY", getScore(attributes, "PROFANITY"));
            scores.put("THREAT", getScore(attributes, "THREAT"));

            // Determine if content should be flagged
            boolean isToxic = scores.get("TOXICITY") >= TOXICITY_THRESHOLD;
            boolean isSevereToxic = scores.get("SEVERE_TOXICITY") >= SEVERE_TOXICITY_THRESHOLD;
            boolean hasThreat = scores.get("THREAT") >= THREAT_THRESHOLD;
            boolean hasInsult = scores.get("INSULT") >= INSULT_THRESHOLD;
            boolean hasProfanity = scores.get("PROFANITY") >= PROFANITY_THRESHOLD;

            boolean isHarmful = isToxic || isSevereToxic || hasThreat || hasInsult || hasProfanity;
            double maxScore = scores.values().stream().max(Double::compare).orElse(0.0);

            String reason = buildReason(scores);

            return new ModerationResult(isHarmful, maxScore, reason, scores);

        } catch (Exception e) {
            throw new IOException("Error parsing Perspective API response: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts score from attribute node.
     */
    private double getScore(JsonNode attributes, String attributeName) {
        return attributes.path(attributeName)
                .path("summaryScore")
                .path("value")
                .asDouble(0.0);
    }

    /**
     * Builds a human-readable reason for flagging.
     */
    private String buildReason(Map<String, Double> scores) {
        StringBuilder reason = new StringBuilder();
        
        if (scores.get("TOXICITY") >= TOXICITY_THRESHOLD) {
            reason.append("Contenu toxique détecté. ");
        }
        if (scores.get("SEVERE_TOXICITY") >= SEVERE_TOXICITY_THRESHOLD) {
            reason.append("Toxicité sévère détectée. ");
        }
        if (scores.get("THREAT") >= THREAT_THRESHOLD) {
            reason.append("Menaces détectées. ");
        }
        if (scores.get("INSULT") >= INSULT_THRESHOLD) {
            reason.append("Insultes détectées. ");
        }
        if (scores.get("PROFANITY") >= PROFANITY_THRESHOLD) {
            reason.append("Langage inapproprié détecté. ");
        }

        return reason.length() > 0 ? reason.toString().trim() : "Contenu approprié";
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
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Result of content moderation analysis.
     */
    public static class ModerationResult {
        private final boolean harmful;
        private final double maxScore;
        private final String reason;
        private final Map<String, Double> scores;

        public ModerationResult(boolean harmful, double maxScore, String reason) {
            this(harmful, maxScore, reason, new HashMap<>());
        }

        public ModerationResult(boolean harmful, double maxScore, String reason, Map<String, Double> scores) {
            this.harmful = harmful;
            this.maxScore = maxScore;
            this.reason = reason;
            this.scores = scores;
        }

        public boolean isHarmful() {
            return harmful;
        }

        public double getMaxScore() {
            return maxScore;
        }

        public String getReason() {
            return reason;
        }

        public Map<String, Double> getScores() {
            return scores;
        }

        @Override
        public String toString() {
            return String.format("ModerationResult{harmful=%s, maxScore=%.2f, reason='%s'}", 
                    harmful, maxScore, reason);
        }
    }
}
