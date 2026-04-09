package services.coaching_session_module;

import model.Session;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.util.Date;

public class SessionService implements CRUD<Session, Integer> {

    @Override
    public void create(Session s) throws SQLException {
        insert(s);
    }

    @Override
    public void insert(Session s) throws SQLException {

        String sql = "INSERT INTO session (" +
                "coaching_request_id, status, proposed_time_by_user, proposed_time_by_coach, " +
                "scheduled_at, duration, created_at, updated_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection cnx = DbConnexion.getConnection();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, s.getCoachingRequestId());
            ps.setString(2, s.getStatus());

            ps.setTimestamp(3, toTimestamp(s.getProposedTimeByUser()));
            ps.setTimestamp(4, toTimestamp(s.getProposedTimeByCoach()));
            ps.setTimestamp(5, toTimestamp(s.getScheduledAt()));

            ps.setObject(6, s.getDuration());

            ps.setTimestamp(7, toTimestamp(s.getCreatedAt()));
            ps.setTimestamp(8, toTimestamp(s.getUpdatedAt()));

            ps.executeUpdate();
        }
    }

    @Override
    public void update(Session s) throws SQLException {

        s.setUpdatedAt(new Date());

        String sql = "UPDATE session SET " +
                "coaching_request_id=?, status=?, proposed_time_by_user=?, proposed_time_by_coach=?, " +
                "scheduled_at=?, duration=?, updated_at=? WHERE id=?";

        Connection cnx = DbConnexion.getConnection();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, s.getCoachingRequestId());
            ps.setString(2, s.getStatus());

            ps.setTimestamp(3, toTimestamp(s.getProposedTimeByUser()));
            ps.setTimestamp(4, toTimestamp(s.getProposedTimeByCoach()));
            ps.setTimestamp(5, toTimestamp(s.getScheduledAt()));

            ps.setObject(6, s.getDuration());

            ps.setTimestamp(7, toTimestamp(s.getUpdatedAt()));
            ps.setInt(8, s.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {

        String sql = "DELETE FROM session WHERE id=?";

        Connection cnx = DbConnexion.getConnection();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private static Timestamp toTimestamp(Date d) {
        return d == null ? null : new Timestamp(d.getTime());
    }
}
