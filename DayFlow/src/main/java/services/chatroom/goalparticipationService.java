<<<<<<<< Updated upstream:DayFlow/src/main/java/services/chatroom_module/GoalParticipationService.java
package services.chatroom_module;

import model.goals_activity_management.GoalParticipation;
========
package services.chatroom;

import model.GoalParticipation;
>>>>>>>> Stashed changes:DayFlow/src/main/java/services/chatroom/goalparticipationService.java
import services.CRUD;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class GoalParticipationService implements CRUD<GoalParticipation, Integer> {

    private final Connection cnx;

    public GoalParticipationService() {
<<<<<<<< Updated upstream:DayFlow/src/main/java/services/chatroom_module/GoalParticipationService.java
        cnx = DbConnexion.getInstance().getCnx();
========
        try {
            cnx = DbConnexion.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Connexion BD échouée", e);
        }
>>>>>>>> Stashed changes:DayFlow/src/main/java/services/chatroom/goalparticipationService.java
    }

    @Override
    public void create(GoalParticipation gp) throws SQLException {
        insert(gp);
    }

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
}
