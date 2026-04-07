package services;

import model.Activity;
import utils.DbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ActivityService implements CRUD<Activity, Integer> {

    private static final String CREATE_ACTIVITY = """
            INSERT INTO activity (title, start_time, duration, status, has_reminder,
                                  reminder_at, priority, deadline, is_favorite,
                                  completed_at, actual_duration_minutes,
                                  planned_duration_minutes, created_at, routine_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_ACTIVITY = """
            UPDATE activity SET
                title = ?, start_time = ?, duration = ?, status = ?, has_reminder = ?,
                reminder_at = ?, priority = ?, deadline = ?, is_favorite = ?,
                completed_at = ?, actual_duration_minutes = ?,
                planned_duration_minutes = ?, updated_at = ?
            WHERE id = ?
            """;

    private static final String DELETE_ACTIVITY = """
            DELETE FROM activity WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, title, start_time, duration, status, has_reminder, reminder_at,
                   priority, deadline, is_favorite, completed_at,
                   actual_duration_minutes, planned_duration_minutes,
                   created_at, updated_at, routine_id
            FROM activity
            """;

    private static final String SELECT_BY_ID      = SELECT_ALL + " WHERE id = ?";
    private static final String SELECT_BY_ROUTINE = SELECT_ALL + " WHERE routine_id = ?";

    // ─── CRUD ─────────────────────────────────────────────────

    @Override
    public void create(Activity activity) throws SQLException {
        insert(activity);
    }

    @Override
    public void insert(Activity activity) throws SQLException {
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(CREATE_ACTIVITY, Statement.RETURN_GENERATED_KEYS)) {

            int i = 1;
            ps.setString(i++, activity.getTitle());
            ps.setTimestamp(i++, Timestamp.valueOf(activity.getStartTime()));
            ps.setTime(i++, Time.valueOf(activity.getDuration()));
            ps.setString(i++, activity.getStatus());
            ps.setBoolean(i++, activity.isHasReminder());
            setNullableTimestamp(ps, i++, activity.getReminderAt());
            setNullableString(ps, i++, activity.getPriority());
            setNullableDate(ps, i++, activity.getDeadline());
            ps.setBoolean(i++, activity.isFavorite());
            setNullableTimestamp(ps, i++, activity.getCompletedAt());
            setNullableInt(ps, i++, activity.getActualDurationMinutes());
            setNullableInt(ps, i++, activity.getPlannedDurationMinutes());
            ps.setTimestamp(i++, Timestamp.valueOf(activity.getCreatedAt()));
            ps.setInt(i, activity.getRoutine().getId());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) activity.setId(keys.getInt(1));
            }
        }
    }

    @Override
    public void update(Activity activity) throws SQLException {
        if (activity.getId() == 0)
            throw new SQLException("id obligatoire pour UPDATE");

        activity.onUpdate();

        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE_ACTIVITY)) {

            int i = 1;
            ps.setString(i++, activity.getTitle());
            ps.setTimestamp(i++, Timestamp.valueOf(activity.getStartTime()));
            ps.setTime(i++, Time.valueOf(activity.getDuration()));
            ps.setString(i++, activity.getStatus());
            ps.setBoolean(i++, activity.isHasReminder());
            setNullableTimestamp(ps, i++, activity.getReminderAt());
            setNullableString(ps, i++, activity.getPriority());
            setNullableDate(ps, i++, activity.getDeadline());
            ps.setBoolean(i++, activity.isFavorite());
            setNullableTimestamp(ps, i++, activity.getCompletedAt());
            setNullableInt(ps, i++, activity.getActualDurationMinutes());
            setNullableInt(ps, i++, activity.getPlannedDurationMinutes());
            ps.setTimestamp(i++, Timestamp.valueOf(activity.getUpdatedAt()));
            ps.setInt(i, activity.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null)
            throw new SQLException("id obligatoire pour DELETE");

        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE_ACTIVITY)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ─── Queries ──────────────────────────────────────────────

    public Optional<Activity> findById(int id) throws SQLException {
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Activity> findAll() throws SQLException {
        List<Activity> list = new ArrayList<>();
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Activity> findByRoutine(int routineId) throws SQLException {
        List<Activity> list = new ArrayList<>();
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ROUTINE)) {
            ps.setInt(1, routineId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ─── Row mapper ───────────────────────────────────────────

    private Activity mapRow(ResultSet rs) throws SQLException {
        Activity a = new Activity();
        a.setId(rs.getInt("id"));
        a.setTitle(rs.getString("title"));
        a.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        a.setDuration(rs.getTime("duration").toLocalTime());
        a.setStatus(rs.getString("status"));
        a.setHasReminder(rs.getBoolean("has_reminder"));

        Timestamp reminderAt = rs.getTimestamp("reminder_at");
        a.setReminderAt(reminderAt != null ? reminderAt.toLocalDateTime() : null);

        a.setPriority(rs.getString("priority"));

        Date deadline = rs.getDate("deadline");
        a.setDeadline(deadline != null ? deadline.toLocalDate() : null);

        a.setFavorite(rs.getBoolean("is_favorite"));

        Timestamp completedAt = rs.getTimestamp("completed_at");
        a.setCompletedAt(completedAt != null ? completedAt.toLocalDateTime() : null);

        int actual = rs.getInt("actual_duration_minutes");
        a.setActualDurationMinutes(rs.wasNull() ? null : actual);

        int planned = rs.getInt("planned_duration_minutes");
        a.setPlannedDurationMinutes(rs.wasNull() ? null : planned);

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) a.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        a.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);

        return a;
    }

    // ─── Helpers ──────────────────────────────────────────────

    private void setNullableString(PreparedStatement ps, int i, String val) throws SQLException {
        if (val == null) ps.setNull(i, Types.VARCHAR);
        else             ps.setString(i, val);
    }

    private void setNullableInt(PreparedStatement ps, int i, Integer val) throws SQLException {
        if (val == null) ps.setNull(i, Types.INTEGER);
        else             ps.setInt(i, val);
    }

    private void setNullableDate(PreparedStatement ps, int i, java.time.LocalDate val) throws SQLException {
        if (val == null) ps.setNull(i, Types.DATE);
        else             ps.setDate(i, Date.valueOf(val));
    }

    private void setNullableTimestamp(PreparedStatement ps, int i, java.time.LocalDateTime val) throws SQLException {
        if (val == null) ps.setNull(i, Types.TIMESTAMP);
        else             ps.setTimestamp(i, Timestamp.valueOf(val));
    }
}