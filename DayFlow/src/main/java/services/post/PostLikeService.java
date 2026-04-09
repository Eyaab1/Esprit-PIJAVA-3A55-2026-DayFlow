package services.post;

import model.PostLike;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PostLikeService implements CRUD<PostLike, Integer> {

    private static final String INSERT_POST_LIKE = """
            INSERT INTO post_like (post_id, user_id, created_at)
            VALUES (?, ?, ?)
            """;

    private static final String UPDATE_POST_LIKE = """
            UPDATE post_like SET
                post_id = ?, user_id = ?, created_at = ?
            WHERE id = ?
            """;

    private static final String DELETE_POST_LIKE = """
            DELETE FROM post_like WHERE id = ?
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, post_id, user_id, created_at
            FROM post_like WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, post_id, user_id, created_at
            FROM post_like
            """;

    private static final String SELECT_BY_POST_ID = """
            SELECT id, post_id, user_id, created_at
            FROM post_like WHERE post_id = ?
            """;

    @Override
    public void create(PostLike postLike) throws SQLException {
        insert(postLike);
    }

    @Override
    public void insert(PostLike postLike) throws SQLException {
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT_POST_LIKE, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, postLike.getPostId());
            ps.setInt(2, postLike.getUserId());
            ps.setTimestamp(3, postLike.getCreatedAt() != null ? Timestamp.valueOf(postLike.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    postLike.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(PostLike postLike) throws SQLException {
        if (postLike.getId() == null) {
            throw new SQLException("id obligatoire pour UPDATE");
        }
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE_POST_LIKE)) {
            ps.setInt(1, postLike.getPostId());
            ps.setInt(2, postLike.getUserId());
            ps.setTimestamp(3, postLike.getCreatedAt() != null ? Timestamp.valueOf(postLike.getCreatedAt()) : null);
            ps.setInt(4, postLike.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null) {
            throw new SQLException("id obligatoire pour DELETE");
        }
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE_POST_LIKE)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public PostLike findById(Integer id) throws SQLException {
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<PostLike> findAll() throws SQLException {
        List<PostLike> likes = new ArrayList<>();
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                likes.add(mapRow(rs));
            }
        }
        return likes;
    }

    public List<PostLike> findByPostId(Integer postId) throws SQLException {
        List<PostLike> likes = new ArrayList<>();
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_POST_ID)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    likes.add(mapRow(rs));
                }
            }
        }
        return likes;
    }

    // Helper methods
    private PostLike mapRow(ResultSet rs) throws SQLException {
        PostLike pl = new PostLike(null, null, null, null);
        pl.setId(rs.getInt("id"));
        pl.setPostId(rs.getInt("post_id"));
        pl.setUserId(rs.getInt("user_id"));
        Timestamp createdTs = rs.getTimestamp("created_at");
        pl.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);
        return pl;
    }
}
