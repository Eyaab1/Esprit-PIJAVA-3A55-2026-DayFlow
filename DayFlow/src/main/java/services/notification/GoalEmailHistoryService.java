package services.notification;

import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Tracks email reminder history for goals to prevent duplicates.
 */
public class GoalEmailHistoryService {

    private final Connection cnx;

    public GoalEmailHistoryService() {
        this.cnx = DbConnexion.getConnection();
    }

    public boolean hasEmailBeenSent(int goalId, int userId, EmailService.DeadlineReminderType reminderType,
                                    LocalDateTime deadlineSnapshot) throws SQLException {
        String sql = """
                SELECT 1
                FROM goal_email_notification_history
                WHERE goal_id = ?
                  AND user_id = ?
                  AND reminder_type = ?
                  AND deadline_snapshot = ?
                  AND email_sent = TRUE
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            ps.setInt(2, userId);
            ps.setString(3, reminderType.name());
            ps.setTimestamp(4, Timestamp.valueOf(deadlineSnapshot));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void logEmailAttempt(int goalId, int userId, EmailService.DeadlineReminderType reminderType,
                                LocalDateTime deadlineSnapshot, boolean emailSent, String errorMessage) throws SQLException {
        String sql = """
                INSERT INTO goal_email_notification_history (
                    goal_id, user_id, reminder_type, deadline_snapshot,
                    last_reminder_sent, email_sent, error_message, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (goal_id, user_id, reminder_type, deadline_snapshot)
                DO UPDATE SET
                    last_reminder_sent = EXCLUDED.last_reminder_sent,
                    email_sent = EXCLUDED.email_sent,
                    error_message = EXCLUDED.error_message
                """;
        LocalDateTime now = LocalDateTime.now();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            ps.setInt(2, userId);
            ps.setString(3, reminderType.name());
            ps.setTimestamp(4, Timestamp.valueOf(deadlineSnapshot));
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.setBoolean(6, emailSent);
            ps.setString(7, errorMessage);
            ps.setTimestamp(8, Timestamp.valueOf(now));
            ps.executeUpdate();
        }
    }

    public void deletePendingByGoal(int goalId) throws SQLException {
        String sql = """
                DELETE FROM goal_email_notification_history
                WHERE goal_id = ?
                  AND reminder_type IN ('DEADLINE_7D', 'DEADLINE_3D', 'DEADLINE_24H', 'CUSTOM_EXACT')
                  AND email_sent = FALSE
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            ps.executeUpdate();
        }
    }
}
