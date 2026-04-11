package services.chatroom_module;

import model.goals_activity_management.GoalParticipation;
import services.CRUD;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class GoalParticipationService implements CRUD<GoalParticipation, Integer> {

    private final Connection cnx;

    public GoalParticipationService() {
        cnx = DbConnexion.getConnection();
    }

    @Override
    public void create(GoalParticipation gp) throws SQLException {
        insert(gp);
    }

    @Override
    public void insert(GoalParticipation gp) throws SQLException {
        String sql = "INSERT INTO goal_participation (user_id, goal_id, created_at, role, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, gp.getUserId());
            ps.setInt(2, gp.getGoalId());
            ps.setTimestamp(3, Timestamp.valueOf(gp.getCreatedAt()));
            ps.setString(4, gp.getRole());
            ps.setString(5, gp.getStatus());
            ps.executeUpdate();
        }
    }

    @Override
    public void update(GoalParticipation gp) throws SQLException {
        String sql = "UPDATE goal_participation SET role = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, gp.getRole());
            ps.setString(2, gp.getStatus());
            ps.setInt(3, gp.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM goal_participation WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
