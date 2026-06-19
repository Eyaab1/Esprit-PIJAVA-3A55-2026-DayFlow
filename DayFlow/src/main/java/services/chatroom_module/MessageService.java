package services.chatroom_module;

import model.chatroom.Message;
import services.CRUD;
import utils.DbConnexion;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageService implements CRUD<Message, Integer> {

    private final Connection cnx;

    public MessageService() {
        cnx = DbConnexion.getConnection();
    }

    // ══════════════════════════════════════════════════════════════════════
    // CREATE
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void create(Message message) throws SQLException { insert(message); }

    @Override
    public void insert(Message message) throws SQLException {
        String sql =
            "INSERT INTO message (content, created_at, is_pinned, is_edited, " +
            "chatroom_id, author_id, attachment_path, attachment_type, " +
            "attachment_original_name, audio_duration) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, message.getContent() != null ? message.getContent() : "");
            ps.setTimestamp(2, Timestamp.valueOf(message.getCreatedAt()));
            ps.setBoolean(3, message.isPinned());
            ps.setBoolean(4, message.isEdited());
            ps.setInt(5, message.getChatroomId());
            ps.setInt(6, message.getAuthorId());
            ps.setString(7, message.getAttachmentPath());
            ps.setString(8, message.getAttachmentType());
            ps.setString(9, message.getAttachmentOriginalName());
            ps.setInt(10, message.getAudioDuration());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) message.setId(keys.getInt(1));
            }
        }
    }

    /** Envoie un message texte simple avec validation. */
    public void postMessage(int userId, int chatroomId, String rawContent) throws SQLException {
        String content = MessageValidator.validateContent(rawContent);
        Message m = new Message(content, chatroomId, userId);
        insert(m);
    }

    /** Envoie une réponse à un message existant. */
    public void postReply(int userId, int chatroomId, String rawContent, int parentId) throws SQLException {
        String content = MessageValidator.validateContent(rawContent);
        if (parentId <= 0) throw new IllegalArgumentException("parentId invalide.");
        String sql =
            "INSERT INTO message (content, created_at, is_pinned, is_edited, " +
            "chatroom_id, author_id, reply_to_id, attachment_type) " +
            "VALUES (?, NOW(), false, false, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, content);
            ps.setInt(2, chatroomId);
            ps.setInt(3, userId);
            ps.setInt(4, parentId);
            ps.setString(5, Message.TYPE_TEXT);
            ps.executeUpdate();
        }
    }

    /**
     * Envoie un message multimédia (IMAGE / AUDIO / VIDEO / FILE).
     * Copie le fichier dans uploads/<type>/ et enregistre le chemin en BD.
     */
    public void sendMedia(int userId, int chatroomId,
                          String filePath, String type,
                          String originalName, int audioDuration) throws SQLException {
        // ── Validation ────────────────────────────────────────────────
        if (filePath == null || filePath.isBlank())
            throw new IllegalArgumentException("Chemin de fichier requis.");
        if (!List.of(Message.TYPE_IMAGE, Message.TYPE_AUDIO,
                     Message.TYPE_VIDEO, Message.TYPE_FILE).contains(type))
            throw new IllegalArgumentException("Type non supporté : " + type);

        File src = new File(filePath);
        if (!src.exists()) throw new IllegalArgumentException("Fichier introuvable.");
        if (src.length() > 50L * 1024 * 1024)
            throw new IllegalArgumentException("Fichier trop volumineux (max 50 MB).");

        // ── Copie dans uploads/ ───────────────────────────────────────
        String uploadDir = "uploads/" + type.toLowerCase() + "/";
        new File(uploadDir).mkdirs();
        String destPath = uploadDir + System.currentTimeMillis() + "_" + src.getName();
        try {
            Files.copy(src.toPath(), Path.of(destPath), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new SQLException("Erreur copie fichier : " + e.getMessage());
        }

        // ── Insertion BD ──────────────────────────────────────────────
        String name = originalName != null ? originalName : src.getName();
        Message m = new Message();
        m.setChatroomId(chatroomId);
        m.setAuthorId(userId);
        m.setContent(name);
        m.setAttachmentPath(destPath);
        m.setAttachmentType(type);
        m.setAttachmentOriginalName(name);
        m.setAudioDuration(audioDuration);
        insert(m);
    }

    // ══════════════════════════════════════════════════════════════════════
    // READ
    // ══════════════════════════════════════════════════════════════════════

    public List<Message> findByChatroomId(int chatroomId) throws SQLException {
        String sql =
            "SELECT id, content, created_at, is_pinned, is_edited, chatroom_id, author_id, " +
            "COALESCE(is_spam, false) AS is_spam, " +
            "COALESCE(reply_to_id, 0) AS reply_to_id, " +
            "COALESCE(is_starred, false) AS is_starred, " +
            "attachment_path, attachment_type, attachment_original_name, " +
            "COALESCE(audio_duration, 0) AS audio_duration " +
            "FROM message " +
            "WHERE chatroom_id = ? AND (is_spam = false OR is_spam IS NULL) " +
            "ORDER BY is_pinned DESC, created_at ASC";
        List<Message> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, chatroomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public String findContentById(int messageId) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT content FROM message WHERE id = ?")) {
            ps.setInt(1, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("content") : null;
            }
        }
    }

    public List<Message> rechercher(String keyword, int chatroomId) throws SQLException {
        String validated = MessageValidator.validateSearch(keyword);
        String[] mots = validated.split("\\s+");
        StringBuilder sql = new StringBuilder(
            "SELECT id, content, created_at, is_pinned, is_edited, chatroom_id, author_id, " +
            "COALESCE(is_spam,false) AS is_spam, COALESCE(reply_to_id,0) AS reply_to_id, " +
            "COALESCE(is_starred,false) AS is_starred, " +
            "attachment_path, attachment_type, attachment_original_name, " +
            "COALESCE(audio_duration,0) AS audio_duration " +
            "FROM message WHERE chatroom_id = ? AND (");
        for (int i = 0; i < mots.length; i++) {
            sql.append("LOWER(content) LIKE ?");
            if (i < mots.length - 1) sql.append(" OR ");
        }
        sql.append(") ORDER BY created_at ASC");
        List<Message> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql.toString())) {
            ps.setInt(1, chatroomId);
            for (int i = 0; i < mots.length; i++)
                ps.setString(i + 2, "%" + mots[i].toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Message> findStarredByChatroom(int chatroomId) throws SQLException {
        String sql =
            "SELECT id, content, created_at, is_pinned, is_edited, chatroom_id, author_id, " +
            "COALESCE(is_spam,false) AS is_spam, COALESCE(reply_to_id,0) AS reply_to_id, " +
            "COALESCE(is_starred,false) AS is_starred, " +
            "attachment_path, attachment_type, attachment_original_name, " +
            "COALESCE(audio_duration,0) AS audio_duration " +
            "FROM message WHERE chatroom_id = ? AND is_starred = true ORDER BY created_at DESC";
        List<Message> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, chatroomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Message> getAll() throws SQLException {
        String sql =
            "SELECT id, content, created_at, is_pinned, is_edited, chatroom_id, author_id, " +
            "COALESCE(is_spam,false) AS is_spam, COALESCE(reply_to_id,0) AS reply_to_id, " +
            "COALESCE(is_starred,false) AS is_starred, " +
            "attachment_path, attachment_type, attachment_original_name, " +
            "COALESCE(audio_duration,0) AS audio_duration " +
            "FROM message ORDER BY created_at DESC";
        List<Message> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public int[] getStats(int chatroomId) throws SQLException {
        int msgs = 0, active = 0;
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT COUNT(*) FROM message WHERE chatroom_id=? AND (is_spam=false OR is_spam IS NULL)")) {
            ps.setInt(1, chatroomId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) msgs = rs.getInt(1); }
        }
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT COUNT(DISTINCT author_id) FROM message WHERE chatroom_id=? AND (is_spam=false OR is_spam IS NULL)")) {
            ps.setInt(1, chatroomId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) active = rs.getInt(1); }
        }
        return new int[]{msgs, active};
    }

    // ══════════════════════════════════════════════════════════════════════
    // UPDATE
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void update(Message message) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "UPDATE message SET content=?, is_pinned=?, is_edited=? WHERE id=?")) {
            ps.setString(1, message.getContent());
            ps.setBoolean(2, message.isPinned());
            ps.setBoolean(3, message.isEdited());
            ps.setInt(4, message.getId());
            ps.executeUpdate();
        }
    }

    public void togglePin(int messageId, boolean pinned) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "UPDATE message SET is_pinned=? WHERE id=?")) {
            ps.setBoolean(1, pinned); ps.setInt(2, messageId); ps.executeUpdate();
        }
    }

    public void toggleStar(int messageId, boolean starred) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "UPDATE message SET is_starred=? WHERE id=?")) {
            ps.setBoolean(1, starred); ps.setInt(2, messageId); ps.executeUpdate();
        }
    }

    public void softDelete(int messageId) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "UPDATE message SET is_spam=true WHERE id=?")) {
            ps.setInt(1, messageId); ps.executeUpdate();
        }
    }

    public void forwardMessage(int messageId, int targetChatroomId, int userId) throws SQLException {
        String content = findContentById(messageId);
        if (content == null) throw new SQLException("Message introuvable.");
        postMessage(userId, targetChatroomId, "↪ " + content);
    }

    // ══════════════════════════════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void delete(Integer id) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM message WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    public void deleteByChatroomId(int chatroomId) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM message WHERE chatroom_id=?")) {
            ps.setInt(1, chatroomId); ps.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // MAPPING
    // ══════════════════════════════════════════════════════════════════════

    private static Message mapRow(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getInt("id"));
        String content = rs.getString("content");
        if (content != null && !content.isBlank()) {
            try { m.setContent(content); } catch (Exception ignored) {}
        }
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) m.setCreatedAt(ts.toLocalDateTime());
        m.setPinned(rs.getBoolean("is_pinned"));
        m.setEdited(rs.getBoolean("is_edited"));
        m.setChatroomId(rs.getInt("chatroom_id"));
        m.setAuthorId(rs.getInt("author_id"));
        m.setReplyToId(rs.getInt("reply_to_id"));
        m.setStarred(rs.getBoolean("is_starred"));
        m.setAttachmentPath(rs.getString("attachment_path"));
        m.setAttachmentType(rs.getString("attachment_type"));
        m.setAttachmentOriginalName(rs.getString("attachment_original_name"));
        m.setAudioDuration(rs.getInt("audio_duration"));
        return m;
    }
}
