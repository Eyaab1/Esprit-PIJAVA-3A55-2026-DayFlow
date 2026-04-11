package services.Goal_acitvityManagment_module;

import model.goals_activity_management.Goal;
import services.CRUD;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoalService implements CRUD<Goal, Integer> {

    private Connection cnx;

    private static final String INSERT_GOAL = """
            INSERT INTO goal (
                title, description, start_date, end_date, deadline, status, priority,
                is_favorite, progress, required_tasks, trello_board_id, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_GOAL = """
            UPDATE goal SET
                title=?, description=?, start_date=?, end_date=?, deadline=?, status=?, priority=?,
                is_favorite=?, progress=?, required_tasks=?, trello_board_id=?, updated_at=?
            WHERE id=?
            """;

    public GoalService() {
        cnx = DbConnexion.getInstance().getCnx();
    }

    @Override
    public void create(Goal goal) throws SQLException {
        insert(goal);
    }

    @Override
    public void insert(Goal goal) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(INSERT_GOAL, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setString(i++, goal.getTitle());
            ps.setString(i++, goal.getDescription());
            ps.setDate(i++, goal.getStartDate() != null ? Date.valueOf(goal.getStartDate()) : null);
            ps.setDate(i++, goal.getEndDate() != null ? Date.valueOf(goal.getEndDate()) : null);
            ps.setDate(i++, goal.getDeadline() != null ? Date.valueOf(goal.getDeadline()) : null);
            ps.setString(i++, goal.getStatus());
            if (goal.getPriority() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, goal.getPriority());
            }
            ps.setBoolean(i++, goal.isFavorite());
            ps.setInt(i++, goal.getProgress());
            if (goal.getRequiredTasks() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, goal.getRequiredTasks());
            }
            if (goal.getTrelloBoardId() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, goal.getTrelloBoardId());
            }
            ps.setTimestamp(i++, goal.getCreatedAt() != null ? Timestamp.valueOf(goal.getCreatedAt()) : null);
            ps.setTimestamp(i++, goal.getUpdatedAt() != null ? Timestamp.valueOf(goal.getUpdatedAt()) : null);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    goal.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Goal goal) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(UPDATE_GOAL)) {
            int i = 1;
            ps.setString(i++, goal.getTitle());
            ps.setString(i++, goal.getDescription());
            ps.setDate(i++, goal.getStartDate() != null ? Date.valueOf(goal.getStartDate()) : null);
            ps.setDate(i++, goal.getEndDate() != null ? Date.valueOf(goal.getEndDate()) : null);
            ps.setDate(i++, goal.getDeadline() != null ? Date.valueOf(goal.getDeadline()) : null);
            ps.setString(i++, goal.getStatus());
            if (goal.getPriority() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, goal.getPriority());
            }
            ps.setBoolean(i++, goal.isFavorite());
            ps.setInt(i++, goal.getProgress());
            if (goal.getRequiredTasks() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, goal.getRequiredTasks());
            }
            if (goal.getTrelloBoardId() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, goal.getTrelloBoardId());
            }
            ps.setTimestamp(i++, goal.getUpdatedAt() != null ? Timestamp.valueOf(goal.getUpdatedAt()) : null);
            ps.setInt(i, goal.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM goal WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Goal findById(int id) throws SQLException {
        String sql = """
                SELECT id, title, description, start_date, end_date, deadline, status, priority,
                       is_favorite, progress, required_tasks, trello_board_id, created_at, updated_at
                FROM goal WHERE id = ?
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapGoal(rs);
                }
            }
        }
        return null;
    }

    /**
     * Liste des goals avec propriétaire (OWNER), nombre de participants approuvés et id chatroom.
     */
    public List<GoalListRow> findAllForDashboard() throws SQLException {
        String sql = """
                SELECT g.id, g.title, g.description, g.start_date, g.end_date, g.deadline, g.status, g.priority,
                       g.is_favorite, g.progress, g.required_tasks, g.trello_board_id, g.created_at, g.updated_at,
                       u.first_name AS owner_fn, u.last_name AS owner_ln,
                       c.id AS chatroom_id,
                       COALESCE(pc.cnt, 0) AS participant_count
                FROM goal g
                LEFT JOIN goal_participation owner_gp ON owner_gp.goal_id = g.id AND owner_gp.role = 'OWNER'
                LEFT JOIN "user" u ON u.id = owner_gp.user_id
                LEFT JOIN chatroom c ON c.goal_id = g.id
                LEFT JOIN (
                    SELECT goal_id, COUNT(*)::int AS cnt
                    FROM goal_participation
                    WHERE status = 'APPROVED'
                    GROUP BY goal_id
                ) pc ON pc.goal_id = g.id
                ORDER BY g.created_at DESC
                """;
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

    private static Goal mapGoal(ResultSet rs) throws SQLException {
        Goal g = new Goal();
        g.setId(rs.getInt("id"));
        g.setTitle(rs.getString("title"));
        g.setDescription(rs.getString("description"));
        Date sd = rs.getDate("start_date");
        g.setStartDate(sd != null ? sd.toLocalDate() : null);
        Date ed = rs.getDate("end_date");
        g.setEndDate(ed != null ? ed.toLocalDate() : null);
        Date dl = rs.getDate("deadline");
        g.setDeadline(dl != null ? dl.toLocalDate() : null);
        g.setStatus(rs.getString("status"));
        g.setPriority(rs.getString("priority"));
        g.setFavorite(rs.getBoolean("is_favorite"));
        g.setProgress(rs.getInt("progress"));
        int rt = rs.getInt("required_tasks");
        g.setRequiredTasks(rs.wasNull() ? null : rt);
        g.setTrelloBoardId(rs.getString("trello_board_id"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) {
            g.setCreatedAt(ca.toLocalDateTime());
        }
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) {
            g.setUpdatedAt(ua.toLocalDateTime());
        }
        return g;
    }

    public record GoalListRow(Goal goal, String ownerFirstName, String ownerLastName,
                              int participantCount, Integer chatroomId) {
    }

    /**
     * Compteurs globaux pour le tableau de bord (tous les objectifs en base).
     */
    public record GoalStatusCounts(int active, int completed, int paused, int failed, int draft, int archived,
                                   int total) {
    }

    public GoalStatusCounts countGoalsByStatus() throws SQLException {
        String sql = """
                SELECT LOWER(TRIM(status)) AS st, COUNT(*)::int AS cnt
                FROM goal
                GROUP BY LOWER(TRIM(status))
                """;
        Map<String, Integer> m = new HashMap<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.put(rs.getString("st"), rs.getInt("cnt"));
            }
        }
        int active = m.getOrDefault("active", 0);
        int completed = m.getOrDefault("completed", 0);
        int paused = m.getOrDefault("paused", 0);
        int failed = m.getOrDefault("failed", 0);
        int draft = m.getOrDefault("draft", 0);
        int archived = m.getOrDefault("archived", 0);
        int total = active + completed + paused + failed + draft + archived;
        return new GoalStatusCounts(active, completed, paused, failed, draft, archived, total);
    }
}
