package services.Goal_acitvityManagment_module;

import model.goals_activity_management.Activity;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityService implements CRUD<Activity, Integer> {

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

        String sql = "INSERT INTO activity VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, a.getTitle());
        ps.setTimestamp(2, Timestamp.valueOf(a.getStartTime()));
        ps.setTime(3, Time.valueOf(a.getDuration()));
        ps.setString(4, a.getStatus());
        ps.setString(5, a.getPriority());

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
    }

    @Override
    public void update(Activity a) throws SQLException {

        a.onUpdate();

        String sql = "UPDATE activity SET title=?, start_time=?, duration=?, status=?, priority=?, has_reminder=?, reminder_at=?, deadline=?, is_favorite=?, completed_at=?, actual_duration_minutes=?, planned_duration_minutes=?, updated_at=?, routine_id=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, a.getTitle());
        ps.setTimestamp(2, Timestamp.valueOf(a.getStartTime()));
        ps.setTime(3, Time.valueOf(a.getDuration()));
        ps.setString(4, a.getStatus());
        ps.setString(5, a.getPriority());

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

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM activity WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public List<Activity> findByRoutineId(int routineId) throws SQLException {
        String sql = """
                SELECT id, title, start_time, duration, status, priority, has_reminder, 
                       reminder_at, deadline, is_favorite, completed_at, actual_duration_minutes, 
                       planned_duration_minutes, created_at, updated_at, routine_id
                FROM activity WHERE routine_id = ?
                ORDER BY start_time ASC
                """;
        List<Activity> activities = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, routineId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Activity activity = new Activity();
                    activity.setId(rs.getInt("id"));
                    activity.setTitle(rs.getString("title"));
                    
                    Timestamp startTime = rs.getTimestamp("start_time");
                    if (startTime != null) activity.setStartTime(startTime.toLocalDateTime());
                    
                    Time duration = rs.getTime("duration");
                    if (duration != null) activity.setDuration(duration.toLocalTime());
                    
                    activity.setStatus(rs.getString("status"));
                    activity.setPriority(rs.getString("priority"));
                    activity.setHasReminder(rs.getBoolean("has_reminder"));
                    
                    Timestamp reminderAt = rs.getTimestamp("reminder_at");
                    if (reminderAt != null) activity.setReminderAt(reminderAt.toLocalDateTime());
                    
                    Date deadline = rs.getDate("deadline");
                    if (deadline != null) activity.setDeadline(deadline.toLocalDate());
                    
                    activity.setFavorite(rs.getBoolean("is_favorite"));
                    
                    Timestamp completedAt = rs.getTimestamp("completed_at");
                    if (completedAt != null) activity.setCompletedAt(completedAt.toLocalDateTime());
                    
                    Integer actualDuration = (Integer) rs.getObject("actual_duration_minutes");
                    if (actualDuration != null) activity.setActualDurationMinutes(actualDuration);
                    
                    Integer plannedDuration = (Integer) rs.getObject("planned_duration_minutes");
                    if (plannedDuration != null) activity.setPlannedDurationMinutes(plannedDuration);
                    
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) activity.setUpdatedAt(updatedAt.toLocalDateTime());
                    
                    activities.add(activity);
                }
            }
        }
        return activities;
    }
}
