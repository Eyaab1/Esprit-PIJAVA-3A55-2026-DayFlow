package services.goals_routines;

import model.goals_activity_management.Activity;
import model.goals_activity_management.Routine;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityService implements CRUD<Activity, Integer> {

    public record ActivityBarCounts(int completed, int remaining) {
    }

    private Connection cnx;

    public ActivityService() {
        cnx = DbConnexion.getInstance().getCnx();
    }

    @Override
    public void create(Activity a) throws SQLException {
        insert(a);
    }

    @Override
    public void insert(Activity a) throws SQLException {

        String sql = """
                INSERT INTO activity (
                    title, start_time, duration, status, priority,
                    has_reminder, reminder_at, deadline, is_favorite,
                    completed_at, actual_duration_minutes, planned_duration_minutes,
                    created_at, updated_at, routine_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, a.getTitle());
            ps.setTimestamp(2, Timestamp.valueOf(a.getStartTime()));
            ps.setTime(3, Time.valueOf(a.getDuration()));
            ps.setString(4, a.getStatus());
            if (a.getPriority() != null) {
                ps.setString(5, a.getPriority());
            } else {
                ps.setNull(5, Types.VARCHAR);
            }

            ps.setBoolean(6, a.isHasReminder());
            ps.setTimestamp(7, a.getReminderAt() != null ? Timestamp.valueOf(a.getReminderAt()) : null);

            ps.setDate(8, a.getDeadline() != null ? Date.valueOf(a.getDeadline()) : null);
            ps.setBoolean(9, a.isFavorite());

            ps.setTimestamp(10, a.getCompletedAt() != null ? Timestamp.valueOf(a.getCompletedAt()) : null);
            ps.setObject(11, a.getActualDurationMinutes());
            ps.setObject(12, a.getPlannedDurationMinutes());

            ps.setTimestamp(13, Timestamp.valueOf(a.getCreatedAt()));
            ps.setTimestamp(14, a.getUpdatedAt() != null ? Timestamp.valueOf(a.getUpdatedAt()) : null);

            ps.setInt(15, a.getRoutine() != null ? a.getRoutine().getId() : 0);

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    a.setId(keys.getInt(1));
                }
            }
        }
    }

    public List<Activity> findByRoutineId(int routineId) throws SQLException {
        String sql = """
                SELECT id, title, start_time, duration, status, priority,
                       has_reminder, reminder_at, deadline, is_favorite,
                       completed_at, actual_duration_minutes, planned_duration_minutes,
                       created_at, updated_at, routine_id
                FROM activity
                WHERE routine_id = ?
                ORDER BY start_time ASC
                """;
        List<Activity> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, routineId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapActivity(rs));
                }
            }
        }
        return list;
    }

    public void deleteByRoutineId(int routineId) throws SQLException {
        String sql = "DELETE FROM activity WHERE routine_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, routineId);
            ps.executeUpdate();
        }
    }

    private static Activity mapActivity(ResultSet rs) throws SQLException {
        Activity a = new Activity();
        a.setId(rs.getInt("id"));
        a.setTitle(rs.getString("title"));
        Timestamp st = rs.getTimestamp("start_time");
        if (st != null) {
            a.setStartTime(st.toLocalDateTime());
        }
        Time dur = rs.getTime("duration");
        if (dur != null) {
            a.setDuration(dur.toLocalTime());
        }
        a.setStatus(rs.getString("status"));
        String pr = rs.getString("priority");
        if (pr != null) {
            a.setPriority(pr);
        }
        Timestamp rem = rs.getTimestamp("reminder_at");
        if (rem != null) {
            a.setReminderAt(rem.toLocalDateTime());
        }
        a.setHasReminder(rs.getBoolean("has_reminder"));
        Date dl = rs.getDate("deadline");
        if (dl != null) {
            a.setDeadline(dl.toLocalDate());
        }
        a.setFavorite(rs.getBoolean("is_favorite"));
        Timestamp comp = rs.getTimestamp("completed_at");
        if (comp != null) {
            a.setCompletedAt(comp.toLocalDateTime());
        }
        int act = rs.getInt("actual_duration_minutes");
        if (!rs.wasNull()) {
            a.setActualDurationMinutes(act);
        }
        int plan = rs.getInt("planned_duration_minutes");
        if (!rs.wasNull()) {
            a.setPlannedDurationMinutes(plan);
        }
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) {
            a.setCreatedAt(ca.toLocalDateTime());
        }
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) {
            a.setUpdatedAt(ua.toLocalDateTime());
        }
        Routine stub = new Routine();
        stub.setId(rs.getInt("routine_id"));
        a.setRoutine(stub);
        return a;
    }

    @Override
    public void update(Activity a) throws SQLException {

        a.onUpdate();

        String sql = "UPDATE activity SET title=?, start_time=?, duration=?, status=?, priority=?, has_reminder=?, reminder_at=?, deadline=?, is_favorite=?, completed_at=?, actual_duration_minutes=?, planned_duration_minutes=?, updated_at=?, routine_id=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, a.getTitle());
            ps.setTimestamp(2, Timestamp.valueOf(a.getStartTime()));
            ps.setTime(3, Time.valueOf(a.getDuration()));
            ps.setString(4, a.getStatus());
            if (a.getPriority() != null) {
                ps.setString(5, a.getPriority());
            } else {
                ps.setNull(5, Types.VARCHAR);
            }

            ps.setBoolean(6, a.isHasReminder());
            ps.setTimestamp(7, a.getReminderAt() != null ? Timestamp.valueOf(a.getReminderAt()) : null);

            ps.setDate(8, a.getDeadline() != null ? Date.valueOf(a.getDeadline()) : null);
            ps.setBoolean(9, a.isFavorite());

            ps.setTimestamp(10, a.getCompletedAt() != null ? Timestamp.valueOf(a.getCompletedAt()) : null);
            ps.setObject(11, a.getActualDurationMinutes());
            ps.setObject(12, a.getPlannedDurationMinutes());

            ps.setTimestamp(13, Timestamp.valueOf(a.getUpdatedAt()));
            ps.setInt(14, a.getRoutine() != null ? a.getRoutine().getId() : 0);

            ps.setInt(15, a.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM activity WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Activités liées aux routines d'un objectif : terminées vs restantes.
     */
    public ActivityBarCounts countForGoal(int goalId) throws SQLException {
        String sql = """
                SELECT
                  COALESCE(SUM(CASE WHEN LOWER(TRIM(a.status)) = 'completed' THEN 1 ELSE 0 END), 0)::int AS done_cnt,
                  COALESCE(SUM(CASE WHEN LOWER(TRIM(a.status)) IS DISTINCT FROM 'completed' THEN 1 ELSE 0 END), 0)::int AS rest_cnt
                FROM activity a
                INNER JOIN routine r ON r.id = a.routine_id
                WHERE r.goal_id = ?
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ActivityBarCounts(rs.getInt("done_cnt"), rs.getInt("rest_cnt"));
                }
            }
        }
        return new ActivityBarCounts(0, 0);
    }
}
