package services.admin;

import services.account.SecurityAlertMailService;
import services.account.UserService;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminModerationService {

    public enum ModerationAction {
        IGNORE,
        WARNING_ONLY,
        TEMP_BAN,
        PERMANENT_BAN
    }

    public record ModerationIncidentRow(
            int id,
            LocalDateTime createdAt,
            Integer userId,
            String userName,
            String userEmail,
            String entityType,
            String contentText,
            String contentPreview,
            String detectedReason,
            String flaggedAttributes,
            String warningStatus,
            String incidentStatus,
            String accountStatus,
            LocalDateTime bannedUntil
    ) {
    }

    private static final String SELECT_INCIDENTS = """
            SELECT mi.id, mi.created_at, mi.user_id, mi.entity_type, mi.content_text, mi.content_preview,
                   mi.detected_reason, mi.flagged_attributes, mi.warning_status, mi.incident_status,
                   u.first_name, u.last_name, u.email, u.status AS user_status, u.banned_until
            FROM moderation_incident mi
            LEFT JOIN "user" u ON u.id = mi.user_id
            ORDER BY mi.created_at DESC
            LIMIT ?
            """;

    private static final String UPDATE_INCIDENT_ACTION = """
            UPDATE moderation_incident
            SET incident_status = ?, action_taken = ?, action_reason = ?, ban_days = ?, action_by_admin_id = ?, action_at = ?, warning_status = ?
            WHERE id = ?
            """;

    private final UserService userService = new UserService();
    private final SecurityAlertMailService securityAlertMailService = new SecurityAlertMailService();

    public List<ModerationIncidentRow> findRecentIncidents(int limit) throws SQLException {
        Connection c = DbConnexion.getConnection();
        List<ModerationIncidentRow> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(SELECT_INCIDENTS)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String first = rs.getString("first_name");
                    String last = rs.getString("last_name");
                    String userName = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
                    if (userName.isBlank()) {
                        userName = "Utilisateur";
                    }
                    Timestamp createdTs = rs.getTimestamp("created_at");
                    Timestamp bannedUntilTs = rs.getTimestamp("banned_until");
                    out.add(new ModerationIncidentRow(
                            rs.getInt("id"),
                            createdTs != null ? createdTs.toLocalDateTime() : null,
                            (Integer) rs.getObject("user_id"),
                            userName,
                            rs.getString("email"),
                            rs.getString("entity_type"),
                            rs.getString("content_text"),
                            rs.getString("content_preview"),
                            rs.getString("detected_reason"),
                            rs.getString("flagged_attributes"),
                            rs.getString("warning_status"),
                            rs.getString("incident_status"),
                            rs.getString("user_status"),
                            bannedUntilTs != null ? bannedUntilTs.toLocalDateTime() : null
                    ));
                }
            }
        }
        return out;
    }

    public void applyAction(int incidentId,
                            Integer userId,
                            Integer adminId,
                            String userEmail,
                            String userDisplayName,
                            ModerationAction action,
                            String reason,
                            Integer tempBanDays) throws SQLException {
        if (action == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();

        String incidentStatus = "ACTION_TAKEN";
        String warningStatus = "WARNED";
        String actionName = action.name();
        Integer banDays = null;

        if (action == ModerationAction.IGNORE) {
            incidentStatus = "SEEN";
            warningStatus = "IGNORED";
        } else if (action == ModerationAction.WARNING_ONLY) {
            warningStatus = "WARNING_ONLY";
        } else if (action == ModerationAction.TEMP_BAN) {
            int days = tempBanDays == null ? 1 : Math.max(1, tempBanDays);
            banDays = days;
            LocalDateTime until = now.plusDays(days);
            if (userId != null) {
                userService.updateModerationStatus(userId, "temp_banned", until, reason);
            }
            sendBanEmail(userEmail, userDisplayName, false, days, reason);
        } else if (action == ModerationAction.PERMANENT_BAN) {
            if (userId != null) {
                userService.updateModerationStatus(userId, "banned", null, reason);
            }
            sendBanEmail(userEmail, userDisplayName, true, null, reason);
        }

        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE_INCIDENT_ACTION)) {
            ps.setString(1, incidentStatus);
            ps.setString(2, actionName);
            ps.setString(3, reason);
            if (banDays == null) {
                ps.setNull(4, java.sql.Types.INTEGER);
            } else {
                ps.setInt(4, banDays);
            }
            if (adminId == null) {
                ps.setNull(5, java.sql.Types.INTEGER);
            } else {
                ps.setInt(5, adminId);
            }
            ps.setTimestamp(6, Timestamp.valueOf(now));
            ps.setString(7, warningStatus);
            ps.setInt(8, incidentId);
            ps.executeUpdate();
        }
    }

    private void sendBanEmail(String userEmail, String userDisplayName, boolean permanent, Integer days, String reason) {
        if (userEmail == null || userEmail.isBlank()) {
            return;
        }
        securityAlertMailService.sendModerationBanNotice(
                userEmail,
                userDisplayName,
                permanent,
                days,
                reason
        );
    }
}
