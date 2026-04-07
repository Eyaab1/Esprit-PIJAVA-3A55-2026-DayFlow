package services;

import model.Goal;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class GoalService implements CRUD<Goal, Integer> {

    private Connection cnx;

    public GoalService() {
        cnx = DbConnexion.getInstance().getCnx();
    }

    @Override
    public void create(Goal goal) throws SQLException {
        insert(goal);
    }

    @Override
    public void insert(Goal goal) throws SQLException {
        String sql = "INSERT INTO goal (title, description, target_value) VALUES (?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, goal.getTitle());
        ps.setString(2, goal.getDescription());
        ps.setInt(3, goal.getTargetValue());

        ps.executeUpdate();
    }

    @Override
    public void update(Goal goal) throws SQLException {
        String sql = "UPDATE goal SET title=?, description=?, target_value=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, goal.getTitle());
        ps.setString(2, goal.getDescription());
        ps.setInt(3, goal.getTargetValue());
        ps.setInt(4, goal.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM goal WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
    }
}