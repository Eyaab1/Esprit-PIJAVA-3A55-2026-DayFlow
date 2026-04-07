package services;

import model.Session;
import utils.DbConnexion;

import java.sql.*;

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

        try (Connection cnx = DbConnexion.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, s.getCoachingRequest().getId()); // ⚠️ or coachingRequestId
            ps.setString(2, s.getStatus());

            ps.setTimestamp(3, s.getProposedTimeByUser() != null ?
                    Timestamp.valueOf(s.getProposedTimeByUser()) : null);

            ps.setTimestamp(4, s.getProposedTimeByCoach() != null ?
                    Timestamp.valueOf(s.getProposedTimeByCoach()) : null);

            ps.setTimestamp(5, s.getScheduledAt() != null ?
                    Timestamp.valueOf(s.getScheduledAt()) : null);

            ps.setObject(6, s.getDuration());

            ps.setTimestamp(7, Timestamp.valueOf(s.getCreatedAt()));
            ps.setTimestamp(8, s.getUpdatedAt() != null ?
                    Timestamp.valueOf(s.getUpdatedAt()) : null);

            ps.executeUpdate();
        }
    }

    @Override
    public void update(Session s) throws SQLException {

        s.setUpdatedAt(java.time.LocalDateTime.now());

        String sql = "UPDATE session SET " +
                "coaching_request_id=?, status=?, proposed_time_by_user=?, proposed_time_by_coach=?, " +
                "scheduled_at=?, duration=?, updated_at=? WHERE id=?";

        try (Connection cnx = DbConnexion.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, s.getCoachingRequest().getId()); // ⚠️ or coachingRequestId
            ps.setString(2, s.getStatus());

            ps.setTimestamp(3, s.getProposedTimeByUser() != null ?
                    Timestamp.valueOf(s.getProposedTimeByUser()) : null);

            ps.setTimestamp(4, s.getProposedTimeByCoach() != null ?
                    Timestamp.valueOf(s.getProposedTimeByCoach()) : null);

            ps.setTimestamp(5, s.getScheduledAt() != null ?
                    Timestamp.valueOf(s.getScheduledAt()) : null);

            ps.setObject(6, s.getDuration());

            ps.setTimestamp(7, Timestamp.valueOf(s.getUpdatedAt()));
            ps.setInt(8, s.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {

        String sql = "DELETE FROM session WHERE id=?";

        try (Connection cnx = DbConnexion.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
