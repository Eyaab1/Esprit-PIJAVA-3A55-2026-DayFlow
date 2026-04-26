package services.post.moderation;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.user.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModerationLogService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path LOG_PATH = Path.of("logs", "moderation-rejections.jsonl");

    public void logRejectedAttempt(User user, String entityType, String contentPreview, ModerationResult result) {
        if (result == null || !result.isRejected()) {
            return;
        }

        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("timestamp", LocalDateTime.now().toString());
        logEntry.put("userId", user != null ? user.getId() : null);
        logEntry.put("userEmail", user != null ? user.getEmail() : null);
        logEntry.put("entityType", entityType);
        logEntry.put("contentPreview", contentPreview);
        logEntry.put("toxicityScores", result.getToxicityScores());
        logEntry.put("flaggedAttributes", result.getFlaggedAttributes());
        logEntry.put("highestScore", result.getHighestScore());
        logEntry.put("highestAttribute", result.getHighestAttribute());
        logEntry.put("thresholdUsed", result.getThreshold());
        logEntry.put("source", result.getSource());
        logEntry.put("analyzedAt", result.getAnalyzedAt().toString());

        try {
            Files.createDirectories(LOG_PATH.getParent());
            String line = JSON.writeValueAsString(logEntry) + System.lineSeparator();
            Files.writeString(LOG_PATH, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Unable to write moderation log: " + e.getMessage());
        }
    }
}
