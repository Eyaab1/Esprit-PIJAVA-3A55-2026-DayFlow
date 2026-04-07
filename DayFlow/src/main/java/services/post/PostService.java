package services.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import enums.PostStatus;
import model.Post;
import org.postgresql.util.PGobject;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostService implements CRUD<Post, Integer> {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String INSERT_POST = """
            INSERT INTO post (
                title, content, created_at, updated_at, deleted_at, scheduled_at,
                status, images, created_by_id, view_count, click_count
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_POST = """
            UPDATE post SET
                title = ?, content = ?, updated_at = ?, deleted_at = ?,
                scheduled_at = ?, status = ?, images = ?, created_by_id = ?,
                view_count = ?, click_count = ?
            WHERE id = ?
            """;

    private static final String DELETE_POST = """
            DELETE FROM post WHERE id = ?
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, title, content, created_at, updated_at, deleted_at, scheduled_at,
                   status, images, created_by_id, view_count, click_count
            FROM post WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, title, content, created_at, updated_at, deleted_at, scheduled_at,
                   status, images, created_by_id, view_count, click_count
            FROM post
            """;

    @Override
    public void create(Post post) throws SQLException {
        insert(post);
    }

    @Override
    public void insert(Post post) throws SQLException {
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT_POST, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setString(i++, post.getTitle());
            ps.setString(i++, post.getContent());
            ps.setTimestamp(i++, post.getCreatedAt() != null ? Timestamp.valueOf(post.getCreatedAt()) : null);
            ps.setTimestamp(i++, post.getUpdatedAt() != null ? Timestamp.valueOf(post.getUpdatedAt()) : null);
            ps.setTimestamp(i++, post.getDeletedAt() != null ? Timestamp.valueOf(post.getDeletedAt()) : null);
            ps.setTimestamp(i++, post.getScheduledAt() != null ? Timestamp.valueOf(post.getScheduledAt()) : null);
            ps.setString(i++, post.getStatus() != null ? post.getStatus().value : null);
            ps.setObject(i++, toJsonList(post.getImages()));
            if (post.getCreatedById() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, post.getCreatedById());
            }
            ps.setInt(i++, post.getViewCount() != null ? post.getViewCount() : 0);
            ps.setInt(i++, post.getClickCount() != null ? post.getClickCount() : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    post.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Post post) throws SQLException {
        if (post.getId() == null) {
            throw new SQLException("id obligatoire pour UPDATE");
        }
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE_POST)) {
            int i = 1;
            ps.setString(i++, post.getTitle());
            ps.setString(i++, post.getContent());
            ps.setTimestamp(i++, post.getUpdatedAt() != null ? Timestamp.valueOf(post.getUpdatedAt()) : null);
            ps.setTimestamp(i++, post.getDeletedAt() != null ? Timestamp.valueOf(post.getDeletedAt()) : null);
            ps.setTimestamp(i++, post.getScheduledAt() != null ? Timestamp.valueOf(post.getScheduledAt()) : null);
            ps.setString(i++, post.getStatus() != null ? post.getStatus().value : null);
            ps.setObject(i++, toJsonList(post.getImages()));
            if (post.getCreatedById() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, post.getCreatedById());
            }
            ps.setInt(i++, post.getViewCount() != null ? post.getViewCount() : 0);
            ps.setInt(i++, post.getClickCount() != null ? post.getClickCount() : 0);
            ps.setInt(i, post.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null) {
            throw new SQLException("id obligatoire pour DELETE");
        }
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE_POST)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Post findById(Integer id) throws SQLException {
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

    public List<Post> findAll() throws SQLException {
        List<Post> posts = new ArrayList<>();
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                posts.add(mapRow(rs));
            }
        }
        return posts;
    }

    // Business methods
    public void createPost(Post post) throws SQLException {
        insert(post);
    }

    public Post getPostById(int id) throws SQLException {
        return findById(id);
    }

    public List<Post> getAllPosts() throws SQLException {
        return findAll();
    }

    public void updatePost(Post post) throws SQLException {
        update(post);
    }

    public void deletePost(int id) throws SQLException {
        delete(id);
    }

    // Helper methods
    private Post mapRow(ResultSet rs) throws SQLException {
        Post p = new Post(null, null, null, null, null, null, null, null, null, null, null, null);
        p.setId(rs.getInt("id"));
        p.setTitle(rs.getString("title"));
        p.setContent(rs.getString("content"));
        Timestamp createdTs = rs.getTimestamp("created_at");
        p.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        p.setUpdatedAt(updatedTs != null ? updatedTs.toLocalDateTime() : null);
        Timestamp deletedTs = rs.getTimestamp("deleted_at");
        p.setDeletedAt(deletedTs != null ? deletedTs.toLocalDateTime() : null);
        Timestamp scheduledTs = rs.getTimestamp("scheduled_at");
        p.setScheduledAt(scheduledTs != null ? scheduledTs.toLocalDateTime() : null);
        p.setStatus(PostStatus.fromValue(rs.getString("status")));
        p.setImages(readStringList(rs.getString("images")));
        int createdById = rs.getInt("created_by_id");
        p.setCreatedById(rs.wasNull() ? null : createdById);
        p.setViewCount(rs.getInt("view_count"));
        p.setClickCount(rs.getInt("click_count"));
        return p;
    }

    private PGobject toJsonList(List<String> values) throws SQLException {
        PGobject json = new PGobject();
        json.setType("json");
        try {
            json.setValue(JSON.writeValueAsString(values != null ? values : List.of()));
        } catch (JsonProcessingException e) {
            throw new SQLException("Sérialisation JSON impossible", e);
        }
        return json;
    }

    private static List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return JSON.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
