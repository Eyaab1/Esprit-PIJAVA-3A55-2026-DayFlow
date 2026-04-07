package services;

import model.Goal;
import utils.DbConnexion;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GoalService implements CRUD<Goal, Integer> {

    private static final String CREATE_GOAL = """
            INSERT INTO goal (title, description, start_date, end_date, status, priority,
                              deadline, is_favorite, progress, required_tasks, trello_board_id,
                              created_at, user_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_GOAL = """
            UPDATE goal SET
                title = ?, description = ?, start_date = ?, end_date = ?, status = ?,
                priority = ?, deadline = ?, is_favorite = ?, progress = ?,
                required_tasks = ?, trello_board_id = ?, updated_at = ?
            WHERE id = ?
            """;

    private static final String DELETE_GOAL = """
            DELETE FROM goal WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, title, description, start_date, end_date, status, priority,
                   deadline, is_favorite, progress, required_tasks, trello_board_id,
                   created_at, updated_at, user_id
            FROM goal
            """;

    private static final String SELECT_BY_ID = SELECT_ALL + " WHERE id = ?";

    private static final String SELECT_BY_USER = SELECT_ALL + " WHERE user_id = ?";

    // ─── CRUD ─────────────────────────────────────────────────

    @Override
    public void create(Goal goal) throws SQLException {
        insert(goal);
    }

    @Override
    public void insert(Goal goal) throws SQLException {
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(CREATE_GOAL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, goal.getTitle());
            setNullableString(ps, 2, goal.getDescription());
            ps.setDate(3, Date.valueOf(goal.getStartDate()));
            ps.setDate(4, Date.valueOf(goal.getEndDate()));
            ps.setString(5, goal.getStatus());
            setNullableString(ps, 6, goal.getPriority());
            setNullableDate(ps, 7, goal.getDeadline());
            ps.setBoolean(8, goal.isFavorite());
            ps.setInt(9, goal.getProgress());
            setNullableInt(ps, 10, goal.getRequiredTasks());
            setNullableString(ps, 11, goal.getTrelloBoardId());
            ps.setTimestamp(12, Timestamp.valueOf(goal.getCreatedAt()));
            ps.setInt(13, goal.getUser().getId());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) goal.setId(keys.getInt(1));
            }
        }
    }

    @Override
    public void update(Goal goal) throws SQLException {
        if (goal.getId() == 0)
            throw new SQLException("id obligatoire pour UPDATE");

        goal.onUpdate();

        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE_GOAL)) {

            int i = 1;
            ps.setString(i++, goal.getTitle());
            setNullableString(ps, i++, goal.getDescription());
            ps.setDate(i++, Date.valueOf(goal.getStartDate()));
            ps.setDate(i++, Date.valueOf(goal.getEndDate()));
            ps.setString(i++, goal.getStatus());
            setNullableString(ps, i++, goal.getPriority());
            setNullableDate(ps, i++, goal.getDeadline());
            ps.setBoolean(i++, goal.isFavorite());
            ps.setInt(i++, goal.getProgress());
            setNullableInt(ps, i++, goal.getRequiredTasks());
            setNullableString(ps, i++, goal.getTrelloBoardId());
            ps.setTimestamp(i++, Timestamp.valueOf(goal.getUpdatedAt()));
            ps.setInt(i, goal.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null)
            throw new SQLException("id obligatoire pour DELETE");

        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE_GOAL)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ─── Queries ──────────────────────────────────────────────

    public Optional<Goal> findById(int id) throws SQLException {
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Goal> findAll() throws SQLException {
        List<Goal> list = new ArrayList<>();
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Goal> findByUser(int userId) throws SQLException {
        List<Goal> list = new ArrayList<>();
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_USER)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ─── Row mapper ───────────────────────────────────────────

    private Goal mapRow(ResultSet rs) throws SQLException {
        Goal g = new Goal();
        g.setId(rs.getInt("id"));
        g.setTitle(rs.getString("title"));
        g.setDescription(rs.getString("description"));
        g.setStartDate(rs.getDate("start_date").toLocalDate());
        g.setEndDate(rs.getDate("end_date").toLocalDate());
        g.setStatus(rs.getString("status"));
        g.setPriority(rs.getString("priority"));

        Date deadline = rs.getDate("deadline");
        g.setDeadline(deadline != null ? deadline.toLocalDate() : null);

        g.setFavorite(rs.getBoolean("is_favorite"));
        g.setProgress(rs.getInt("progress"));

        int reqTasks = rs.getInt("required_tasks");
        g.setRequiredTasks(rs.wasNull() ? null : reqTasks);

        g.setTrelloBoardId(rs.getString("trello_board_id"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) g.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        g.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);

        return g;
    }

    // ─── Helpers ──────────────────────────────────────────────

    private void setNullableString(PreparedStatement ps, int i, String val) throws SQLException {
        if (val == null) ps.setNull(i, Types.VARCHAR);
        else             ps.setString(i, val);
    }

    private void setNullableInt(PreparedStatement ps, int i, Integer val) throws SQLException {
        if (val == null) ps.setNull(i, Types.INTEGER);
        else             ps.setInt(i, val);
    }

    private void setNullableDate(PreparedStatement ps, int i, LocalDate val) throws SQLException {
        if (val == null) ps.setNull(i, Types.DATE);
        else             ps.setDate(i, Date.valueOf(val));
    }
}