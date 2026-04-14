package services.admin_module;

import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Liste et filtres des publications pour l’administration (tableau type maquette).
 */
public final class AdminPostService {

    public record AdminPostRow(
            int id,
            String title,
            String contentSnippet,
            String authorFirstName,
            String authorLastName,
            String authorEmail,
            LocalDateTime createdAt,
            int viewCount,
            int clickCount,
            String statusRaw,
            String tagsSummary
    ) {
        public String authorFullName() {
            return (safe(authorFirstName) + " " + safe(authorLastName)).trim();
        }

        private static String safe(String s) {
            return s == null ? "" : s;
        }
    }

    public enum TrendFilter {
        ALL, TRENDING, STABLE, DECLINING
    }

    public enum SortOrder {
        NEWEST, OLDEST
    }

    private final Connection cnx = DbConnexion.getConnection();

    public List<AdminPostRow> searchPosts(
            String authorNamePart,
            String authorEmailPart,
            int tagIdOrZero,
            SortOrder sort,
            int limit
    ) throws SQLException {
        String n = authorNamePart == null ? "" : authorNamePart.trim();
        String e = authorEmailPart == null ? "" : authorEmailPart.trim();
        String order = sort == SortOrder.OLDEST ? "ASC" : "DESC";
        String tagClause = tagIdOrZero > 0
                ? " AND EXISTS (SELECT 1 FROM post_tags pt2 WHERE pt2.post_id = p.id AND pt2.tag_id = " + tagIdOrZero + ")"
                : "";
        String sql = """
                SELECT p.id, p.title, p.content, p.created_at, p.status, p.view_count, p.click_count,
                       u.first_name AS afn, u.last_name AS aln, u.email AS aemail,
                       (SELECT string_agg(t.name, ', ' ORDER BY t.name)
                        FROM post_tags pt
                        INNER JOIN tags t ON t.id = pt.tag_id
                        WHERE pt.post_id = p.id) AS tag_names
                FROM post p
                LEFT JOIN "user" u ON u.id = p.created_by_id
                WHERE (p.deleted_at IS NULL)
                  AND (? = '' OR u.first_name ILIKE '%' || ? || '%' OR u.last_name ILIKE '%' || ? || '%'
                       OR (u.first_name || ' ' || u.last_name) ILIKE '%' || ? || '%')
                  AND (? = '' OR u.email ILIKE '%' || ? || '%')
                """ + tagClause + """
                ORDER BY p.created_at %s
                LIMIT ?
                """.formatted(order);
        List<AdminPostRow> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, n);
            ps.setString(i++, n);
            ps.setString(i++, n);
            ps.setString(i++, n);
            ps.setString(i++, e);
            ps.setString(i++, e);
            ps.setInt(i, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String content = rs.getString("content");
                    list.add(new AdminPostRow(
                            rs.getInt("id"),
                            safe(rs.getString("title")),
                            snippet(content, 120),
                            safe(rs.getString("afn")),
                            safe(rs.getString("aln")),
                            safe(rs.getString("aemail")),
                            toLdt(rs.getTimestamp("created_at")),
                            rs.getInt("view_count"),
                            rs.getInt("click_count"),
                            safe(rs.getString("status")),
                            safe(rs.getString("tag_names"))
                    ));
                }
            }
        }
        return list;
    }

    public static String ctrLabel(int views, int clicks) {
        int v = Math.max(1, views);
        double ctr = (double) clicks / v;
        return ctr >= 0.06 ? "Élevé" : "Faible";
    }

    public static String trendLabel(int views, int clicks) {
        int v = Math.max(1, views);
        double ctr = (double) clicks / v;
        if (views >= 40 && ctr >= 0.05) {
            return "Tendance";
        }
        if (views < 15 || ctr < 0.015) {
            return "En baisse";
        }
        return "Stable";
    }

    public static boolean matchesTrend(AdminPostRow row, TrendFilter filter) {
        if (filter == TrendFilter.ALL) {
            return true;
        }
        String t = trendLabel(row.viewCount(), row.clickCount());
        return switch (filter) {
            case TRENDING -> "Tendance".equals(t);
            case STABLE -> "Stable".equals(t);
            case DECLINING -> "En baisse".equals(t);
            default -> true;
        };
    }

    private static LocalDateTime toLdt(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
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

    public static String initials(String first, String last) {
        String a = first != null && !first.isBlank() ? first.substring(0, 1) : "";
        String b = last != null && !last.isBlank() ? last.substring(0, 1) : "";
        return (a + b).toLowerCase(Locale.ROOT);
    }
}
