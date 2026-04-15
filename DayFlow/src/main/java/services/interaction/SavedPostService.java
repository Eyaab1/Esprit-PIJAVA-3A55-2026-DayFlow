package services.interaction;

import model.interaction.SavedPost;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SavedPostService implements CRUD<SavedPost, Integer> {

    private static final String INSERT_SAVED_POST = """
            INSERT INTO saved_posts (saved_at, user_id, post_id)
            VALUES (?, ?, ?)
            """;

    private static final String UPDATE_SAVED_POST = """
            UPDATE saved_posts SET
                saved_at = ?, user_id = ?, post_id = ?
            WHERE id = ?
            """;

    private static final String DELETE_SAVED_POST = """
            DELETE FROM saved_posts WHERE id = ?
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, saved_at, user_id, post_id
            FROM saved_posts WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, saved_at, user_id, post_id
            FROM saved_posts
            """;

    private static final String SELECT_BY_USER_ID = """
            SELECT id, saved_at, user_id, post_id
            FROM saved_posts WHERE user_id = ?
            """;

    @Override
    public void create(SavedPost savedPost) throws SQLException {
        insert(savedPost);
    }

    @Override
    public void insert(SavedPost savedPost) throws SQLException {
        // FIX: moved null checks BEFORE setters
        if (savedPost.getUserId() == null || savedPost.getPostId() == null) {
            throw new SQLException("userId and postId are required");
        }
        // FIX: prevent duplicate saved_posts
        String checkDuplicate = "SELECT 1 FROM saved_posts WHERE user_id = ? AND post_id = ?";
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement checkPs = c.prepareStatement(checkDuplicate)) {
            checkPs.setInt(1, savedPost.getUserId());
            checkPs.setInt(2, savedPost.getPostId());
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) {
                    throw new SQLException("Post already saved by this user");
                }
            }
        }
        try (PreparedStatement ps = c.prepareStatement(INSERT_SAVED_POST, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, savedPost.getSavedAt() != null ? Timestamp.valueOf(savedPost.getSavedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, savedPost.getUserId());
            ps.setInt(3, savedPost.getPostId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    savedPost.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(SavedPost savedPost) throws SQLException {
        if (savedPost.getId() == null) {
            throw new SQLException("id obligatoire pour UPDATE");
        }
        // FIX: moved null checks BEFORE setters
        if (savedPost.getUserId() == null || savedPost.getPostId() == null) {
            throw new SQLException("userId and postId are required");
        }
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE_SAVED_POST)) {
            ps.setTimestamp(1, savedPost.getSavedAt() != null ? Timestamp.valueOf(savedPost.getSavedAt()) : null);
            ps.setInt(2, savedPost.getUserId());
            ps.setInt(3, savedPost.getPostId());
            ps.setInt(4, savedPost.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null) {
            throw new SQLException("id obligatoire pour DELETE");
        }
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(DELETE_SAVED_POST)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public SavedPost findById(Integer id) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<SavedPost> findAll() throws SQLException {
        List<SavedPost> savedPosts = new ArrayList<>();
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                savedPosts.add(mapRow(rs));
            }
        }
        return savedPosts;
    }

    public List<SavedPost> findByUserId(Integer userId) throws SQLException {
        List<SavedPost> savedPosts = new ArrayList<>();
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_USER_ID)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    savedPosts.add(mapRow(rs));
                }
            }
        }
        return savedPosts;
    }

    // Helper methods
    private SavedPost mapRow(ResultSet rs) throws SQLException {
        SavedPost sp = new SavedPost();
        sp.setId(rs.getInt("id"));
        Timestamp savedTs = rs.getTimestamp("saved_at");
        sp.setSavedAt(savedTs != null ? savedTs.toLocalDateTime() : null);
        sp.setUserId(rs.getInt("user_id"));
        sp.setPostId(rs.getInt("post_id"));
        return sp;
    }
}
