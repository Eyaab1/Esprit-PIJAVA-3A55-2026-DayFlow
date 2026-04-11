package services;

import model.GoalParticipation;
import utils.DbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GoalParticipationService implements CRUD<GoalParticipation, Integer> {

    private Connection cnx;

    public GoalParticipationService() {
        try {
            cnx = DbConnexion.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Connexion BD échouée", e);
        }
    }

    @Override
    public void create(GoalParticipation gp) throws SQLException { insert(gp); }

    @Override
    public void insert(GoalParticipation gp) throws SQLException {
        String sql = "INSERT INTO goal_participation (user_id, goal_id, created_at, role, status) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, gp.getUserId());
        ps.setInt(2, gp.getGoalId());
        ps.setTimestamp(3, Timestamp.valueOf(gp.getCreatedAt()));
        ps.setString(4, gp.getRole());
        ps.setString(5, gp.getStatus());
        ps.executeUpdate();
    }

    @Override
    public void update(GoalParticipation gp) throws SQLException {
        String sql = "UPDATE goal_participation SET role = ?, status = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, gp.getRole());
        ps.setString(2, gp.getStatus());
        ps.setInt(3, gp.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM goal_participation WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public List<GoalParticipation> getAll() throws SQLException {
        List<GoalParticipation> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM goal_participation");
        while (rs.next()) {
            GoalParticipation gp = new GoalParticipation();
            gp.setId(rs.getInt("id"));
            gp.setUserId(rs.getInt("user_id"));
            gp.setGoalId(rs.getInt("goal_id"));
            gp.setRole(rs.getString("role"));
            gp.setStatus(rs.getString("status"));
            gp.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            list.add(gp);
        }
        return list;
    }
}
