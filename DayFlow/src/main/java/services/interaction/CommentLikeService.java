package services.interaction;

import model.interaction.CommentLike;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentLikeService implements CRUD<CommentLike, Integer> {

    private static final String INSERT_COMMENT_LIKE = """
            INSERT INTO comment_like (created_at, comment_id, user_id)
            VALUES (?, ?, ?)
            """;

    private static final String UPDATE_COMMENT_LIKE = """
            UPDATE comment_like SET
                created_at = ?, comment_id = ?, user_id = ?
            WHERE id = ?
            """;

    private static final String DELETE_COMMENT_LIKE = """
            DELETE FROM comment_like WHERE id = ?
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, created_at, comment_id, user_id
            FROM comment_like WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, created_at, comment_id, user_id
            FROM comment_like
            """;

    private static final String SELECT_BY_COMMENT_ID = """
            SELECT id, created_at, comment_id, user_id
            FROM comment_like WHERE comment_id = ?
            """;

    @Override
    public void create(CommentLike commentLike) throws SQLException {
        insert(commentLike);
    }

    @Override
    public void insert(CommentLike commentLike) throws SQLException {
        // FIX: moved null checks BEFORE setters
        if (commentLike.getCommentId() == null || commentLike.getUserId() == null) {
            throw new SQLException("commentId and userId are required");
        }
        // FIX: prevent duplicate comment_like
        String checkDuplicate = "SELECT 1 FROM comment_like WHERE comment_id = ? AND user_id = ?";
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement checkPs = c.prepareStatement(checkDuplicate)) {
            checkPs.setInt(1, commentLike.getCommentId());
            checkPs.setInt(2, commentLike.getUserId());
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) {
                    throw new SQLException("User has already liked this comment");
                }
            }
        }
        try (PreparedStatement ps = c.prepareStatement(INSERT_COMMENT_LIKE, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, commentLike.getCreatedAt() != null ? Timestamp.valueOf(commentLike.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, commentLike.getCommentId());
            ps.setInt(3, commentLike.getUserId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    commentLike.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(CommentLike commentLike) throws SQLException {
        if (commentLike.getId() == null) {
            throw new SQLException("id obligatoire pour UPDATE");
        }
        // FIX: moved null checks BEFORE setters
        if (commentLike.getCommentId() == null || commentLike.getUserId() == null) {
            throw new SQLException("commentId and userId are required");
        }
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE_COMMENT_LIKE)) {
            ps.setTimestamp(1, commentLike.getCreatedAt() != null ? Timestamp.valueOf(commentLike.getCreatedAt()) : null);
            ps.setInt(2, commentLike.getCommentId());
            ps.setInt(3, commentLike.getUserId());
            ps.setInt(4, commentLike.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null) {
            throw new SQLException("id obligatoire pour DELETE");
        }
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(DELETE_COMMENT_LIKE)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public CommentLike findById(Integer id) throws SQLException {
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

    public List<CommentLike> findAll() throws SQLException {
        List<CommentLike> likes = new ArrayList<>();
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                likes.add(mapRow(rs));
            }
        }
        return likes;
    }

    public List<CommentLike> findByCommentId(Integer commentId) throws SQLException {
        List<CommentLike> likes = new ArrayList<>();
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_COMMENT_ID)) {
            ps.setInt(1, commentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    likes.add(mapRow(rs));
                }
            }
        }
        return likes;
    }

    // Helper methods
    private CommentLike mapRow(ResultSet rs) throws SQLException {
        CommentLike cl = new CommentLike();
        cl.setId(rs.getInt("id"));
        Timestamp createdTs = rs.getTimestamp("created_at");
        cl.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);
        cl.setCommentId(rs.getInt("comment_id"));
        cl.setUserId(rs.getInt("user_id"));
        return cl;
    }
}
