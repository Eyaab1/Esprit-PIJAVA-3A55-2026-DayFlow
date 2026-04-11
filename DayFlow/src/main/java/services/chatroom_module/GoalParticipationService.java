package services.chatroom_module;

import model.goals_activity_management.GoalParticipation;
import services.CRUD;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, gp.getUserId());
            ps.setInt(2, gp.getGoalId());
            ps.setTimestamp(3, Timestamp.valueOf(gp.getCreatedAt()));
            ps.setString(4, gp.getRole());
            ps.setString(5, gp.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    gp.setId(keys.getInt(1));
                }
            }
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

    public void updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE goal_participation SET status = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void updateRoleAndStatus(int id, String role, String status) throws SQLException {
        String sql = "UPDATE goal_participation SET role = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setString(2, status);
            ps.setInt(3, id);
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

    public Optional<GoalParticipation> findByUserAndGoal(int userId, int goalId) throws SQLException {
        String sql = "SELECT id, user_id, goal_id, created_at, role, status FROM goal_participation WHERE user_id = ? AND goal_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<GoalParticipation> findById(int id) throws SQLException {
        String sql = "SELECT id, user_id, goal_id, created_at, role, status FROM goal_participation WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean isOwnerOrAdmin(int userId, int goalId) throws SQLException {
        String sql = """
                SELECT 1 FROM goal_participation
                WHERE user_id = ? AND goal_id = ? AND status = 'APPROVED'
                  AND (role = 'OWNER' OR role = 'ADMIN')
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<GoalParticipation> findPendingByGoal(int goalId) throws SQLException {
        String sql = """
                SELECT id, user_id, goal_id, created_at, role, status
                FROM goal_participation
                WHERE goal_id = ? AND status = 'PENDING'
                ORDER BY created_at ASC
                """;
        return queryList(sql, goalId);
    }

    public List<GoalParticipation> findApprovedByGoal(int goalId) throws SQLException {
        String sql = """
                SELECT id, user_id, goal_id, created_at, role, status
                FROM goal_participation
                WHERE goal_id = ? AND status = 'APPROVED'
                ORDER BY role ASC, created_at ASC
                """;
        return queryList(sql, goalId);
    }

    public int countApprovedByGoal(int goalId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM goal_participation WHERE goal_id = ? AND status = 'APPROVED'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<Integer> listGoalIdsForUserApproved(int userId) throws SQLException {
        String sql = "SELECT goal_id FROM goal_participation WHERE user_id = ? AND status = 'APPROVED'";
        List<Integer> out = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getInt(1));
                }
            }
        }
        return out;
    }

    private List<GoalParticipation> queryList(String sql, int goalId) throws SQLException {
        List<GoalParticipation> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private static GoalParticipation mapRow(ResultSet rs) throws SQLException {
        GoalParticipation gp = new GoalParticipation();
        gp.setId(rs.getInt("id"));
        gp.setUserId(rs.getInt("user_id"));
        gp.setGoalId(rs.getInt("goal_id"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            gp.setCreatedAt(ts.toLocalDateTime());
        }
        gp.setRole(rs.getString("role"));
        gp.setStatus(rs.getString("status"));
        return gp;
    }
}
