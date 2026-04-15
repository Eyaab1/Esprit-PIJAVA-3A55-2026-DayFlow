package services.interaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import enums.PostStatus;
import model.interaction.Post;
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
                title, content, created_at, created_by_id, status, images,
                scheduled_at, updated_at, slug, deleted_at, view_count, click_count
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_POST = """
            UPDATE post SET
                title = ?, content = ?, created_at = ?, created_by_id = ?, status = ?,
                images = ?, scheduled_at = ?, updated_at = ?, slug = ?, deleted_at = ?,
                view_count = ?, click_count = ?
            WHERE id = ?
            """;

    private static final String DELETE_POST = """
            DELETE FROM post WHERE id = ?
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, title, content, created_at, created_by_id, status, images,
                   scheduled_at, updated_at, slug, deleted_at, view_count, click_count
            FROM post WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, title, content, created_at, created_by_id, status, images,
                   scheduled_at, updated_at, slug, deleted_at, view_count, click_count
            FROM post
            """;

    private static final String PUBLISH_DUE_SCHEDULED_POSTS = """
            UPDATE post
            SET status = ?, updated_at = ?
            WHERE status = ? AND scheduled_at IS NOT NULL AND scheduled_at <= ?
            """;

    @Override
    public void create(Post post) throws SQLException {
        insert(post);
    }

    @Override
    public void insert(Post post) throws SQLException {
        // FIX: added null safety checks
        if (post.getTitle() == null || post.getTitle().isBlank()) {
            throw new SQLException("Post title is required");
        }
        if (post.getCreatedById() == null) {
            throw new SQLException("createdById is required");
        }
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT_POST, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setString(i++, post.getTitle());
            ps.setString(i++, post.getContent());
            ps.setTimestamp(i++, post.getCreatedAt() != null ? Timestamp.valueOf(post.getCreatedAt()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
            if (post.getCreatedById() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, post.getCreatedById());
            }
            ps.setString(i++, post.getStatus() != null ? post.getStatus().value : null);
            ps.setObject(i++, toJsonList(post.getImages()));
            ps.setTimestamp(i++, post.getScheduledAt() != null ? Timestamp.valueOf(post.getScheduledAt()) : null);
            ps.setTimestamp(i++, post.getUpdatedAt() != null ? Timestamp.valueOf(post.getUpdatedAt()) : null);
            if (post.getSlug() == null) {
                post.setSlug(post.getTitle().toLowerCase().replace(" ", "-"));
            }
            ps.setString(i++, post.getSlug());
            ps.setTimestamp(i++, post.getDeletedAt() != null ? Timestamp.valueOf(post.getDeletedAt()) : null);
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
        // FIX: added null safety checks for update
        if (post.getTitle() == null || post.getTitle().isBlank()) {
            throw new SQLException("Post title is required");
        }
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE_POST)) {
            int i = 1;
            ps.setString(i++, post.getTitle());
            ps.setString(i++, post.getContent());
            ps.setTimestamp(i++, post.getCreatedAt() != null ? Timestamp.valueOf(post.getCreatedAt()) : null);
            if (post.getCreatedById() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, post.getCreatedById());
            }
            ps.setString(i++, post.getStatus() != null ? post.getStatus().value : "DRAFT");
            ps.setObject(i++, toJsonList(post.getImages()));
            ps.setTimestamp(i++, post.getScheduledAt() != null ? Timestamp.valueOf(post.getScheduledAt()) : null);
            ps.setTimestamp(i++, post.getUpdatedAt() != null ? Timestamp.valueOf(post.getUpdatedAt()) : null);
            ps.setString(i++, post.getSlug());
            ps.setTimestamp(i++, post.getDeletedAt() != null ? Timestamp.valueOf(post.getDeletedAt()) : null);
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
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(DELETE_POST)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Post findById(Integer id) throws SQLException {
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

    public List<Post> findAll() throws SQLException {
        List<Post> posts = new ArrayList<>();
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_ALL);
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
        publishDueScheduledPosts();
        return findAll();
    }

    public void updatePost(Post post) throws SQLException {
        update(post);
    }

    public void deletePost(int id) throws SQLException {
        delete(id);
    }

    public void publishDueScheduledPosts() throws SQLException {
        Connection c = DbConnexion.getConnection();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        try (PreparedStatement ps = c.prepareStatement(PUBLISH_DUE_SCHEDULED_POSTS)) {
            ps.setString(1, PostStatus.PUBLISHED.value);
            ps.setTimestamp(2, Timestamp.valueOf(now));
            ps.setString(3, PostStatus.SCHEDULED.value);
            ps.setTimestamp(4, Timestamp.valueOf(now));
            ps.executeUpdate();
        }
    }

    /** Post + compteurs pour la page profil. */
    public record PostWithStats(Post post, int likeCount, int commentCount) {}

    private static final String POST_ROW_SELECT = """
            p.id, p.title, p.content, p.created_at, p.created_by_id, p.status, p.images,
            p.scheduled_at, p.updated_at, p.slug, p.deleted_at, p.view_count, p.click_count
            """;

    private static final String PROFILE_STATS_SUFFIX = """
            ,
            (SELECT COUNT(*)::int FROM post_like pl WHERE pl.post_id = p.id) AS like_cnt,
            (SELECT COUNT(*)::int FROM comment cmt WHERE cmt.post_id = p.id) AS comment_cnt
            """;

    public List<PostWithStats> findProfilePostsByAuthorAndStatus(int authorId, PostStatus status) throws SQLException {
        publishDueScheduledPosts();
        if (status == null) {
            return List.of();
        }
        String sql = "SELECT " + POST_ROW_SELECT + PROFILE_STATS_SUFFIX + """
                FROM post p
                WHERE p.created_by_id = ? AND LOWER(TRIM(p.status)) = LOWER(TRIM(?)) AND p.deleted_at IS NULL
                ORDER BY p.created_at DESC
                """;
        Connection c = DbConnexion.getConnection();
        List<PostWithStats> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, authorId);
            ps.setString(2, status.value);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRowWithStats(rs));
                }
            }
        }
        return out;
    }

    public int countPostsByAuthorAndStatus(int authorId, PostStatus status) throws SQLException {
        publishDueScheduledPosts();
        if (status == null) {
            return 0;
        }
        String sql = """
                SELECT COUNT(*)::int FROM post p
                WHERE p.created_by_id = ? AND LOWER(TRIM(p.status)) = LOWER(TRIM(?)) AND p.deleted_at IS NULL
                """;
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, authorId);
            ps.setString(2, status.value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<PostWithStats> findProfileSavedPosts(int userId) throws SQLException {
        String sql = "SELECT " + POST_ROW_SELECT + PROFILE_STATS_SUFFIX + """
                FROM post p
                INNER JOIN saved_posts sp ON sp.post_id = p.id AND sp.user_id = ?
                WHERE p.deleted_at IS NULL
                ORDER BY sp.saved_at DESC
                """;
        Connection c = DbConnexion.getConnection();
        List<PostWithStats> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRowWithStats(rs));
                }
            }
        }
        return out;
    }

    private PostWithStats mapRowWithStats(ResultSet rs) throws SQLException {
        return new PostWithStats(mapRow(rs), rs.getInt("like_cnt"), rs.getInt("comment_cnt"));
    }

    // Helper methods
    private Post mapRow(ResultSet rs) throws SQLException {
        Post p = new Post();
        p.setId(rs.getInt("id"));
        p.setTitle(rs.getString("title"));
        p.setContent(rs.getString("content"));
        Timestamp createdTs = rs.getTimestamp("created_at");
        p.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);
        int createdById = rs.getInt("created_by_id");
        p.setCreatedById(rs.wasNull() ? null : createdById);
        p.setStatus(PostStatus.fromValue(rs.getString("status")));
        p.setImages(readStringList(rs.getString("images")));
        Timestamp scheduledTs = rs.getTimestamp("scheduled_at");
        p.setScheduledAt(scheduledTs != null ? scheduledTs.toLocalDateTime() : null);
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        p.setUpdatedAt(updatedTs != null ? updatedTs.toLocalDateTime() : null);
        p.setSlug(rs.getString("slug"));
        Timestamp deletedTs = rs.getTimestamp("deleted_at");
        p.setDeletedAt(deletedTs != null ? deletedTs.toLocalDateTime() : null);
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
