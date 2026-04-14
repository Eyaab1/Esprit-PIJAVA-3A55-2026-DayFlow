package services.admin_module;

import model.coaching_session.CoachingRequest;
import model.coaching_session.Session;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agrégations et listes pour le tableau de bord administrateur (aligné maquettes DayFlow).
 */
public final class AdminStatsService {

    private final Connection cnx = DbConnexion.getConnection();

    public record OverviewStats(
            int totalUsers,
            int coachCount,
            int totalRequests,
            int pendingRequests,
            int totalSessions,
            int confirmedSessions,
            int goalsCount,
            int routinesCount
    ) {}

    public record RecentRequestRow(int id, String userName, String message, String status, Date createdAt) {}

    public record RecentSessionRow(int id, String coachName, String clientName, String status, Date createdAt) {}

    public record DailyPoint(LocalDate day, int users, int sessions, int requests) {}

    public record UserCardRow(
            int id,
            String firstName,
            String lastName,
            String email,
            boolean coach,
            String speciality,
            Double rating,
            Integer reviewCount,
            int coachSessionsCount,
            int coachRequestsCount,
            int clientRoutinesCount
    ) {}

    public OverviewStats loadOverview() throws SQLException {
        int totalUsers = count("SELECT COUNT(*) FROM \"user\"");
        int coachCount = count("""
                SELECT COUNT(*) FROM "user" u WHERE u.roles::text LIKE '%ROLE_COACH%'
                """);
        int totalRequests = count("SELECT COUNT(*) FROM coaching_request");
        int pendingRequests = count("""
                SELECT COUNT(*) FROM coaching_request WHERE LOWER(TRIM(status)) = ?
                """, CoachingRequest.STATUS_PENDING);
        int totalSessions = count("SELECT COUNT(*) FROM session");
        int confirmedSessions = count("""
                SELECT COUNT(*) FROM session WHERE LOWER(TRIM(status)) = ?
                """, Session.STATUS_CONFIRMED);
        int goalsCount = count("SELECT COUNT(*) FROM goal");
        int routinesCount = count("SELECT COUNT(*) FROM routine");
        return new OverviewStats(
                totalUsers, coachCount, totalRequests, pendingRequests,
                totalSessions, confirmedSessions, goalsCount, routinesCount
        );
    }

    private int count(String sql, String... stringParams) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            for (int i = 0; i < stringParams.length; i++) {
                ps.setString(i + 1, stringParams[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<RecentRequestRow> findRecentRequests(int limit) throws SQLException {
        String sql = """
                SELECT cr.id, cr.message, cr.status, cr.created_at,
                       u.first_name AS fn, u.last_name AS ln
                FROM coaching_request cr
                INNER JOIN "user" u ON u.id = cr.user_id
                ORDER BY cr.created_at DESC
                LIMIT ?
                """;
        List<RecentRequestRow> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = safe(rs.getString("fn")) + " " + safe(rs.getString("ln"));
                    list.add(new RecentRequestRow(
                            rs.getInt("id"),
                            name.trim(),
                            safe(rs.getString("message")),
                            safe(rs.getString("status")),
                            toDate(rs.getTimestamp("created_at"))
                    ));
                }
            }
        }
        return list;
    }

