package services.chatroom_module;

import model.chatroom.Chatroom;
import services.CRUD;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChatroomService implements CRUD<Chatroom, Integer> {

    private final Connection cnx;

    public ChatroomService() {
        cnx = DbConnexion.getConnection();
    }

    @Override
    public void create(Chatroom chatroom) throws SQLException {
        insert(chatroom);
    }

    @Override
    public void insert(Chatroom chatroom) throws SQLException {
        String sql = "INSERT INTO chatroom (goal_id, state, created_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, chatroom.getGoalId());
            ps.setString(2, chatroom.getState());
            ps.setTimestamp(3, Timestamp.valueOf(chatroom.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    chatroom.setId(keys.getInt(1));
                }
            }
        }
    }

    public Optional<Chatroom> findByGoalId(int goalId) throws SQLException {
        String sql = "SELECT id, goal_id, state, created_at FROM chatroom WHERE goal_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Chatroom> findById(int id) throws SQLException {
        String sql = "SELECT id, goal_id, state, created_at FROM chatroom WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    private static Chatroom mapRow(ResultSet rs) throws SQLException {
        Chatroom c = new Chatroom();
        c.setId(rs.getInt("id"));
        c.setGoalId(rs.getInt("goal_id"));
        c.setState(rs.getString("state"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            c.setCreatedAt(ts.toLocalDateTime());
        }
        return c;
    }

    public record ChatroomListItem(int chatroomId, int goalId, String goalTitle, String lastMessageSnippet) {
    }

    public List<ChatroomListItem> findAccessibleForUser(int userId) throws SQLException {
        String sql = """
                SELECT c.id AS cid, g.id AS gid, g.title,
                       (SELECT m.content FROM message m
                        WHERE m.chatroom_id = c.id
                        ORDER BY m.created_at DESC NULLS LAST
                        LIMIT 1) AS snippet
                FROM chatroom c
                INNER JOIN goal g ON g.id = c.goal_id
                INNER JOIN goal_participation gp ON gp.goal_id = g.id
                WHERE gp.user_id = ? AND gp.status = 'APPROVED'
                ORDER BY g.created_at DESC
                """;
        List<ChatroomListItem> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String snip = rs.getString("snippet");
                    list.add(new ChatroomListItem(
                            rs.getInt("cid"),
                            rs.getInt("gid"),
                            rs.getString("title"),
                            rs.wasNull() ? null : snip));
                }
            }
        }
        return list;
    }

    @Override
    public void update(Chatroom chatroom) throws SQLException {
        String sql = "UPDATE chatroom SET goal_id = ?, state = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, chatroom.getGoalId());
            ps.setString(2, chatroom.getState());
            ps.setInt(3, chatroom.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM chatroom WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
