package services.post;

import model.SavedPost;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SavedPostService implements CRUD<SavedPost, Integer> {

    private static final String INSERT_SAVED_POST = """
            INSERT INTO saved_posts (user_id, post_id, saved_at)
            VALUES (?, ?, ?)
            """;

    private static final String UPDATE_SAVED_POST = """
            UPDATE saved_posts SET
                user_id = ?, post_id = ?, saved_at = ?
            WHERE id = ?
            """;

    private static final String DELETE_SAVED_POST = """
            DELETE FROM saved_posts WHERE id = ?
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, user_id, post_id, saved_at
            FROM saved_posts WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, user_id, post_id, saved_at
            FROM saved_posts
            """;

    private static final String SELECT_BY_USER_ID = """
            SELECT id, user_id, post_id, saved_at
            FROM saved_posts WHERE user_id = ?
            """;

    @Override
    public void create(SavedPost savedPost) throws SQLException {
        insert(savedPost);
    }

    @Override
    public void insert(SavedPost savedPost) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT_SAVED_POST, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, savedPost.getUserId());
            ps.setInt(2, savedPost.getPostId());
            ps.setTimestamp(3, savedPost.getSavedAt() != null ? Timestamp.valueOf(savedPost.getSavedAt()) : Timestamp.valueOf(LocalDateTime.now()));
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
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE_SAVED_POST)) {
            ps.setInt(1, savedPost.getUserId());
            ps.setInt(2, savedPost.getPostId());
            ps.setTimestamp(3, savedPost.getSavedAt() != null ? Timestamp.valueOf(savedPost.getSavedAt()) : null);
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
        SavedPost sp = new SavedPost(null, null, null, null);
        sp.setId(rs.getInt("id"));
        sp.setUserId(rs.getInt("user_id"));
        sp.setPostId(rs.getInt("post_id"));
        Timestamp savedTs = rs.getTimestamp("saved_at");
        sp.setSavedAt(savedTs != null ? savedTs.toLocalDateTime() : null);
        return sp;
    }
}
