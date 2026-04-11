package services.chatroom_module;

import model.chatroom.Message;
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

public class MessageService implements CRUD<Message, Integer> {

    private final Connection cnx;

    public MessageService() {
        cnx = DbConnexion.getConnection();
    }

    @Override
    public void create(Message message) throws SQLException {
        insert(message);
    }

    /**
     * Envoie un message après vérification : salon {@code active} et utilisateur participant {@code APPROVED}.
     */
    public void postMessage(int userId, int chatroomId, String rawContent) throws SQLException {
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty()) {
            throw new SQLException("Le message ne peut pas être vide.");
        }
        validateCanPost(userId, chatroomId);
        Message m = new Message(content, chatroomId, userId);
        insert(m);
    }

    private void validateCanPost(int userId, int chatroomId) throws SQLException {
        String sql = """
                SELECT 1 FROM chatroom c
                INNER JOIN goal_participation gp ON gp.goal_id = c.goal_id
                WHERE c.id = ? AND c.state = 'active'
                  AND gp.user_id = ? AND gp.status = 'APPROVED'
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, chatroomId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException(
                            "Impossible d'envoyer un message : salon fermé ou vous n'êtes pas membre approuvé.");
                }
            }
        }
    }

    @Override
    public void insert(Message message) throws SQLException {
        String sql = "INSERT INTO message (content, created_at, is_pinned, is_edited, chatroom_id, author_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, message.getContent());
            ps.setTimestamp(2, Timestamp.valueOf(message.getCreatedAt()));
            ps.setBoolean(3, message.isPinned());
            ps.setBoolean(4, message.isEdited());
            ps.setInt(5, message.getChatroomId());
            ps.setInt(6, message.getAuthorId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    message.setId(keys.getInt(1));
                }
            }
        }
    }

    public void deleteByChatroomId(int chatroomId) throws SQLException {
        String sql = "DELETE FROM message WHERE chatroom_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, chatroomId);
            ps.executeUpdate();
        }
    }

    public List<Message> findByChatroomId(int chatroomId) throws SQLException {
        String sql = """
                SELECT id, content, created_at, is_pinned, is_edited, chatroom_id, author_id
                FROM message WHERE chatroom_id = ? ORDER BY created_at ASC
                """;
        List<Message> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, chatroomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Message m = new Message();
                    m.setId(rs.getInt("id"));
                    m.setContent(rs.getString("content"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        m.setCreatedAt(ts.toLocalDateTime());
                    }
                    m.setPinned(rs.getBoolean("is_pinned"));
                    m.setEdited(rs.getBoolean("is_edited"));
                    m.setChatroomId(rs.getInt("chatroom_id"));
                    m.setAuthorId(rs.getInt("author_id"));
                    list.add(m);
                }
            }
        }
        return list;
    }

    @Override
    public void update(Message message) throws SQLException {
        String sql = "UPDATE message SET content = ?, is_pinned = ?, is_edited = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, message.getContent());
            ps.setBoolean(2, message.isPinned());
            ps.setBoolean(3, message.isEdited());
            ps.setInt(4, message.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM message WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