    public List<RecentSessionRow> findRecentSessions(int limit) throws SQLException {
        String sql = """
                SELECT s.id, s.status, s.created_at,
                       cf.first_name AS cfn, cf.last_name AS cln,
                       uf.first_name AS ufn, uf.last_name AS uln
                FROM session s
                INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
                INNER JOIN "user" cf ON cf.id = cr.coach_id
                INNER JOIN "user" uf ON uf.id = cr.user_id
                ORDER BY s.created_at DESC
                LIMIT ?
                """;
        List<RecentSessionRow> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String coach = (safe(rs.getString("cfn")) + " " + safe(rs.getString("cln"))).trim();
                    String client = (safe(rs.getString("ufn")) + " " + safe(rs.getString("uln"))).trim();
                    list.add(new RecentSessionRow(
                            rs.getInt("id"),
                            coach,
                            client,
                            safe(rs.getString("status")),
                            toDate(rs.getTimestamp("created_at"))
                    ));
                }
            }
        }
        return list;
    }

    /** Comptes par statut de demande (graphique secteurs). */
    public Map<String, Integer> countRequestsByStatus() throws SQLException {
        String sql = """
                SELECT LOWER(TRIM(status)) AS st, COUNT(*)::int AS cnt
                FROM coaching_request
                GROUP BY LOWER(TRIM(status))
                """;
        Map<String, Integer> map = new LinkedHashMap<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("st"), rs.getInt("cnt"));
            }
        }
        return map;
    }

    public List<DailyPoint> dailyActivityLast7Days() throws SQLException {
        String sql = """
                WITH days AS (
                    SELECT generate_series((CURRENT_DATE - INTERVAL '6 day')::date, CURRENT_DATE::date, INTERVAL '1 day')::date AS d
                )
                SELECT d,
                    (SELECT COUNT(*) FROM "user" u WHERE u.created_at::date = d) AS ucnt,
                    (SELECT COUNT(*) FROM session s WHERE s.created_at::date = d) AS scnt,
                    (SELECT COUNT(*) FROM coaching_request cr WHERE cr.created_at::date = d) AS rcnt
                FROM days
                ORDER BY d
                """;
        List<DailyPoint> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new DailyPoint(
                        rs.getDate("d").toLocalDate(),
                        rs.getInt("ucnt"),
                        rs.getInt("scnt"),
                        rs.getInt("rcnt")
                ));
            }
        }
        return list;
    }

    public List<UserCardRow> searchUsers(String namePart, String emailPart, int limit) throws SQLException {
        String n = namePart == null ? "" : namePart.trim();
        String e = emailPart == null ? "" : emailPart.trim();
        String sql = """
                SELECT u.id, u.first_name, u.last_name, u.email, u.roles::text AS roles_txt,
                       u.speciality, u.rating, u.review_count,
                       (SELECT COUNT(*) FROM session s
                        INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
                        WHERE cr.coach_id = u.id) AS sess_cnt,
                       (SELECT COUNT(*) FROM coaching_request cr2 WHERE cr2.coach_id = u.id) AS req_cnt,
                       (SELECT COUNT(*) FROM routine r
                        INNER JOIN goal g ON g.id = r.goal_id
                        WHERE g.user_id = u.id) AS client_routines
                FROM "user" u
                WHERE (? = '' OR u.first_name ILIKE '%' || ? || '%' OR u.last_name ILIKE '%' || ? || '%'
                      OR (u.first_name || ' ' || u.last_name) ILIKE '%' || ? || '%')
                  AND (? = '' OR u.email ILIKE '%' || ? || '%')
                ORDER BY u.id
                LIMIT ?
                """;
        List<UserCardRow> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, n);
            ps.setString(2, n);
            ps.setString(3, n);
            ps.setString(4, n);
            ps.setString(5, e);
            ps.setString(6, e);
            ps.setInt(7, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String rolesTxt = rs.getString("roles_txt");
                    boolean coach = rolesTxt != null && rolesTxt.contains("ROLE_COACH");
                    double rating = rs.getDouble("rating");
                    if (rs.wasNull()) {
                        rating = 0;
                    }
                    int rc = rs.getInt("review_count");
                    Integer reviewCount = rs.wasNull() ? null : rc;
                    list.add(new UserCardRow(
                            rs.getInt("id"),
                            safe(rs.getString("first_name")),
                            safe(rs.getString("last_name")),
                            safe(rs.getString("email")),
                            coach,
                            safe(rs.getString("speciality")),
                            rating,
                            reviewCount,
                            rs.getInt("sess_cnt"),
                            rs.getInt("req_cnt"),
                            rs.getInt("client_routines")
                    ));
                }
            }
        }
        return list;
    }

    /** Coachs uniquement ; recherche libre sur nom, e-mail ou spécialité. */
    public List<UserCardRow> searchCoaches(String query, int limit) throws SQLException {
        String t = query == null ? "" : query.trim();
        String sql = """
                SELECT u.id, u.first_name, u.last_name, u.email, u.roles::text AS roles_txt,
                       u.speciality, u.rating, u.review_count,
                       (SELECT COUNT(*) FROM session s
                        INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
                        WHERE cr.coach_id = u.id) AS sess_cnt,
                       (SELECT COUNT(*) FROM coaching_request cr2 WHERE cr2.coach_id = u.id) AS req_cnt,
                       (SELECT COUNT(*) FROM routine r
                        INNER JOIN goal g ON g.id = r.goal_id
                        WHERE g.user_id = u.id) AS client_routines
                FROM "user" u
                WHERE u.roles::text LIKE '%ROLE_COACH%'
                  AND (? = '' OR u.first_name ILIKE '%' || ? || '%' OR u.last_name ILIKE '%' || ? || '%'
                      OR (u.first_name || ' ' || u.last_name) ILIKE '%' || ? || '%'
                      OR u.email ILIKE '%' || ? || '%'
                      OR COALESCE(u.speciality, '') ILIKE '%' || ? || '%')
                ORDER BY u.id
                LIMIT ?
                """;
        List<UserCardRow> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, t);
            ps.setString(2, t);
            ps.setString(3, t);
            ps.setString(4, t);
            ps.setString(5, t);
            ps.setString(6, t);
            ps.setString(7, t);
            ps.setInt(8, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double rating = rs.getDouble("rating");
                    if (rs.wasNull()) {
                        rating = 0;
                    }
                    int rc = rs.getInt("review_count");
                    Integer reviewCount = rs.wasNull() ? null : rc;
                    list.add(new UserCardRow(
                            rs.getInt("id"),
                            safe(rs.getString("first_name")),
                            safe(rs.getString("last_name")),
                            safe(rs.getString("email")),
                            true,
                            safe(rs.getString("speciality")),
                            rating,
                            reviewCount,
                            rs.getInt("sess_cnt"),
                            rs.getInt("req_cnt"),
                            rs.getInt("client_routines")
                    ));
                }
            }
        }
        return list;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static Date toDate(Timestamp ts) {
        return ts == null ? null : new Date(ts.getTime());
    }
}
