package services.tag;

import model.Tag;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TagService implements CRUD<Tag, Integer> {

    private static final String INSERT_TAG = """
            INSERT INTO tags (name, created_at, usage_count)
            VALUES (?, ?, ?)
            """;

    private static final String UPDATE_TAG = """
            UPDATE tags SET
                name = ?, created_at = ?, usage_count = ?
            WHERE id = ?
            """;

    private static final String DELETE_TAG = """
            DELETE FROM tags WHERE id = ?
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, name, created_at, usage_count
            FROM tags WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT id, name, created_at, usage_count
            FROM tags
            """;

    private static final String SELECT_BY_POST_ID = """
            SELECT t.id, t.name, t.created_at, t.usage_count
            FROM tags t
            INNER JOIN post_tags pt ON t.id = pt.tag_id
            WHERE pt.post_id = ?
            """;

    private static final String INSERT_POST_TAG = """
            INSERT INTO post_tags (post_id, tag_id)
            VALUES (?, ?)
            """;

    private static final String DELETE_POST_TAG = """
            DELETE FROM post_tags WHERE post_id = ? AND tag_id = ?
            """;

    @Override
    public void create(Tag tag) throws SQLException {
        insert(tag);
    }

    @Override
    public void insert(Tag tag) throws SQLException {
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT_TAG, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tag.getName());
            ps.setTimestamp(2, tag.getCreatedAt() != null ? Timestamp.valueOf(tag.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, tag.getUsageCount() != null ? tag.getUsageCount() : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    tag.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Tag tag) throws SQLException {
        if (tag.getId() == null) {
            throw new SQLException("id obligatoire pour UPDATE");
        }
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE_TAG)) {
            ps.setString(1, tag.getName());
            ps.setTimestamp(2, tag.getCreatedAt() != null ? Timestamp.valueOf(tag.getCreatedAt()) : null);
            ps.setInt(3, tag.getUsageCount() != null ? tag.getUsageCount() : 0);
            ps.setInt(4, tag.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null) {
            throw new SQLException("id obligatoire pour DELETE");
        }
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE_TAG)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Tag findById(Integer id) throws SQLException {
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

    public List<Tag> findAll() throws SQLException {
        List<Tag> tags = new ArrayList<>();
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tags.add(mapRow(rs));
            }
        }
        return tags;
    }

    public List<Tag> findByPostId(Integer postId) throws SQLException {
        List<Tag> tags = new ArrayList<>();
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_POST_ID)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tags.add(mapRow(rs));
                }
            }
        }
        return tags;
    }

    public void addTagToPost(Integer postId, Integer tagId) throws SQLException {
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT_POST_TAG)) {
            ps.setInt(1, postId);
            ps.setInt(2, tagId);
            ps.executeUpdate();
        }
    }

    public void removeTagFromPost(Integer postId, Integer tagId) throws SQLException {
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE_POST_TAG)) {
            ps.setInt(1, postId);
            ps.setInt(2, tagId);
            ps.executeUpdate();
        }
    }

    // Business methods
    public void addTag(Tag tag) throws SQLException {
        insert(tag);
    }

    public Tag getTagById(int id) throws SQLException {
        return findById(id);
    }

    public List<Tag> getAllTags() throws SQLException {
        return findAll();
    }

    public List<Tag> getTagsByPost(int postId) throws SQLException {
        return findByPostId(postId);
    }

    public void updateTag(Tag tag) throws SQLException {
        update(tag);
    }

    public void deleteTag(int id) throws SQLException {
        delete(id);
    }

    public void attachTagToPost(int postId, int tagId) throws SQLException {
        addTagToPost(postId, tagId);
    }

    public void detachTagFromPost(int postId, int tagId) throws SQLException {
        removeTagFromPost(postId, tagId);
    }

    // Helper methods
    private Tag mapRow(ResultSet rs) throws SQLException {
        Tag t = new Tag(null, null, null, null);
        t.setId(rs.getInt("id"));
        t.setName(rs.getString("name"));
        Timestamp createdTs = rs.getTimestamp("created_at");
        t.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);
        t.setUsageCount(rs.getInt("usage_count"));
        return t;
    }
}
