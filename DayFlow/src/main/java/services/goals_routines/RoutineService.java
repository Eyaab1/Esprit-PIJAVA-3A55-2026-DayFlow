package services.goals_routines;

import model.goals_activity_management.Goal;
import model.goals_activity_management.Routine;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RoutineService implements CRUD<Routine, Integer> {

    private Connection cnx;

    public RoutineService() {
        cnx = DbConnexion.getInstance().getCnx();
    }

    @Override
    public void create(Routine r) throws SQLException {
        insert(r);
    }

    @Override
    public void insert(Routine r) throws SQLException {

        String sql = """
                INSERT INTO routine (
                    title, description, visibility, status, priority,
                    deadline, is_favorite, created_at, updated_at, goal_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, r.getTitle());
            if (r.getDescription() != null) {
                ps.setString(2, r.getDescription());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setString(3, r.getVisibility());
            ps.setString(4, r.getStatus());
            if (r.getPriority() != null) {
                ps.setString(5, r.getPriority());
            } else {
                ps.setNull(5, Types.VARCHAR);
            }

            ps.setTimestamp(6, r.getDeadline() != null ? Timestamp.valueOf(r.getDeadline()) : null);
            ps.setBoolean(7, r.isFavorite());

            ps.setTimestamp(8, Timestamp.valueOf(r.getCreatedAt()));
            ps.setTimestamp(9, r.getUpdatedAt() != null ? Timestamp.valueOf(r.getUpdatedAt()) : null);

            ps.setInt(10, r.getGoal() != null ? r.getGoal().getId() : 0);

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    r.setId(keys.getInt(1));
                }
            }
        }
    }

    public List<Routine> findByGoalId(int goalId) throws SQLException {
        String sql = """
                SELECT id, title, description, visibility, status, priority, deadline,
                       is_favorite, created_at, updated_at, goal_id
                FROM routine
                WHERE goal_id = ?
                ORDER BY created_at DESC
                """;
        List<Routine> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRoutine(rs));
                }
            }
        }
        return list;
    }

    /**
     * Pour le graphique « routines » : terminées vs en cours (tout sauf completed).
     */
    public RoutineProgressCounts countProgressForGoal(int goalId) throws SQLException {
        String sql = """
                SELECT
                  COALESCE(SUM(CASE WHEN LOWER(TRIM(status)) = 'completed' THEN 1 ELSE 0 END), 0)::int AS done_cnt,
                  COALESCE(SUM(CASE WHEN LOWER(TRIM(status)) <> 'completed' THEN 1 ELSE 0 END), 0)::int AS prog_cnt
                FROM routine
                WHERE goal_id = ?
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new RoutineProgressCounts(rs.getInt("done_cnt"), rs.getInt("prog_cnt"));
                }
            }
        }
        return new RoutineProgressCounts(0, 0);
    }

    public record RoutineProgressCounts(int completed, int inProgress) {
    }

    public Routine findById(int id) throws SQLException {
        String sql = """
                SELECT id, title, description, visibility, status, priority, deadline,
                       is_favorite, created_at, updated_at, goal_id
                FROM routine WHERE id = ?
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRoutine(rs);
                }
            }
        }
        return null;
    }

    public Integer findGoalIdByRoutineId(int routineId) throws SQLException {
        String sql = "SELECT goal_id FROM routine WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, routineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("goal_id");
                }
            }
        }
        return null;
    }

    /**
     * Recalculates routine progress based on completed activities and keeps routine status aligned.
     * Formula: (completed / total) * 100.
     */
    public int recalculateRoutineProgress(int routineId) throws SQLException {
        String statsSql = """
                SELECT
                    COUNT(*)::int AS total_count,
                    COALESCE(SUM(CASE WHEN LOWER(TRIM(status)) = 'completed' THEN 1 ELSE 0 END), 0)::int AS completed_count
                FROM activity
                WHERE routine_id = ?
                """;

        int total = 0;
        int completed = 0;
        try (PreparedStatement ps = cnx.prepareStatement(statsSql)) {
            ps.setInt(1, routineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt("total_count");
                    completed = rs.getInt("completed_count");
                }
            }
        }

        int progress = total == 0 ? 0 : Math.round((completed * 100.0f) / total);

        String currentStatus = null;
        String statusSql = "SELECT status FROM routine WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(statusSql)) {
            ps.setInt(1, routineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    currentStatus = rs.getString("status");
                }
            }
        }

        String newStatus = currentStatus;
        if (progress == 100) {
            newStatus = "completed";
        } else if ("completed".equalsIgnoreCase(currentStatus)) {
            newStatus = "active";
        }

        String updateSql = "UPDATE routine SET status = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(updateSql)) {
            ps.setString(1, newStatus);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, routineId);
            ps.executeUpdate();
        }

        return progress;
    }

    private static Routine mapRoutine(ResultSet rs) throws SQLException {
        Routine r = new Routine();
        r.setId(rs.getInt("id"));
        r.setTitle(rs.getString("title"));
        String desc = rs.getString("description");
        if (desc != null && !desc.isBlank()) {
            r.setDescription(desc);
        }
        r.setVisibility(rs.getString("visibility"));
        r.setStatus(rs.getString("status"));
        String pr = rs.getString("priority");
        if (pr != null) {
            r.setPriority(pr);
        }
        Timestamp dl = rs.getTimestamp("deadline");
        if (dl != null) {
            r.setDeadline(dl.toLocalDateTime());
        }
        r.setFavorite(rs.getBoolean("is_favorite"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) {
            r.setCreatedAt(ca.toLocalDateTime());
        }
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) {
            r.setUpdatedAt(ua.toLocalDateTime());
        }
        Goal g = new Goal();
        g.setId(rs.getInt("goal_id"));
        r.setGoal(g);
        return r;
    }

    @Override
    public void update(Routine r) throws SQLException {

        r.onUpdate();

        String sql = "UPDATE routine SET title=?, description=?, visibility=?, status=?, priority=?, deadline=?, is_favorite=?, updated_at=?, goal_id=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, r.getTitle());
            if (r.getDescription() != null) {
                ps.setString(2, r.getDescription());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setString(3, r.getVisibility());
            ps.setString(4, r.getStatus());
            if (r.getPriority() != null) {
                ps.setString(5, r.getPriority());
            } else {
                ps.setNull(5, Types.VARCHAR);
            }

            ps.setTimestamp(6, r.getDeadline() != null ? Timestamp.valueOf(r.getDeadline()) : null);
            ps.setBoolean(7, r.isFavorite());

            ps.setTimestamp(8, Timestamp.valueOf(r.getUpdatedAt()));
            ps.setInt(9, r.getGoal() != null ? r.getGoal().getId() : 0);

            ps.setInt(10, r.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM routine WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
