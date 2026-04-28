package services.chatroom;

import model.chatroom.Chatroom;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ChatroomService implements CRUD<Chatroom, Integer> {

    private final Connection cnx;

    public ChatroomService() {
        cnx = DbConnexion.getConnection();
    }

    @Override
    public void create(Chatroom chatroom) throws SQLException { insert(chatroom); }

    @Override
    public void insert(Chatroom chatroom) throws SQLException {
        String sql = "INSERT INTO chatroom (goal_id, state, created_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, chatroom.getGoalId());
            ps.setString(2, chatroom.getState());
            ps.setTimestamp(3, Timestamp.valueOf(chatroom.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) chatroom.setId(keys.getInt(1));
            }
        }
    }

    @Override
    public void update(Chatroom chatroom) throws SQLException {
        String sql = "UPDATE chatroom SET goal_id = ?, state = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, chatroom.getGoalId());
            ps.setString(2, chatroom.getState());
            ps.setInt(3, chatroom.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM chatroom WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Optional<Chatroom> findByGoalId(int goalId) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT id, goal_id, state, created_at FROM chatroom WHERE goal_id = ?")) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<Chatroom> findById(int id) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT id, goal_id, state, created_at FROM chatroom WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Chatroom> getAll() throws SQLException {
        List<Chatroom> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT id, goal_id, state, created_at FROM chatroom ORDER BY created_at DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── Record ────────────────────────────────────────────────────────────
    public record ChatroomListItem(
            int chatroomId,
            int goalId,
            String goalTitle,
            String lastMessageSnippet,
            int unreadCount) {}

    /**
     * Nom réel de la colonne « titre » sur {@code goal} (title, name, nom, etc.).
     * Évite l'erreur PostgreSQL « column g.title does not exist » si le schéma diffère.
     */
    private String resolveGoalTitleColumnRef(String alias) throws SQLException {
        List<String> columns = listTableColumns("goal");
        if (columns.isEmpty()) {
            columns = listTableColumns("Goal");
        }
        List<String> preferred = List.of("title", "titre", "name", "nom", "label", "designation", "description");
        for (String want : preferred) {
            for (String col : columns) {
                if (col != null && col.equalsIgnoreCase(want)) {
                    return alias + "." + pgIdent(col);
                }
            }
        }
        throw new SQLException(
                "Table goal : aucune colonne titre reconnue (essayé : title, titre, name, nom, label…). "
                        + "Colonnes trouvées : " + columns);
    }

    private List<String> listTableColumns(String table) throws SQLException {
        List<String> out = new ArrayList<>();
        DatabaseMetaData md = cnx.getMetaData();
        String schema = cnx.getSchema();
        if (schema != null) {
            try (ResultSet rs = md.getColumns(null, schema, table, null)) {
                while (rs.next()) {
                    out.add(rs.getString("COLUMN_NAME"));
                }
            }
        }
        if (out.isEmpty()) {
            try (ResultSet rs = md.getColumns(null, "public", table, null)) {
                while (rs.next()) {
                    out.add(rs.getString("COLUMN_NAME"));
                }
            }
        }
        if (out.isEmpty()) {
            try (ResultSet rs = md.getColumns(null, null, table, null)) {
                while (rs.next()) {
                    out.add(rs.getString("COLUMN_NAME"));
                }
            }
        }
        return out;
    }

    /** Identifiant PostgreSQL sûr pour une colonne simple ou entre guillemets. */
    private static String pgIdent(String col) {
        boolean simple = col.chars().allMatch(c ->
                c == '_' || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'));
        if (simple && col.toLowerCase(Locale.ROOT).equals(col)) {
            return col.toLowerCase(Locale.ROOT);
        }
        return "\"" + col.replace("\"", "\"\"") + "\"";
    }

    // ── findAccessibleForUser ─────────────────────────────────────────────
    public List<ChatroomListItem> findAccessibleForUser(int userId) throws SQLException {

        String titleRef = resolveGoalTitleColumnRef("g");
        String sql =
            "SELECT c.id AS cid, g.id AS gid, " + titleRef + " AS goal_title, " +
            "(SELECT m.content FROM message m " +
            " WHERE m.chatroom_id = c.id " +
            " ORDER BY m.created_at DESC NULLS LAST LIMIT 1) AS snippet, " +
            "(SELECT COUNT(*) FROM message m2 " +
            " WHERE m2.chatroom_id = c.id AND m2.author_id != ?) AS unread " +
            "FROM chatroom c " +
            "INNER JOIN goal g ON g.id = c.goal_id " +
            "INNER JOIN goal_participation gp ON gp.goal_id = g.id " +
            "WHERE gp.user_id = ? AND gp.status = 'APPROVED' " +
            "ORDER BY g.id DESC";

        List<ChatroomListItem> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String snip = rs.getString("snippet");
                    list.add(new ChatroomListItem(
                            rs.getInt("cid"),
                            rs.getInt("gid"),
                            rs.getString("goal_title"),
                            rs.wasNull() ? null : snip,
                            rs.getInt("unread")));
                }
            }
        }
        return list;
    }

    private static Chatroom mapRow(ResultSet rs) throws SQLException {
        Chatroom c = new Chatroom();
        c.setId(rs.getInt("id"));
        c.setGoalId(rs.getInt("goal_id"));
        c.setState(normalizeStateFromDb(rs.getString("state")));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) c.setCreatedAt(ts.toLocalDateTime());
        return c;
    }

    /**
     * Valeurs possibles côté Symfony / anciennes BDs : ACTIVE, open, null…
     * Le modèle Java n'accepte que {@code active} et {@code inactive}.
     */
    private static String normalizeStateFromDb(String raw) {
        if (raw == null || raw.isBlank()) {
            return "active";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if ("active".equals(s) || "open".equals(s) || "enabled".equals(s) || "1".equals(s)) {
            return "active";
        }
        if ("inactive".equals(s) || "closed".equals(s) || "locked".equals(s) || "archived".equals(s) || "0".equals(s)) {
            return "inactive";
        }
        return "active";
    }
}
