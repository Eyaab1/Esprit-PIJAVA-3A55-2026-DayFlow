package services.goals_routines;

import model.goals_activity_management.Activity;
import model.goals_activity_management.Routine;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ActivityService implements CRUD<Activity, Integer> {

    public record ActivityBarCounts(int completed, int remaining) {
    }

    private Connection cnx;
    private final RoutineService routineService = new RoutineService();
    private final GoalService goalService = new GoalService();

    public ActivityService() {
        cnx = DbConnexion.getInstance().getCnx();
    }

    @Override
    public void create(Activity a) throws SQLException {
        insert(a);
    }

    @Override
    public void insert(Activity a) throws SQLException {
        normalizeAndValidateForPersistence(a);
        alignCompletionTimestamp(a);

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

            ps.setTimestamp(8, a.getDeadline() != null ? Timestamp.valueOf(a.getDeadline()) : null);
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

        if (a.getRoutine() != null) {
            handleProgressRecalculation(a.getRoutine().getId());
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
        Timestamp dl = rs.getTimestamp("deadline");
        if (dl != null) {
            a.setDeadline(dl.toLocalDateTime());
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
        Integer previousRoutineId = findRoutineIdByActivityId(a.getId());
        normalizeAndValidateForPersistence(a);
        alignCompletionTimestamp(a);
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

            ps.setTimestamp(8, a.getDeadline() != null ? Timestamp.valueOf(a.getDeadline()) : null);
            ps.setBoolean(9, a.isFavorite());

            ps.setTimestamp(10, a.getCompletedAt() != null ? Timestamp.valueOf(a.getCompletedAt()) : null);
            ps.setObject(11, a.getActualDurationMinutes());
            ps.setObject(12, a.getPlannedDurationMinutes());

            ps.setTimestamp(13, Timestamp.valueOf(a.getUpdatedAt()));
            ps.setInt(14, a.getRoutine() != null ? a.getRoutine().getId() : 0);

            ps.setInt(15, a.getId());

            ps.executeUpdate();
        }

        if (previousRoutineId != null) {
            handleProgressRecalculation(previousRoutineId);
        }
        if (a.getRoutine() != null && (previousRoutineId == null || previousRoutineId != a.getRoutine().getId())) {
            handleProgressRecalculation(a.getRoutine().getId());
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        Integer routineId = findRoutineIdByActivityId(id);
        String sql = "DELETE FROM activity WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        if (routineId != null) {
            handleProgressRecalculation(routineId);
        }
    }

    public Activity findById(int id) throws SQLException {
        String sql = """
                SELECT id, title, start_time, duration, status, priority,
                       has_reminder, reminder_at, deadline, is_favorite,
                       completed_at, actual_duration_minutes, planned_duration_minutes,
                       created_at, updated_at, routine_id
                FROM activity
                WHERE id = ?
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapActivity(rs);
                }
            }
        }
        return null;
    }

    /**
     * Updates only activity status with transactional progress recalculation.
     */
    public void updateActivityStatus(int activityId, String newStatus) throws SQLException {
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Le statut de l'activité est obligatoire.");
        }
        String normalizedStatus = newStatus.trim().toLowerCase();
        Activity.validateStatus(normalizedStatus);

        boolean previousAutoCommit = cnx.getAutoCommit();
        try {
            cnx.setAutoCommit(false);

            Integer routineId = findRoutineIdByActivityId(activityId);
            if (routineId == null) {
                throw new IllegalArgumentException("Activité introuvable: " + activityId);
            }

            Timestamp completedAt = "completed".equals(normalizedStatus) ? Timestamp.valueOf(LocalDateTime.now()) : null;

            String sql = "UPDATE activity SET status = ?, completed_at = ?, updated_at = ? WHERE id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setString(1, normalizedStatus);
                ps.setTimestamp(2, completedAt);
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(4, activityId);
                ps.executeUpdate();
            }

            routineService.recalculateRoutineProgress(routineId);
            Integer goalId = routineService.findGoalIdByRoutineId(routineId);
            if (goalId != null) {
                goalService.recalculateGoalProgress(goalId);
            }

            cnx.commit();
        } catch (Exception e) {
            cnx.rollback();
            if (e instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw new SQLException("Erreur lors de la mise à jour du statut de l'activité.", e);
        } finally {
            cnx.setAutoCommit(previousAutoCommit);
        }
    }

    private Integer findRoutineIdByActivityId(int activityId) throws SQLException {
        String sql = "SELECT routine_id FROM activity WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("routine_id");
                }
            }
        }
        return null;
    }

    private void normalizeAndValidateForPersistence(Activity activity) {
        if (!activity.isHasReminder()) {
            activity.setReminderAt(null);
        }
        activity.validate();
    }

    private void alignCompletionTimestamp(Activity activity) {
        if ("completed".equalsIgnoreCase(activity.getStatus())) {
            if (activity.getCompletedAt() == null) {
                activity.setCompletedAt(LocalDateTime.now());
            }
        } else {
            activity.setCompletedAt(null);
        }
    }

    private void handleProgressRecalculation(int routineId) throws SQLException {
        routineService.recalculateRoutineProgress(routineId);
        Integer goalId = routineService.findGoalIdByRoutineId(routineId);
        if (goalId != null) {
            goalService.recalculateGoalProgress(goalId);
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
