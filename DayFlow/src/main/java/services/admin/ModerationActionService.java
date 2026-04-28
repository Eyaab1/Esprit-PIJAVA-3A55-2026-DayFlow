package services.admin;

import services.account.SecurityAlertMailService;
import services.account.UserService;
import services.notification.NotificationService;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ModerationActionService {

    public enum BanType {
        POSTING_BAN,
        ACCOUNT_BAN
    }

    private final UserService userService = new UserService();
    private final SecurityAlertMailService mailService = new SecurityAlertMailService();
    private final NotificationService notificationService = new NotificationService();

    private static final String UPDATE_INCIDENT_WITH_BAN = """
            UPDATE moderation_incident
            SET incident_status = ?, action_taken = ?, action_reason = ?, ban_days = ?, 
                action_by_admin_id = ?, action_at = ?, warning_status = ?, ban_type = ?
            WHERE id = ?
            """;

    /**
     * Apply a posting ban to a user (user can login but cannot create/edit posts or comments)
     */
    public void applyPostingBan(int incidentId, Integer userId, Integer adminId, String userEmail, 
                                String userName, int banDays, String reason) throws SQLException {
        if (userId == null) {
            throw new SQLException("User ID required for posting ban");
        }

        // Verify user exists before applying ban
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM \"user\" WHERE id = ?")) {
            ps.setInt(1, userId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Cannot ban user: User ID " + userId + " does not exist in the database");
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime banUntil = now.plusDays(banDays);

        // Update user posting ban
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE \"user\" SET posting_banned_until = ?, posting_ban_reason = ? WHERE id = ?")) {
            ps.setTimestamp(1, Timestamp.valueOf(banUntil));
            ps.setString(2, reason);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }

        // Update moderation incident
        try (PreparedStatement ps = c.prepareStatement(UPDATE_INCIDENT_WITH_BAN)) {
            ps.setString(1, "ACTION_TAKEN");
            ps.setString(2, "POSTING_BAN");
            ps.setString(3, reason);
            ps.setInt(4, banDays);
            if (adminId == null) {
                ps.setNull(5, java.sql.Types.INTEGER);
            } else {
                ps.setInt(5, adminId);
            }
            ps.setTimestamp(6, Timestamp.valueOf(now));
            ps.setString(7, "POSTING_BAN_APPLIED");
            ps.setString(8, "POSTING_BAN");
            ps.setInt(9, incidentId);
            ps.executeUpdate();
        }

        // Create notification for the user
        String formattedDate = banUntil.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String notificationMessage = "You are temporarily banned from posting because your content violated community rules. Your restriction will end on " + formattedDate + ".";
        
        try {
            notificationService.createNotification(userId, "MODERATION_BAN", notificationMessage);
            System.out.println("Notification created successfully for user ID: " + userId);
        } catch (SQLException e) {
            // Log but don't fail the ban operation if notification creation fails
            System.err.println("Failed to create notification for user ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Apply an account ban to a user (full account suspension + email notification)
     */
    public void applyAccountBan(int incidentId, Integer userId, Integer adminId, String userEmail,
                                String userName, int banDays, String reason) throws SQLException {
        if (userId == null) {
            throw new SQLException("User ID required for account ban");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime banUntil = now.plusDays(banDays);
        
        System.out.println("Applying account ban: User ID=" + userId + ", Days=" + banDays + ", Until=" + 
                          banUntil.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        // Update user account ban
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE \"user\" SET banned_until = ?, ban_reason = ?, status = ? WHERE id = ?")) {
            ps.setTimestamp(1, Timestamp.valueOf(banUntil));
            ps.setString(2, reason);
            ps.setString(3, "temp_banned");
            ps.setInt(4, userId);
            int updated = ps.executeUpdate();
            
            if (updated == 0) {
                throw new SQLException("Failed to update user ban status: User ID " + userId + " not found");
            }
            
            System.out.println("User account ban applied successfully in database");
        }

        // Update moderation incident
        try (PreparedStatement ps = c.prepareStatement(UPDATE_INCIDENT_WITH_BAN)) {
            ps.setString(1, "ACTION_TAKEN");
            ps.setString(2, "ACCOUNT_BAN");
            ps.setString(3, reason);
            ps.setInt(4, banDays);
            if (adminId == null) {
                ps.setNull(5, java.sql.Types.INTEGER);
            } else {
                ps.setInt(5, adminId);
            }
            ps.setTimestamp(6, Timestamp.valueOf(now));
            ps.setString(7, "ACCOUNT_BAN_APPLIED");
            ps.setString(8, "ACCOUNT_BAN");
            ps.setInt(9, incidentId);
            ps.executeUpdate();
            
            System.out.println("Moderation incident updated successfully");
        }

        // Send email notification
        sendAccountBanEmail(userEmail, userName, banDays, reason);
    }

    /**
     * Check if a user is currently banned from posting
     */
    public boolean isPostingBanned(Integer userId) throws SQLException {
        if (userId == null) {
            return false;
        }
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT posting_banned_until FROM \"user\" WHERE id = ?")) {
            ps.setInt(1, userId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp banUntil = rs.getTimestamp("posting_banned_until");
                    if (banUntil != null) {
                        LocalDateTime banTime = banUntil.toLocalDateTime();
                        return LocalDateTime.now().isBefore(banTime);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Get posting ban end date for a user
     */
    public LocalDateTime getPostingBanUntil(Integer userId) throws SQLException {
        if (userId == null) {
            return null;
        }
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT posting_banned_until FROM \"user\" WHERE id = ?")) {
            ps.setInt(1, userId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp banUntil = rs.getTimestamp("posting_banned_until");
                    if (banUntil != null) {
                        return banUntil.toLocalDateTime();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get posting ban reason for a user
     */
    public String getPostingBanReason(Integer userId) throws SQLException {
        if (userId == null) {
            return null;
        }
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT posting_ban_reason FROM \"user\" WHERE id = ?")) {
            ps.setInt(1, userId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("posting_ban_reason");
                }
            }
        }
        return null;
    }

    /**
     * Automatically lift posting ban if expired
     */
    public void liftExpiredPostingBan(Integer userId) throws SQLException {
        if (userId == null) {
            return;
        }
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE \"user\" SET posting_banned_until = NULL, posting_ban_reason = NULL WHERE id = ? AND posting_banned_until IS NOT NULL AND posting_banned_until < NOW()")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private void sendAccountBanEmail(String userEmail, String userName, int banDays, String reason) {
        if (userEmail == null || userEmail.isBlank()) {
            System.err.println("Cannot send account ban email: User email is null or empty");
            return;
        }
        
        System.out.println("Sending account ban email to: " + userEmail + " (User: " + userName + ", Days: " + banDays + ")");
        
        try {
            boolean sent = mailService.sendModerationBanNotice(userEmail, userName, false, banDays, reason);
            if (sent) {
                System.out.println("Account ban email sent successfully to: " + userEmail);
            } else {
                System.err.println("Failed to send account ban email to: " + userEmail + " (Mail service returned false)");
            }
        } catch (Exception e) {
            System.err.println("Exception while sending account ban email to: " + userEmail);
            e.printStackTrace();
        }
    }
}
