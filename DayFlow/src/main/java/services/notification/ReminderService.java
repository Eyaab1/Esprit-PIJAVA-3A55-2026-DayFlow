package services.notification;

import model.notification.ReminderLog;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for managing reminder logs.
 * Prevents duplicate reminders and tracks sent reminders.
 */
public class ReminderService implements CRUD<ReminderLog, Integer> {

    private final Connection cnx;

    public ReminderService() {
        cnx = DbConnexion.getConnection();
    }

    @Override
    public void create(ReminderLog reminderLog) throws SQLException {
        insert(reminderLog);
    }

    @Override
    public void insert(ReminderLog reminderLog) throws SQLException {
        String sql = """
                INSERT INTO reminder_log (user_id, entity_type, entity_id, reminder_type, deadline, sent_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, reminderLog.getUserId());
            ps.setString(2, reminderLog.getEntityType());
            ps.setInt(3, reminderLog.getEntityId());
            ps.setString(4, reminderLog.getReminderType().name());
            ps.setTimestamp(5, Timestamp.valueOf(reminderLog.getDeadline()));
            ps.setTimestamp(6, Timestamp.valueOf(reminderLog.getSentAt()));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    reminderLog.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(ReminderLog reminderLog) throws SQLException {
        String sql = """
                UPDATE reminder_log 
                SET user_id = ?, entity_type = ?, entity_id = ?, reminder_type = ?, deadline = ?, sent_at = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reminderLog.getUserId());
            ps.setString(2, reminderLog.getEntityType());
            ps.setInt(3, reminderLog.getEntityId());
            ps.setString(4, reminderLog.getReminderType().name());
            ps.setTimestamp(5, Timestamp.valueOf(reminderLog.getDeadline()));
            ps.setTimestamp(6, Timestamp.valueOf(reminderLog.getSentAt()));
            ps.setInt(7, reminderLog.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM reminder_log WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Optional<ReminderLog> findById(Integer id) throws SQLException {
        String sql = """
                SELECT id, user_id, entity_type, entity_id, reminder_type, deadline, sent_at
                FROM reminder_log WHERE id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<ReminderLog> getAll() throws SQLException {
        String sql = """
                SELECT id, user_id, entity_type, entity_id, reminder_type, deadline, sent_at
                FROM reminder_log ORDER BY sent_at DESC
                """;

        List<ReminderLog> reminders = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reminders.add(mapRow(rs));
            }
        }
        return reminders;
    }

    /**
     * Check if a reminder has already been sent for this entity
     */
    public boolean hasReminderBeenSent(int userId, String entityType, int entityId, 
                                       ReminderLog.ReminderType reminderType, LocalDateTime deadline) throws SQLException {
        String sql = """
                SELECT 1 FROM reminder_log 
                WHERE user_id = ? AND entity_type = ? AND entity_id = ? 
                  AND reminder_type = ? AND deadline = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, entityType);
            ps.setInt(3, entityId);
            ps.setString(4, reminderType.name());
            ps.setTimestamp(5, Timestamp.valueOf(deadline));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Get all reminders for a specific entity
     */
    public List<ReminderLog> getByEntity(String entityType, int entityId) throws SQLException {
        String sql = """
                SELECT id, user_id, entity_type, entity_id, reminder_type, deadline, sent_at
                FROM reminder_log 
                WHERE entity_type = ? AND entity_id = ?
                ORDER BY sent_at DESC
                """;

        List<ReminderLog> reminders = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, entityType);
            ps.setInt(2, entityId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reminders.add(mapRow(rs));
                }
            }
        }
        return reminders;
    }

    /**
     * Delete reminders for an entity (when deadline is updated or entity is deleted)
     */
    public int deleteByEntity(String entityType, int entityId) throws SQLException {
        String sql = "DELETE FROM reminder_log WHERE entity_type = ? AND entity_id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, entityType);
            ps.setInt(2, entityId);
            return ps.executeUpdate();
        }
    }

    /**
     * Map ResultSet row to ReminderLog object
     */
    private static ReminderLog mapRow(ResultSet rs) throws SQLException {
        ReminderLog reminderLog = new ReminderLog();
        reminderLog.setId(rs.getInt("id"));
        reminderLog.setUserId(rs.getInt("user_id"));
        reminderLog.setEntityType(rs.getString("entity_type"));
        reminderLog.setEntityId(rs.getInt("entity_id"));
        reminderLog.setReminderType(ReminderLog.ReminderType.valueOf(rs.getString("reminder_type")));

        Timestamp deadline = rs.getTimestamp("deadline");
        if (deadline != null) {
            reminderLog.setDeadline(deadline.toLocalDateTime());
        }

        Timestamp sentAt = rs.getTimestamp("sent_at");
        if (sentAt != null) {
            reminderLog.setSentAt(sentAt.toLocalDateTime());
        }

        return reminderLog;
    }
}
