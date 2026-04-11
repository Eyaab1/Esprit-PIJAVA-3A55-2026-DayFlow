package services.Goal_acitvityManagment_module;

import model.goals_activity_management.Goal;
import services.CRUD;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class GoalService implements CRUD<Goal, Integer> {

    private Connection cnx;

    private static final String INSERT_GOAL = """
            INSERT INTO goal (
                title, description, start_date, end_date, deadline, status, priority,
                is_favorite, progress, required_tasks, trello_board_id, user_id, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_GOAL = """
            UPDATE goal SET
                title=?, description=?, start_date=?, end_date=?, deadline=?, status=?, priority=?,
                is_favorite=?, progress=?, required_tasks=?, trello_board_id=?, updated_at=?
            WHERE id=?
            """;

    public GoalService() {
        cnx = DbConnexion.getInstance().getCnx();
    }

    @Override
    public void create(Goal goal) throws SQLException {
        insert(goal);
    }

    @Override
    public void insert(Goal goal) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(INSERT_GOAL, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setString(i++, goal.getTitle());
            ps.setString(i++, goal.getDescription());
            ps.setDate(i++, goal.getStartDate() != null ? Date.valueOf(goal.getStartDate()) : null);
            ps.setDate(i++, goal.getEndDate() != null ? Date.valueOf(goal.getEndDate()) : null);
            ps.setDate(i++, goal.getDeadline() != null ? Date.valueOf(goal.getDeadline()) : null);
            ps.setString(i++, goal.getStatus());
            if (goal.getPriority() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, goal.getPriority());
            }
            ps.setBoolean(i++, goal.isFavorite());
            ps.setInt(i++, goal.getProgress());
            if (goal.getRequiredTasks() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, goal.getRequiredTasks());
            }
            if (goal.getTrelloBoardId() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, goal.getTrelloBoardId());
            }
            // Add user_id
            if (goal.getUser() != null && goal.getUser().getId() != null) {
                ps.setInt(i++, goal.getUser().getId());
            } else {
                ps.setNull(i++, Types.INTEGER);
            }
            ps.setTimestamp(i++, goal.getCreatedAt() != null ? Timestamp.valueOf(goal.getCreatedAt()) : null);
            ps.setTimestamp(i++, goal.getUpdatedAt() != null ? Timestamp.valueOf(goal.getUpdatedAt()) : null);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    goal.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Goal goal) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(UPDATE_GOAL)) {
            int i = 1;
            ps.setString(i++, goal.getTitle());
            ps.setString(i++, goal.getDescription());
            ps.setDate(i++, goal.getStartDate() != null ? Date.valueOf(goal.getStartDate()) : null);
            ps.setDate(i++, goal.getEndDate() != null ? Date.valueOf(goal.getEndDate()) : null);
            ps.setDate(i++, goal.getDeadline() != null ? Date.valueOf(goal.getDeadline()) : null);
            ps.setString(i++, goal.getStatus());
            if (goal.getPriority() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, goal.getPriority());
            }
            ps.setBoolean(i++, goal.isFavorite());
            ps.setInt(i++, goal.getProgress());
            if (goal.getRequiredTasks() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, goal.getRequiredTasks());
            }
            if (goal.getTrelloBoardId() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, goal.getTrelloBoardId());
            }
            ps.setTimestamp(i++, goal.getUpdatedAt() != null ? Timestamp.valueOf(goal.getUpdatedAt()) : null);
            ps.setInt(i, goal.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM goal WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Goal> findByUserId(int userId) throws SQLException {
        String sql = """
                SELECT id, title, description, start_date, end_date, deadline, status, priority,
                       is_favorite, progress, required_tasks, trello_board_id, created_at, updated_at
                FROM goal WHERE user_id = ?
                ORDER BY created_at DESC
                """;
        List<Goal> goals = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    Goal goal = new Goal();
                    goal.setId(rs.getInt("id"));
                    goal.setTitle(rs.getString("title"));
                    goal.setDescription(rs.getString("description"));
                    
                    var startDate = rs.getDate("start_date");
                    if (startDate != null) goal.setStartDate(startDate.toLocalDate());
                    
                    var endDate = rs.getDate("end_date");
                    if (endDate != null) goal.setEndDate(endDate.toLocalDate());
                    
                    var deadline = rs.getDate("deadline");
                    if (deadline != null) goal.setDeadline(deadline.toLocalDate());
                    
                    goal.setStatus(rs.getString("status"));
                    goal.setPriority(rs.getString("priority"));
                    goal.setFavorite(rs.getBoolean("is_favorite"));
                    goal.setProgress(rs.getInt("progress"));
                    
                    int reqTasks = rs.getInt("required_tasks");
                    if (!rs.wasNull()) goal.setRequiredTasks(reqTasks);
                    
                    goal.setTrelloBoardId(rs.getString("trello_board_id"));
                    
                    var createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        // Set via reflection or add setter
                    }
                    
                    var updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) goal.setUpdatedAt(updatedAt.toLocalDateTime());
                    
                    goals.add(goal);
                }
            }
        }
        return goals;
    }
}
