package services.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModerationLogFileService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path LOG_PATH = Path.of("logs", "moderation-rejections.jsonl");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public record ModerationLogEntry(
            String timestamp,
            Integer userId,
            String userEmail,
            String entityType,
            String contentPreview,
            Map<String, Double> toxicityScores,
            List<String> flaggedAttributes,
            Double highestScore,
            String highestAttribute,
            Double thresholdUsed,
            String source,
            String analyzedAt
    ) {
        public LocalDateTime getTimestampAsDateTime() {
            try {
                return LocalDateTime.parse(timestamp, FORMATTER);
            } catch (Exception e) {
                return null;
            }
        }
    }

    public List<ModerationLogEntry> readRecentLogs(int limit) throws IOException {
        if (!Files.exists(LOG_PATH)) {
            return Collections.emptyList();
        }

        List<String> lines = Files.readAllLines(LOG_PATH);
        List<ModerationLogEntry> entries = new ArrayList<>();

        // Read from end to beginning (newest first)
        for (int i = lines.size() - 1; i >= 0 && entries.size() < limit; i--) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }

            try {
                ModerationLogEntry entry = parseLogLine(line);
                entries.add(entry);
            } catch (Exception e) {
                System.err.println("Failed to parse log line: " + e.getMessage());
            }
        }

        return entries;
    }

    private ModerationLogEntry parseLogLine(String line) throws IOException {
        JsonNode root = JSON.readTree(line);

        String timestamp = root.path("timestamp").asText(null);
        Integer userId = root.path("userId").isNull() ? null : root.path("userId").asInt();
        String userEmail = root.path("userEmail").asText(null);
        String entityType = root.path("entityType").asText(null);
        String contentPreview = root.path("contentPreview").asText(null);
        Double highestScore = root.path("highestScore").isNull() ? null : root.path("highestScore").asDouble();
        String highestAttribute = root.path("highestAttribute").asText(null);
        Double thresholdUsed = root.path("thresholdUsed").isNull() ? null : root.path("thresholdUsed").asDouble();
        String source = root.path("source").asText(null);
        String analyzedAt = root.path("analyzedAt").asText(null);

        // Parse toxicity scores
        Map<String, Double> toxicityScores = new LinkedHashMap<>();
        JsonNode scoresNode = root.path("toxicityScores");
        if (scoresNode.isObject()) {
            scoresNode.fields().forEachRemaining(entry -> {
                toxicityScores.put(entry.getKey(), entry.getValue().asDouble());
            });
        }

        // Parse flagged attributes
        List<String> flaggedAttributes = new ArrayList<>();
        JsonNode flagsNode = root.path("flaggedAttributes");
        if (flagsNode.isArray()) {
            flagsNode.forEach(node -> flaggedAttributes.add(node.asText()));
        }

        return new ModerationLogEntry(
                timestamp,
                userId,
                userEmail,
                entityType,
                contentPreview,
                toxicityScores,
                flaggedAttributes,
                highestScore,
                highestAttribute,
                thresholdUsed,
                source,
                analyzedAt
        );
    }
}
