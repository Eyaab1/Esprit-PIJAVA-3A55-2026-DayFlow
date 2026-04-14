package services.Goal_acitvityManagment_module;

import model.goals_activity_management.Routine;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoutineService implements CRUD<Routine, Integer> {

    private Connection cnx;

    public RoutineService() {
        cnx = DbConnexion.getInstance().getCnx();
    }

    @Override
    public void create(Routine r) throws SQLException {
        insert(r);
    }

    @Override
    public void insert(Routine r) throws SQLException {

        String sql = "INSERT INTO routine VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, r.getTitle());
        ps.setString(2, r.getDescription());
        ps.setString(3, r.getVisibility());
        ps.setString(4, r.getStatus());
        ps.setString(5, r.getPriority());

        ps.setDate(6, r.getDeadline() != null ? Date.valueOf(r.getDeadline()) : null);
        ps.setBoolean(7, r.isFavorite());

        ps.setTimestamp(8, Timestamp.valueOf(r.getCreatedAt()));
        ps.setTimestamp(9, r.getUpdatedAt() != null ? Timestamp.valueOf(r.getUpdatedAt()) : null);

        ps.setInt(10, r.getGoal() != null ? r.getGoal().getId() : 0);

        ps.executeUpdate();
    }

    @Override
    public void update(Routine r) throws SQLException {

        r.onUpdate();

        String sql = "UPDATE routine SET title=?, description=?, visibility=?, status=?, priority=?, deadline=?, is_favorite=?, updated_at=?, goal_id=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, r.getTitle());
        ps.setString(2, r.getDescription());
        ps.setString(3, r.getVisibility());
        ps.setString(4, r.getStatus());
        ps.setString(5, r.getPriority());

        ps.setDate(6, r.getDeadline() != null ? Date.valueOf(r.getDeadline()) : null);
        ps.setBoolean(7, r.isFavorite());

        ps.setTimestamp(8, Timestamp.valueOf(r.getUpdatedAt()));
        ps.setInt(9, r.getGoal() != null ? r.getGoal().getId() : 0);

        ps.setInt(10, r.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM routine WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public List<Routine> findByGoalId(int goalId) throws SQLException {
        String sql = """
                SELECT id, title, description, visibility, status, priority, deadline, 
                       is_favorite, created_at, updated_at, goal_id
                FROM routine WHERE goal_id = ?
                ORDER BY created_at DESC
                """;
        List<Routine> routines = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Routine routine = new Routine();
                    routine.setId(rs.getInt("id"));
                    routine.setTitle(rs.getString("title"));
                    routine.setDescription(rs.getString("description"));
                    routine.setVisibility(rs.getString("visibility"));
                    routine.setStatus(rs.getString("status"));
                    routine.setPriority(rs.getString("priority"));
                    
                    Date deadline = rs.getDate("deadline");
                    if (deadline != null) routine.setDeadline(deadline.toLocalDate());
                    
                    routine.setFavorite(rs.getBoolean("is_favorite"));
                    
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) routine.setUpdatedAt(updatedAt.toLocalDateTime());
                    
                    routines.add(routine);
                }
            }
        }
        return routines;
    }
}
