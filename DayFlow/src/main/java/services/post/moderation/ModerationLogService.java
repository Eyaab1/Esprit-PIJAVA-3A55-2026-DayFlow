package services.post.moderation;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.user.User;
import utils.DbConnexion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModerationLogService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path LOG_PATH = Path.of("logs", "moderation-rejections.jsonl");
    private static final String INSERT_INCIDENT = """
            INSERT INTO moderation_incident (
                created_at, user_id, user_email, entity_type, content_text, content_preview,
                detected_reason, flagged_attributes, highest_attribute, highest_score, threshold_used, source,
                warning_status, incident_status, account_status_snapshot
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    public void logRejectedAttempt(User user, String entityType, String contentText, String contentPreview, ModerationResult result) {
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
        logEntry.put("status", "NOT_VIEWED"); // Default status

        try {
            Files.createDirectories(LOG_PATH.getParent());
            String line = JSON.writeValueAsString(logEntry) + System.lineSeparator();
            Files.writeString(LOG_PATH, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Unable to write moderation log: " + e.getMessage());
        }

        try {
            insertIncident(user, entityType, contentText, contentPreview, result);
        } catch (SQLException e) {
            System.err.println("Unable to write moderation incident in DB: " + e.getMessage());
        }
    }

    private void insertIncident(User user, String entityType, String contentText, String contentPreview, ModerationResult result) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT_INCIDENT)) {
            int i = 1;
            ps.setTimestamp(i++, Timestamp.valueOf(LocalDateTime.now()));
            if (user == null || user.getId() == null) {
                ps.setNull(i++, java.sql.Types.INTEGER);
            } else {
                ps.setInt(i++, user.getId());
            }
            ps.setString(i++, user != null ? user.getEmail() : null);
            ps.setString(i++, entityType);
            ps.setString(i++, contentText);
            ps.setString(i++, contentPreview);
            ps.setString(i++, result.getUserMessage());
            ps.setString(i++, String.join(", ", result.getFlaggedAttributes()));
            ps.setString(i++, result.getHighestAttribute());
            ps.setDouble(i++, result.getHighestScore());
            ps.setDouble(i++, result.getThreshold());
            ps.setString(i++, result.getSource());
            ps.setString(i++, "WARNED");
            ps.setString(i++, "NEW");
            ps.setString(i, user != null ? user.getStatus() : "unknown");
            ps.executeUpdate();
        }
    }
}
