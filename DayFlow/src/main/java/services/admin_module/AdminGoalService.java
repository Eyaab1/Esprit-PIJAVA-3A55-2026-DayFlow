package services.admin_module;

import utils.DbConnexion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Liste des objectifs pour l’administration (propriétaire, routines, filtres).
 */
public final class AdminGoalService {

    public record AdminGoalRow(
            int id,
            String title,
            String descriptionSnippet,
            String ownerFirstName,
            String ownerLastName,
            String ownerEmail,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate deadline,
            String status,
            String priority,
            int progress,
            int routineCount,
            LocalDateTime createdAt
    ) {
        public String ownerFullName() {
            return (nz(ownerFirstName) + " " + nz(ownerLastName)).trim();
        }

        private static String nz(String s) {
            return s == null ? "" : s;
        }
    }

    public enum SortOrder {
        NEWEST, OLDEST
    }

    private final Connection cnx = DbConnexion.getConnection();

    /**
     * Propriétaire : participation {@code OWNER} si présente, sinon {@code goal.user_id}.
     */
    public List<AdminGoalRow> searchGoals(
            String ownerNamePart,
            String ownerEmailPart,
            String statusFilter,
            SortOrder sort,
            int limit
    ) throws SQLException {
        String n = ownerNamePart == null ? "" : ownerNamePart.trim();
        String e = ownerEmailPart == null ? "" : ownerEmailPart.trim();
        String st = statusFilter == null ? "" : statusFilter.trim().toLowerCase();
        String order = sort == SortOrder.OLDEST ? "ASC" : "DESC";
        String statusClause = st.isEmpty()
                ? ""
                : " AND LOWER(TRIM(g.status)) = ? ";
        String sql = """
                SELECT g.id, g.title, g.description, g.start_date, g.end_date, g.deadline, g.status, g.priority,
                       g.progress, g.created_at, g.updated_at,
                       COALESCE(uo.first_name, ud.first_name) AS ofn,
                       COALESCE(uo.last_name, ud.last_name) AS oln,
                       COALESCE(uo.email, ud.email) AS oemail,
                       (SELECT COUNT(*)::int FROM routine r WHERE r.goal_id = g.id) AS rcnt
                FROM goal g
                LEFT JOIN goal_participation gp ON gp.goal_id = g.id AND gp.role = 'OWNER'
                LEFT JOIN "user" uo ON uo.id = gp.user_id
                LEFT JOIN "user" ud ON ud.id = g.user_id
                WHERE (? = '' OR COALESCE(uo.first_name, ud.first_name) ILIKE '%' || ? || '%'
                      OR COALESCE(uo.last_name, ud.last_name) ILIKE '%' || ? || '%'
                      OR (COALESCE(uo.first_name, ud.first_name) || ' ' || COALESCE(uo.last_name, ud.last_name)) ILIKE '%' || ? || '%')
                  AND (? = '' OR COALESCE(uo.email, ud.email) ILIKE '%' || ? || '%')
                """ + statusClause + """
                ORDER BY g.created_at %s
                LIMIT ?
                """.formatted(order);
        List<AdminGoalRow> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, n);
            ps.setString(i++, n);
            ps.setString(i++, n);
            ps.setString(i++, n);
            ps.setString(i++, e);
            ps.setString(i++, e);
            if (!st.isEmpty()) {
                ps.setString(i++, st);
            }
            ps.setInt(i, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private static AdminGoalRow mapRow(ResultSet rs) throws SQLException {
        Date sd = rs.getDate("start_date");
        Date ed = rs.getDate("end_date");
        Date dl = rs.getDate("deadline");
        Timestamp ca = rs.getTimestamp("created_at");
        String desc = rs.getString("description");
        return new AdminGoalRow(
                rs.getInt("id"),
                safe(rs.getString("title")),
                snippet(desc, 100),
                safe(rs.getString("ofn")),
                safe(rs.getString("oln")),
                safe(rs.getString("oemail")),
                sd != null ? sd.toLocalDate() : null,
                ed != null ? ed.toLocalDate() : null,
                dl != null ? dl.toLocalDate() : null,
                safe(rs.getString("status")),
                safe(rs.getString("priority")),
                rs.getInt("progress"),
                rs.getInt("rcnt"),
                ca != null ? ca.toLocalDateTime() : null
        );
    }

    private static String snippet(String content, int max) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String plain = content.replace('\n', ' ').trim();
        if (plain.length() <= max) {
            return plain;
        }
        return plain.substring(0, max - 1) + "…";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
