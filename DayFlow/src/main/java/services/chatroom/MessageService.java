package services.chatroom;
import model.chatroom.Message;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
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
     * Envoie un message — version simple
     */
    /**
     * Envoie une réponse à un message existant.
     */
    public void postReply(int userId, int chatroomId, String rawContent, int parentId) throws SQLException {
        String content = MessageValidator.validateContent(rawContent);
        if (parentId <= 0) throw new IllegalArgumentException("parentId invalide.");

        String sql = "INSERT INTO message (content, created_at, is_pinned, is_edited, chatroom_id, author_id, reply_to_id) " +
                     "VALUES (?, NOW(), false, false, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, content);
            ps.setInt(2, chatroomId);
            ps.setInt(3, userId);
            ps.setInt(4, parentId);
            ps.executeUpdate();
        }
    }

    /**
     * Retourne le contenu d'un message par son ID (pour afficher le preview de la réponse).
     */
    public String findContentById(int messageId) throws SQLException {
        String sql = "SELECT content FROM message WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("content") : null;
            }
        }
    }

    public void postMessage(int userId, int chatroomId, String rawContent) throws SQLException {
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Le message ne peut pas être vide.");
        }

        Message m = new Message(content, chatroomId, userId);
        insert(m);
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

    public List<Message> getAll() throws SQLException {
        String sql = "SELECT id, content, created_at, is_pinned, is_edited, chatroom_id, author_id FROM message ORDER BY created_at DESC";
        List<Message> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Message m = new Message();
                m.setId(rs.getInt("id"));
                m.setContent(rs.getString("content"));
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) m.setCreatedAt(ts.toLocalDateTime());
                m.setPinned(rs.getBoolean("is_pinned"));
                m.setEdited(rs.getBoolean("is_edited"));
                m.setChatroomId(rs.getInt("chatroom_id"));
                m.setAuthorId(rs.getInt("author_id"));
                list.add(m);
            }
        }
        return list;
    }

    /**
     * Recherche multi-mots dans les messages d'un chatroom.
     * Ex: "hello world" → cherche les messages contenant "hello" OU "world".
     */
    public List<Message> rechercher(String keyword, int chatroomId) throws SQLException {
        String validated = MessageValidator.validateSearch(keyword);
        String[] mots = validated.split("\\s+");

        StringBuilder sql = new StringBuilder(
            "SELECT id, content, created_at, is_pinned, is_edited, chatroom_id, author_id " +
            "FROM message WHERE chatroom_id = ? AND (");
        for (int i = 0; i < mots.length; i++) {
            sql.append("LOWER(content) LIKE ?");
            if (i < mots.length - 1) sql.append(" OR ");
        }
        sql.append(") ORDER BY created_at ASC");

        List<Message> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql.toString())) {
            ps.setInt(1, chatroomId);
            for (int i = 0; i < mots.length; i++) {
                ps.setString(i + 2, "%" + mots[i].toLowerCase() + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Message m = new Message();
                    m.setId(rs.getInt("id"));
                    m.setContent(rs.getString("content"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) m.setCreatedAt(ts.toLocalDateTime());
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

    /**
     * Soft delete : marque le message comme supprimé (is_spam=true)
     * sans le retirer de la BD — l'historique est conservé.
     */
    public void softDelete(int messageId) throws SQLException {
        String sql = "UPDATE message SET is_spam = true WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.executeUpdate();
        }
    }

    /**
     * Bascule l'épinglage d'un message.
     */
    public void togglePin(int messageId, boolean pinned) throws SQLException {
        String sql = "UPDATE message SET is_pinned = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBoolean(1, pinned);
            ps.setInt(2, messageId);
            ps.executeUpdate();
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
                SELECT id, content, created_at, is_pinned, is_edited, chatroom_id, author_id, is_spam,
                       COALESCE(reply_to_id, 0) AS reply_to_id
                FROM message
                WHERE chatroom_id = ? AND (is_spam = false OR is_spam IS NULL)
                ORDER BY is_pinned DESC, created_at ASC
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
                    m.setReplyToId(rs.getInt("reply_to_id"));
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