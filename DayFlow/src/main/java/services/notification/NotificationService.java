package services.notification;

import model.notification.Notification;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for managing notifications.
 * Handles CRUD operations and notification queries.
 */
public class NotificationService implements CRUD<Notification, Integer> {

    private final Connection cnx;

    public NotificationService() {
        cnx = DbConnexion.getConnection();
    }

    @Override
    public void create(Notification notification) throws SQLException {
        insert(notification);
    }

    @Override
    public void insert(Notification notification) throws SQLException {
        String sql = """
                INSERT INTO notification (user_id, type, entity_type, entity_id, title, message, is_read, action_url, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, notification.getUserId());
            ps.setString(2, notification.getType().name());
            ps.setString(3, notification.getEntityType().name().toLowerCase());
            ps.setInt(4, notification.getEntityId());
            ps.setString(5, notification.getTitle());
            ps.setString(6, notification.getMessage());
            ps.setBoolean(7, notification.isRead());
            ps.setString(8, notification.getActionUrl());
            ps.setTimestamp(9, Timestamp.valueOf(notification.getCreatedAt()));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    notification.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Notification notification) throws SQLException {
        String sql = """
                UPDATE notification 
                SET type = ?, entity_type = ?, entity_id = ?, title = ?, message = ?, is_read = ?, action_url = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, notification.getType().name());
            ps.setString(2, notification.getEntityType().name().toLowerCase());
            ps.setInt(3, notification.getEntityId());
            ps.setString(4, notification.getTitle());
            ps.setString(5, notification.getMessage());
            ps.setBoolean(6, notification.isRead());
            ps.setString(7, notification.getActionUrl());
            ps.setInt(8, notification.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM notification WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Optional<Notification> findById(Integer id) throws SQLException {
        String sql = """
                SELECT id, user_id, type, entity_type, entity_id, title, message, is_read, action_url, created_at, read_at
                FROM notification WHERE id = ?
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

    public List<Notification> getAll() throws SQLException {
        String sql = """
                SELECT id, user_id, type, entity_type, entity_id, title, message, is_read, action_url, created_at, read_at
                FROM notification ORDER BY created_at DESC
                """;

        List<Notification> notifications = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                notifications.add(mapRow(rs));
            }
        }
        return notifications;
    }

    /**
     * Get all unread notifications for a user
     */
    public List<Notification> getUnreadByUser(int userId) throws SQLException {
        String sql = """
                SELECT id, user_id, type, entity_type, entity_id, title, message, is_read, action_url, created_at, read_at
                FROM notification 
                WHERE user_id = ? AND is_read = FALSE
                ORDER BY created_at DESC
                """;

        List<Notification> notifications = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRow(rs));
                }
            }
        }
        return notifications;
    }

    /**
     * Get all notifications for a user (paginated)
     */
    public List<Notification> getByUser(int userId, int limit, int offset) throws SQLException {
        String sql = """
                SELECT id, user_id, type, entity_type, entity_id, title, message, is_read, action_url, created_at, read_at
                FROM notification 
                WHERE user_id = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """;

        List<Notification> notifications = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRow(rs));
                }
            }
        }
        return notifications;
    }

    /**
     * Get unread notification count for a user
     */
    public int getUnreadCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notification WHERE user_id = ? AND is_read = FALSE";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Mark notification as read
     */
    public void markAsRead(int notificationId) throws SQLException {
        String sql = "UPDATE notification SET is_read = TRUE, read_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        }
    }

    public void markAsRead(int notificationId, int userId) throws SQLException {
        String sql = """
                UPDATE notification
                SET is_read = TRUE, read_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ?
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void markAllAsRead(int userId) throws SQLException {
        String sql = "UPDATE notification SET is_read = TRUE, read_at = CURRENT_TIMESTAMP WHERE user_id = ? AND is_read = FALSE";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Delete old read notifications
     */
    public int deleteOldNotifications(int daysToKeep) throws SQLException {
        String sql = """
                DELETE FROM notification 
                WHERE is_read = TRUE AND created_at < CURRENT_TIMESTAMP - (? || ' days')::INTERVAL
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, daysToKeep);
            return ps.executeUpdate();
        }
    }

    /**
     * Get notifications for a specific entity
     */
    public List<Notification> getByEntity(String entityType, int entityId) throws SQLException {
        String sql = """
                SELECT id, user_id, type, entity_type, entity_id, title, message, is_read, action_url, created_at, read_at
                FROM notification 
                WHERE entity_type = ? AND entity_id = ?
                ORDER BY created_at DESC
                """;

        List<Notification> notifications = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, entityType);
            ps.setInt(2, entityId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRow(rs));
                }
            }
        }
        return notifications;
    }

    private static Notification mapRow(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setId(rs.getInt("id"));
        notification.setUserId(rs.getInt("user_id"));
        notification.setType(rs.getString("type"));
        notification.setEntityType(rs.getString("entity_type"));
        notification.setEntityId(rs.getInt("entity_id"));
        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setRead(rs.getBoolean("is_read"));
        notification.setActionUrl(rs.getString("action_url"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            notification.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp readAt = rs.getTimestamp("read_at");
        if (readAt != null) {
            notification.setReadAt(readAt.toLocalDateTime());
        }

        return notification;
    }

    public List<Notification> findLatestByUser(int userId, int limit) throws SQLException {
        return getByUser(userId, limit, 0);
    }

    public int countUnreadByUser(int userId) throws SQLException {
        return getUnreadCount(userId);
    }
}
