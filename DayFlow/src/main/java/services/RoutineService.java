package services;

import model.Routine;
import utils.DbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoutineService implements CRUD<Routine, Integer> {

    private static final String CREATE_ROUTINE = """
            INSERT INTO routine (title, description, visibility, status, priority,
                                 deadline, is_favorite, created_at, goal_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_ROUTINE = """
            UPDATE routine SET
                title = ?, description = ?, visibility = ?, status = ?, priority = ?,
                deadline = ?, is_favorite = ?, updated_at = ?
            WHERE id = ?
            """;

    private static final String DELETE_ROUTINE = """
            DELETE FROM routine WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, title, description, visibility, status, priority,
                   deadline, is_favorite, created_at, updated_at, goal_id
            FROM routine
            """;

    private static final String SELECT_BY_ID   = SELECT_ALL + " WHERE id = ?";
    private static final String SELECT_BY_GOAL = SELECT_ALL + " WHERE goal_id = ?";

    // ─── CRUD ─────────────────────────────────────────────────

    @Override
    public void create(Routine routine) throws SQLException {
        insert(routine);
    }

    @Override
    public void insert(Routine routine) throws SQLException {
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(CREATE_ROUTINE, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, routine.getTitle());
            setNullableString(ps, 2, routine.getDescription());
            ps.setString(3, routine.getVisibility());
            ps.setString(4, routine.getStatus());
            setNullableString(ps, 5, routine.getPriority());
            setNullableDate(ps, 6, routine.getDeadline());
            ps.setBoolean(7, routine.isFavorite());
            ps.setTimestamp(8, Timestamp.valueOf(routine.getCreatedAt()));
            ps.setInt(9, routine.getGoal().getId());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) routine.setId(keys.getInt(1));
            }
        }
    }

    @Override
    public void update(Routine routine) throws SQLException {
        if (routine.getId() == 0)
            throw new SQLException("id obligatoire pour UPDATE");

        routine.onUpdate();

        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE_ROUTINE)) {

            int i = 1;
            ps.setString(i++, routine.getTitle());
            setNullableString(ps, i++, routine.getDescription());
            ps.setString(i++, routine.getVisibility());
            ps.setString(i++, routine.getStatus());
            setNullableString(ps, i++, routine.getPriority());
            setNullableDate(ps, i++, routine.getDeadline());
            ps.setBoolean(i++, routine.isFavorite());
            ps.setTimestamp(i++, Timestamp.valueOf(routine.getUpdatedAt()));
            ps.setInt(i, routine.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null)
            throw new SQLException("id obligatoire pour DELETE");

        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE_ROUTINE)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ─── Queries ──────────────────────────────────────────────

    public Optional<Routine> findById(int id) throws SQLException {
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Routine> findAll() throws SQLException {
        List<Routine> list = new ArrayList<>();
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Routine> findByGoal(int goalId) throws SQLException {
        List<Routine> list = new ArrayList<>();
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_GOAL)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ─── Row mapper ───────────────────────────────────────────

    private Routine mapRow(ResultSet rs) throws SQLException {
        Routine r = new Routine();
        r.setId(rs.getInt("id"));
        r.setTitle(rs.getString("title"));
        r.setDescription(rs.getString("description"));
        r.setVisibility(rs.getString("visibility"));
        r.setStatus(rs.getString("status"));
        r.setPriority(rs.getString("priority"));

        Date deadline = rs.getDate("deadline");
        r.setDeadline(deadline != null ? deadline.toLocalDate() : null);

        r.setFavorite(rs.getBoolean("is_favorite"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) r.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        r.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);

        return r;
    }

    // ─── Helpers ──────────────────────────────────────────────

    private void setNullableString(PreparedStatement ps, int i, String val) throws SQLException {
        if (val == null) ps.setNull(i, Types.VARCHAR);
        else             ps.setString(i, val);
    }

    private void setNullableDate(PreparedStatement ps, int i, java.time.LocalDate val) throws SQLException {
        if (val == null) ps.setNull(i, Types.DATE);
        else             ps.setDate(i, Date.valueOf(val));
    }
}