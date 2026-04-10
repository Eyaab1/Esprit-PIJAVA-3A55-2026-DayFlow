
package services.coaching_session_module;

import model.coaching_session.CoachingRequest;
import services.CRUD;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CoachingRequestService implements CRUD<CoachingRequest, Integer> {

    private Connection cnx;

    public CoachingRequestService() {
        cnx = DbConnexion.getInstance().getCnx();
    }

    @Override
    public void create(CoachingRequest entity) throws SQLException {
        insert(entity);
    }

    @Override
    public void insert(CoachingRequest r) throws SQLException {
        String sql = "INSERT INTO coaching_request (user_id, coach_id, message, status, created_at, goal, level, frequency, budget, coaching_type, priority) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, r.getUserId());
        ps.setInt(2, r.getCoachId());
        ps.setString(3, r.getMessage());
        ps.setString(4, r.getStatus());
        ps.setDate(5, new java.sql.Date(r.getCreatedAt().getTime()));
        ps.setString(6, r.getGoal());
        ps.setString(7, r.getLevel());
        ps.setString(8, r.getFrequency());
        ps.setObject(9, r.getBudget());
        ps.setString(10, r.getCoachingType());
        ps.setString(11, r.getPriority());

        ps.executeUpdate();
    }

    @Override
    public void update(CoachingRequest r) throws SQLException {
        String sql = "UPDATE coaching_request SET message=?, status=?, goal=?, level=?, frequency=?, budget=?, coaching_type=?, priority=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, r.getMessage());
        ps.setString(2, r.getStatus());
        ps.setString(3, r.getGoal());
        ps.setString(4, r.getLevel());
        ps.setString(5, r.getFrequency());
        ps.setObject(6, r.getBudget());
        ps.setString(7, r.getCoachingType());
        ps.setString(8, r.getPriority());
        ps.setInt(9, r.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM coaching_request WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
