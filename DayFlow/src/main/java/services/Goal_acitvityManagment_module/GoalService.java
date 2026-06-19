package services.Goal_acitvityManagment_module;

import model.goals_activity_management.Goal;
import model.user.User;
import services.CRUD;
import session.AppSession;
import utils.DbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service CRUD pour la table goal.
 * Colonnes réelles en BD : id, description, start_date, end_date, status,
 *                          created_at, updated_at, user_id
 */
public class GoalService implements CRUD<Goal, Integer> {

    private final Connection cnx;

    public GoalService() {
        cnx = DbConnexion.getConnection();
    }

    @Override
    public void create(Goal goal) throws SQLException { insert(goal); }

    @Override
    public void insert(Goal goal) throws SQLException {
        // Résoudre user_id : depuis l'objet User lié, ou depuis AppSession
        int uid = 0;
        if (goal.getUser() != null && goal.getUser().getId() != null) {
            uid = goal.getUser().getId();
        } else {
            uid = AppSession.getCurrentUser()
                    .map(User::getId)
                    .orElse(0);
        }
        if (uid <= 0) {
            throw new SQLException("Utilisateur non connecté — impossible de créer un objectif.");
        }

        String sql = "INSERT INTO goal (description, start_date, end_date, status, created_at, updated_at, user_id) " +
                     "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, goal.getDescription());
            ps.setDate(2, goal.getStartDate() != null ? Date.valueOf(goal.getStartDate()) : null);
            ps.setDate(3, goal.getEndDate()   != null ? Date.valueOf(goal.getEndDate())   : null);
            ps.setString(4, goal.getStatus() != null ? goal.getStatus() : "active");
            ps.setInt(5, uid);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) goal.setId(keys.getInt(1));
            }
        }
    }

    @Override
    public void update(Goal goal) throws SQLException {
        String sql = "UPDATE goal SET description=?, start_date=?, end_date=?, status=?, " +
                     "updated_at=CURRENT_TIMESTAMP, user_id=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, goal.getDescription());
            ps.setDate(2, goal.getStartDate() != null ? Date.valueOf(goal.getStartDate()) : null);
            ps.setDate(3, goal.getEndDate()   != null ? Date.valueOf(goal.getEndDate())   : null);
            ps.setString(4, goal.getStatus() != null ? goal.getStatus() : "active");
            int uid = goal.getUser() != null && goal.getUser().getId() != null
                    ? goal.getUser().getId() : 0;
            ps.setInt(5, uid);
            ps.setInt(6, goal.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM goal WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Goal findById(int id) throws SQLException {
        String sql = "SELECT id, description, start_date, end_date, status, created_at, updated_at, user_id " +
                     "FROM goal WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapGoal(rs);
            }
        }
        return null;
    }

    public List<Goal> findAll() throws SQLException {
        String sql = "SELECT id, description, start_date, end_date, status, created_at, updated_at, user_id " +
                     "FROM goal ORDER BY created_at DESC";
        List<Goal> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapGoal(rs));
        }
        return list;
    }

    public List<Goal> findByUserId(int userId) throws SQLException {
        String sql = "SELECT id, description, start_date, end_date, status, created_at, updated_at, user_id " +
                     "FROM goal WHERE user_id = ? ORDER BY created_at DESC";
        List<Goal> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapGoal(rs));
            }
        }
        return list;
    }

    /** Dashboard : goals + propriétaire + membres + chatroom */
    public List<GoalListRow> findAllForDashboard() throws SQLException {
        String sql =
            "SELECT g.id, g.description, g.start_date, g.end_date, g.status, " +
            "       g.created_at, g.updated_at, g.user_id, " +
            "       u.first_name AS owner_fn, u.last_name AS owner_ln, " +
            "       c.id AS chatroom_id, " +
            "       COALESCE(pc.cnt, 0) AS participant_count " +
            "FROM goal g " +
            "LEFT JOIN goal_participation owner_gp ON owner_gp.goal_id = g.id AND owner_gp.role = 'OWNER' " +
            "LEFT JOIN \"user\" u ON u.id = owner_gp.user_id " +
            "LEFT JOIN chatroom c ON c.goal_id = g.id " +
            "LEFT JOIN (SELECT goal_id, COUNT(*)::int AS cnt FROM goal_participation " +
            "           WHERE status = 'APPROVED' GROUP BY goal_id) pc ON pc.goal_id = g.id " +
            "ORDER BY g.created_at DESC";

        List<GoalListRow> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Goal g = mapGoal(rs);
                String fn = rs.getString("owner_fn");
                String ln = rs.getString("owner_ln");
                int chatId = rs.getInt("chatroom_id");
                Integer chatroomId = rs.wasNull() ? null : chatId;
                int part = rs.getInt("participant_count");
                list.add(new GoalListRow(g, fn, ln, part, chatroomId));
            }
        }
        return list;
    }

    public GoalStatusCounts countGoalsByStatus() throws SQLException {
        String sql = "SELECT LOWER(TRIM(status)) AS st, COUNT(*)::int AS cnt FROM goal GROUP BY LOWER(TRIM(status))";
        Map<String, Integer> m = new HashMap<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) m.put(rs.getString("st"), rs.getInt("cnt"));
        }
        int active    = m.getOrDefault("active",    0);
        int completed = m.getOrDefault("completed", 0);
        int paused    = m.getOrDefault("paused",    0);
        int failed    = m.getOrDefault("failed",    0);
        int draft     = m.getOrDefault("draft",     0);
        int archived  = m.getOrDefault("archived",  0);
        return new GoalStatusCounts(active, completed, paused, failed, draft, archived,
                active + completed + paused + failed + draft + archived);
    }

    // ── Mapping ───────────────────────────────────────────────────────────

    private static Goal mapGoal(ResultSet rs) throws SQLException {
        Goal g = new Goal();
        g.setId(rs.getInt("id"));
        g.setDescription(rs.getString("description"));
        // title = description (la BD n'a pas de colonne title séparée)
        String desc = rs.getString("description");
        g.setTitleDirect(desc != null ? desc : "—");
        Date sd = rs.getDate("start_date");
        g.setStartDate(sd != null ? sd.toLocalDate() : null);
        Date ed = rs.getDate("end_date");
        g.setEndDate(ed != null ? ed.toLocalDate() : null);
        g.setStatus(rs.getString("status"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) g.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) g.setUpdatedAt(ua.toLocalDateTime());
        try { 
            int uid = rs.getInt("user_id");
            if (!rs.wasNull() && g.getUser() == null) {
                User u = new User();
                u.setId(uid);
                g.setUser(u);
            }
        } catch (SQLException ignored) {}
        return g;
    }

    // ── Records ───────────────────────────────────────────────────────────

    public record GoalListRow(Goal goal, String ownerFirstName, String ownerLastName,
                              int participantCount, Integer chatroomId) {}

    public record GoalStatusCounts(int active, int completed, int paused, int failed,
                                   int draft, int archived, int total) {}
}
