package services.account;

import utils.DbConnexion;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Option B: reuse existing DB table user_login_history.
 * The service adapts dynamically to available column names.
 */
public class AccountSecurityService {

    private static final DateTimeFormatter DISPLAY_DF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH);
    private static final String HISTORY_TABLE = "user_login_history";

    public LoginSuccessMeta registerSuccessfulLogin(int userId, String identifier, int recentFailedAttempts) throws SQLException {
        String normalizedIdentifier = identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
        String deviceLabel = resolveDeviceLabel();
        String ipAddress = resolveIpAddress();
        String sessionToken = UUID.randomUUID().toString();

        Columns c = resolveColumns();
        if (!c.tableExists()) {
            boolean suspiciousFallback = recentFailedAttempts >= 3;
            String reasonFallback = suspiciousFallback ? "Multiple failed logins before success" : null;
            return new LoginSuccessMeta(sessionToken, deviceLabel, ipAddress, suspiciousFallback, reasonFallback);
        }

        int previousSuccessCount = countPreviousSuccess(userId, c);
        int sameDeviceCount = countPreviousSuccessForDevice(userId, deviceLabel, c);

        boolean suspicious = false;
        String suspiciousReason = null;
        if (previousSuccessCount > 0 && c.device() != null && sameDeviceCount == 0) {
            suspicious = true;
            suspiciousReason = "New device detected";
        } else if (recentFailedAttempts >= 3) {
            suspicious = true;
            suspiciousReason = "Multiple failed logins before success";
        }

        insertLoginHistory(userId, normalizedIdentifier, deviceLabel, ipAddress, sessionToken, suspicious, suspiciousReason, c);
        return new LoginSuccessMeta(sessionToken, deviceLabel, ipAddress, suspicious, suspiciousReason);
    }

    public LoginSuccessMeta registerSuccessfulLogin(int userId, String identifier) throws SQLException {
        return registerSuccessfulLogin(userId, identifier, 0);
    }

    public void touchSession(int userId, String sessionToken) throws SQLException {
        if (sessionToken == null || sessionToken.isBlank()) {
            return;
        }
        Columns c = resolveColumns();
        if (!c.tableExists() || c.sessionToken() == null || c.lastSeenAt() == null) {
            return;
        }

        StringBuilder sql = new StringBuilder()
                .append("UPDATE ").append(HISTORY_TABLE)
                .append(" SET ").append(c.lastSeenAt()).append(" = CURRENT_TIMESTAMP")
                .append(" WHERE ").append(c.userId()).append(" = ?")
                .append(" AND ").append(c.sessionToken()).append(" = ?");
        if (c.revokedAt() != null) {
            sql.append(" AND ").append(c.revokedAt()).append(" IS NULL");
        }

        Connection conn = DbConnexion.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, userId);
            ps.setString(2, sessionToken);
            ps.executeUpdate();
        }
    }

    public List<ActiveSessionView> findActiveSessions(int userId, String currentSessionToken) throws SQLException {
        List<ActiveSessionView> out = new ArrayList<>();
        Columns c = resolveColumns();
        if (!c.tableExists() || c.userId() == null) {
            return out;
        }

        if (c.sessionToken() == null) {
            return findActiveSessionsFallbackWithoutToken(userId, c);
        }

        String connectedColumn = c.attemptedAt() != null ? c.attemptedAt() : c.lastSeenAt();
        String lastSeenColumn = c.lastSeenAt() != null ? c.lastSeenAt() : connectedColumn;
        if (connectedColumn == null) {
            return out;
        }

        StringBuilder sql = new StringBuilder()
                .append("SELECT ").append(c.sessionToken()).append(" AS session_token, ")
                .append(c.deviceOrFallback()).append(" AS device_label, ")
                .append(connectedColumn).append(" AS connected_at, ")
                .append(lastSeenColumn).append(" AS last_seen_at")
                .append(" FROM ").append(HISTORY_TABLE)
                .append(" WHERE ").append(c.userId()).append(" = ?");
        if (c.success() != null) {
            sql.append(" AND ").append(c.success()).append(" = true");
        }
        if (c.revokedAt() != null) {
            sql.append(" AND ").append(c.revokedAt()).append(" IS NULL");
        }
        sql.append(" ORDER BY ").append(lastSeenColumn).append(" DESC NULLS LAST LIMIT 8");

        Connection conn = DbConnexion.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String sessionToken = rs.getString("session_token");
                    String device = rs.getString("device_label");
                    LocalDateTime connectedAt = toLdt(rs.getTimestamp("connected_at"));
                    LocalDateTime lastSeen = toLdt(rs.getTimestamp("last_seen_at"));
                    boolean current = sessionToken != null && sessionToken.equals(currentSessionToken);
                    out.add(new ActiveSessionView(
                            device == null || device.isBlank() ? "Unknown device" : device,
                            formatDateTime(connectedAt),
                            formatDateTime(lastSeen),
                            current
                    ));
                }
            }
        }
        return out;
    }

    private List<ActiveSessionView> findActiveSessionsFallbackWithoutToken(int userId, Columns c) throws SQLException {
        List<ActiveSessionView> out = new ArrayList<>();
        String attempted = c.attemptedAt() != null ? c.attemptedAt() : c.lastSeenAt();
        if (attempted == null) {
            return out;
        }

        StringBuilder sql = new StringBuilder()
                .append("SELECT ")
                .append(c.deviceOrFallback()).append(" AS device_label, ")
                .append(attempted).append(" AS connected_at ")
                .append("FROM ").append(HISTORY_TABLE).append(" ")
                .append("WHERE ").append(c.userId()).append(" = ?");
        if (c.success() != null) {
            sql.append(" AND ").append(c.success()).append(" = true");
        }
        sql.append(" ORDER BY ").append(attempted).append(" DESC NULLS LAST LIMIT 1");

        Connection conn = DbConnexion.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime dt = toLdt(rs.getTimestamp("connected_at"));
                    out.add(new ActiveSessionView(
                            rs.getString("device_label"),
                            formatDateTime(dt),
                            formatDateTime(dt),
                            true
                    ));
                }
            }
        }
        return out;
    }

    public List<LoginHistoryView> findRecentLogins(int userId) throws SQLException {
        List<LoginHistoryView> out = new ArrayList<>();
        Columns c = resolveColumns();
        if (!c.tableExists() || c.userId() == null) {
            return out;
        }

        String attempted = c.attemptedAt() != null ? c.attemptedAt() : c.lastSeenAt();
        if (attempted == null) {
            return out;
        }

        String successExpr = c.success() != null ? c.success() : "true";
        String suspiciousExpr = c.suspicious() != null ? c.suspicious() : "false";
        String reasonExpr = c.suspiciousReason() != null ? c.suspiciousReason() : "NULL";
        String deviceExpr = c.deviceOrFallback();
        String ipExpr = c.ipAddress() != null ? c.ipAddress() : "NULL";

        String sql = "SELECT "
                + attempted + " AS attempted_at, "
                + successExpr + " AS success, "
                + suspiciousExpr + " AS suspicious, "
                + reasonExpr + " AS suspicious_reason, "
                + deviceExpr + " AS device_label, "
                + ipExpr + " AS ip_address "
                + "FROM " + HISTORY_TABLE + " "
                + "WHERE " + c.userId() + " = ? "
                + "ORDER BY " + attempted + " DESC NULLS LAST LIMIT 8";

        Connection conn = DbConnexion.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new LoginHistoryView(
                            formatDateTime(toLdt(rs.getTimestamp("attempted_at"))),
                            rs.getBoolean("success"),
                            rs.getBoolean("suspicious"),
                            rs.getString("suspicious_reason"),
                            rs.getString("device_label"),
                            rs.getString("ip_address")
                    ));
                }
            }
        }
        return out;
    }

    public int logoutAllDevices(int userId) throws SQLException {
        Columns c = resolveColumns();
        if (!c.tableExists() || c.userId() == null) {
            return 0;
        }
        if (c.revokedAt() != null) {
            String sql = "UPDATE " + HISTORY_TABLE + " SET " + c.revokedAt() + " = CURRENT_TIMESTAMP WHERE " + c.userId() + " = ?"
                    + " AND " + c.revokedAt() + " IS NULL";
            Connection conn = DbConnexion.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                return ps.executeUpdate();
            }
        }
        return 0;
    }

    public void revokeCurrentSession(int userId, String sessionToken) throws SQLException {
        if (sessionToken == null || sessionToken.isBlank()) {
            return;
        }
        Columns c = resolveColumns();
        if (!c.tableExists() || c.userId() == null || c.sessionToken() == null || c.revokedAt() == null) {
            return;
        }
        String sql = "UPDATE " + HISTORY_TABLE + " SET " + c.revokedAt() + " = CURRENT_TIMESTAMP WHERE "
                + c.userId() + " = ? AND " + c.sessionToken() + " = ? AND " + c.revokedAt() + " IS NULL";
        Connection conn = DbConnexion.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, sessionToken);
            ps.executeUpdate();
        }
    }

    private int countPreviousSuccess(int userId, Columns c) throws SQLException {
        if (c.userId() == null) {
            return 0;
        }
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(HISTORY_TABLE).append(" WHERE ").append(c.userId()).append(" = ?");
        if (c.success() != null) {
            sql.append(" AND ").append(c.success()).append(" = true");
        }
        Connection conn = DbConnexion.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private int countPreviousSuccessForDevice(int userId, String deviceLabel, Columns c) throws SQLException {
        if (c.userId() == null || c.device() == null) {
            return 0;
        }
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(HISTORY_TABLE)
                .append(" WHERE ").append(c.userId()).append(" = ?")
                .append(" AND COALESCE(").append(c.device()).append(", '') = COALESCE(?, '')");
        if (c.success() != null) {
            sql.append(" AND ").append(c.success()).append(" = true");
        }
        Connection conn = DbConnexion.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, userId);
            ps.setString(2, deviceLabel);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private void insertLoginHistory(int userId,
                                    String identifier,
                                    String deviceLabel,
                                    String ipAddress,
                                    String sessionToken,
                                    boolean suspicious,
                                    String suspiciousReason,
                                    Columns c) throws SQLException {
        Map<String, Object> values = new LinkedHashMap<>();
        if (c.userId() != null) {
            values.put(c.userId(), userId);
        }
        if (c.identifier() != null) {
            values.put(c.identifier(), identifier);
        }
        if (c.device() != null) {
            values.put(c.device(), deviceLabel);
        }
        if (c.ipAddress() != null) {
            values.put(c.ipAddress(), ipAddress);
        }
        if (c.success() != null) {
            values.put(c.success(), true);
        }
        if (c.suspicious() != null) {
            values.put(c.suspicious(), suspicious);
        }
        if (c.suspiciousReason() != null) {
            values.put(c.suspiciousReason(), suspiciousReason);
        }
        if (c.sessionToken() != null) {
            values.put(c.sessionToken(), sessionToken);
        }
        if (c.attemptedAt() != null) {
            values.put(c.attemptedAt(), Timestamp.valueOf(LocalDateTime.now()));
        }
        if (c.lastSeenAt() != null) {
            values.put(c.lastSeenAt(), Timestamp.valueOf(LocalDateTime.now()));
        }

        if (values.isEmpty()) {
            return;
        }

        StringBuilder cols = new StringBuilder();
        StringBuilder params = new StringBuilder();
        for (String col : values.keySet()) {
            if (!cols.isEmpty()) {
                cols.append(", ");
                params.append(", ");
            }
            cols.append(col);
            params.append("?");
        }
        String sql = "INSERT INTO " + HISTORY_TABLE + " (" + cols + ") VALUES (" + params + ")";
        Connection conn = DbConnexion.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            for (Object v : values.values()) {
                ps.setObject(i++, v);
            }
            ps.executeUpdate();
        }
    }

    private Columns resolveColumns() throws SQLException {
        Connection conn = DbConnexion.getConnection();
        DatabaseMetaData meta = conn.getMetaData();
        Set<String> cols = new HashSet<>();
        boolean exists = false;
        try (ResultSet rs = meta.getColumns(null, null, HISTORY_TABLE, null)) {
            while (rs.next()) {
                exists = true;
                cols.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        if (!exists) {
            try (ResultSet rs = meta.getColumns(null, "public", HISTORY_TABLE, null)) {
                while (rs.next()) {
                    exists = true;
                    cols.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
        }

        String userId = pick(cols, "user_id", "userid");
        String identifier = pick(cols, "identifier", "email", "user_identifier");
        String device = pick(cols, "device_label", "user_agent", "device", "client", "device_info");
        String ipAddress = pick(cols, "ip_address", "ip", "client_ip");
        String success = pick(cols, "success", "is_successful", "login_success", "is_success");
        String suspicious = pick(cols, "suspicious", "is_suspicious");
        String suspiciousReason = pick(cols, "suspicious_reason", "reason");
        String attemptedAt = pick(cols, "attempted_at", "login_at", "created_at", "logged_in_at", "login_time", "logged_at");
        String lastSeenAt = pick(cols, "last_seen_at", "updated_at");
        String sessionToken = pick(cols, "session_token", "token", "session_id");
        String revokedAt = pick(cols, "revoked_at", "logged_out_at", "logout_at");

        return new Columns(exists, userId, identifier, device, ipAddress, success, suspicious, suspiciousReason, attemptedAt, lastSeenAt, sessionToken, revokedAt);
    }

    private static String pick(Set<String> cols, String... candidates) {
        for (String candidate : candidates) {
            if (cols.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String resolveDeviceLabel() {
        String os = System.getProperty("os.name", "Desktop").trim();
        String user = System.getProperty("user.name", "user").trim();
        String host = "localhost";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
        }
        return os + " / " + user + "@" + host;
    }

    private static String resolveIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
            return "127.0.0.1";
        }
    }

    private static LocalDateTime toLdt(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    private static String formatDateTime(LocalDateTime dt) {
        if (dt == null) {
            return "Unknown";
        }
        return dt.format(DISPLAY_DF);
    }

    private record Columns(boolean tableExists,
                           String userId,
                           String identifier,
                           String device,
                           String ipAddress,
                           String success,
                           String suspicious,
                           String suspiciousReason,
                           String attemptedAt,
                           String lastSeenAt,
                           String sessionToken,
                           String revokedAt) {
        String deviceOrFallback() {
            return device != null ? "COALESCE(" + device + ", 'Unknown device')" : "'Unknown device'";
        }
    }

    public record LoginSuccessMeta(String sessionToken, String deviceLabel, String ipAddress, boolean suspicious, String suspiciousReason) {
    }

    public record ActiveSessionView(String deviceLabel, String connectedAt, String lastSeenAt, boolean currentSession) {
    }

    public record LoginHistoryView(String attemptedAt, boolean success, boolean suspicious, String suspiciousReason,
                                   String deviceLabel, String ipAddress) {
    }
}
