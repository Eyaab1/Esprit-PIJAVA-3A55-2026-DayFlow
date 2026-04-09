package services;

import model.Activity;
import utils.DbConnexion;

import java.sql.*;

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
}