package services.interaction;

import model.interaction.Comment;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentService implements CRUD<Comment, Integer> {

    private static final String INSERT_COMMENT = """
            INSERT INTO comment (content, created_at, post_id, commenter_id, parent_comment_id)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_COMMENT = """
            UPDATE comment SET
                content = ?, created_at = ?, post_id = ?, commenter_id = ?, parent_comment_id = ?
            WHERE id = ?
            """;

    private static final String DELETE_COMMENT = """
            DELETE FROM comment WHERE id = ?
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, content, created_at, post_id, commenter_id, parent_comment_id
            FROM comment WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, content, created_at, post_id, commenter_id, parent_comment_id
            FROM comment
            """;

    private static final String SELECT_BY_POST_ID = """
            SELECT id, content, created_at, post_id, commenter_id, parent_comment_id
            FROM comment WHERE post_id = ?
            """;

    @Override
    public void create(Comment comment) throws SQLException {
        insert(comment);
    }

    @Override
    public void insert(Comment comment) throws SQLException {
        // FIX: moved null checks BEFORE setters
        if (comment.getContent() == null || comment.getContent().isBlank()) {
            throw new SQLException("Comment content cannot be empty");
        }
        if (comment.getPostId() == null || comment.getCommenterId() == null) {
            throw new SQLException("postId and commenterId are required");
        }
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT_COMMENT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, comment.getContent());
            ps.setTimestamp(2, comment.getCreatedAt() != null ? Timestamp.valueOf(comment.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, comment.getPostId());
            ps.setInt(4, comment.getCommenterId());
            if (comment.getParentCommentId() == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, comment.getParentCommentId());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    comment.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Comment comment) throws SQLException {
        if (comment.getId() == null) {
            throw new SQLException("id obligatoire pour UPDATE");
        }
        // FIX: moved null checks BEFORE setters
        if (comment.getContent() == null || comment.getContent().isBlank()) {
            throw new SQLException("Comment content cannot be empty");
        }
        if (comment.getPostId() == null || comment.getCommenterId() == null) {
            throw new SQLException("postId and commenterId are required");
        }
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE_COMMENT)) {
            ps.setString(1, comment.getContent());
            ps.setTimestamp(2, comment.getCreatedAt() != null ? Timestamp.valueOf(comment.getCreatedAt()) : null);
            ps.setInt(3, comment.getPostId());
            ps.setInt(4, comment.getCommenterId());
            if (comment.getParentCommentId() == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, comment.getParentCommentId());
            }
            ps.setInt(6, comment.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null) {
            throw new SQLException("id obligatoire pour DELETE");
        }
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(DELETE_COMMENT)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Comment findById(Integer id) throws SQLException {
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

    public List<Comment> findAll() throws SQLException {
        List<Comment> comments = new ArrayList<>();
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                comments.add(mapRow(rs));
            }
        }
        return comments;
    }

    public List<Comment> findByPostId(Integer postId) throws SQLException {
        List<Comment> comments = new ArrayList<>();
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_POST_ID)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comments.add(mapRow(rs));
                }
            }
        }
        return comments;
    }

    // Business methods
    public void addComment(Comment comment) throws SQLException {
        insert(comment);
    }

    public Comment getCommentById(int id) throws SQLException {
        return findById(id);
    }

    public List<Comment> getAllComments() throws SQLException {
        return findAll();
    }

    public List<Comment> getCommentsByPost(int postId) throws SQLException {
        return findByPostId(postId);
    }

    public void updateComment(Comment comment) throws SQLException {
        update(comment);
    }

    public void deleteComment(int id) throws SQLException {
        delete(id);
    }

    // Helper methods
    private Comment mapRow(ResultSet rs) throws SQLException {
        Comment c = new Comment();         c.setId(rs.getInt("id"));
        c.setContent(rs.getString("content"));
        Timestamp createdTs = rs.getTimestamp("created_at");
        c.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);
        c.setPostId(rs.getInt("post_id"));
        c.setCommenterId(rs.getInt("commenter_id"));
        int parentId = rs.getInt("parent_comment_id");
        c.setParentCommentId(rs.wasNull() ? null : parentId);
        return c;
    }
}
