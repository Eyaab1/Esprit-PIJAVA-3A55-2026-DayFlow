package services.notification;

import model.notification.Notification;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    private static final String SELECT_LATEST_BY_USER = """
            SELECT id, user_id, type, message, is_read, created_at
            FROM notification
            WHERE user_id = ?
            ORDER BY created_at DESC
            LIMIT ?
            """;

    private static final String COUNT_UNREAD_BY_USER = """
            SELECT COUNT(*)::int
            FROM notification
            WHERE user_id = ? AND is_read = FALSE
            """;

    private static final String MARK_ONE_AS_READ = """
            UPDATE notification
            SET is_read = TRUE
            WHERE id = ? AND user_id = ?
            """;

    private static final String MARK_ALL_AS_READ = """
            UPDATE notification
            SET is_read = TRUE
            WHERE user_id = ? AND is_read = FALSE
            """;

    private static final String INSERT_NOTIFICATION = """
            INSERT INTO notification (user_id, type, message, is_read, created_at)
            VALUES (?, ?, ?, ?, ?)
            """;

    public List<Notification> findLatestByUser(int userId, int limit) throws SQLException {
        List<Notification> out = new ArrayList<>();
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_LATEST_BY_USER)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Notification n = new Notification();
                    n.setId(rs.getInt("id"));
                    n.setUserId(rs.getInt("user_id"));
                    n.setType(rs.getString("type"));
                    n.setMessage(rs.getString("message"));
                    n.setRead(rs.getBoolean("is_read"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    n.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
                    out.add(n);
                }
            }
        }
        return out;
    }

    public int countUnreadByUser(int userId) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(COUNT_UNREAD_BY_USER)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void markAsRead(int notificationId, int userId) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(MARK_ONE_AS_READ)) {
            ps.setInt(1, notificationId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void markAllAsRead(int userId) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(MARK_ALL_AS_READ)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public void createNotification(int userId, String type, String message) throws SQLException {
        System.out.println("createNotification called - userId: " + userId + ", type: " + type + ", message: " + message);
        Connection c = DbConnexion.getConnection();
        System.out.println("Database connection obtained: " + (c != null));
        try (PreparedStatement ps = c.prepareStatement(INSERT_NOTIFICATION)) {
            ps.setInt(1, userId);
            ps.setString(2, type);
            ps.setString(3, message);
            ps.setBoolean(4, false);
            ps.setTimestamp(5, Timestamp.valueOf(java.time.LocalDateTime.now()));
            int rowsAffected = ps.executeUpdate();
            System.out.println("Notification inserted successfully. Rows affected: " + rowsAffected);
        } catch (SQLException e) {
            System.err.println("Error inserting notification: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
