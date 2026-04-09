package services;

import model.Message;
import utils.DbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageService implements CRUD<Message, Integer> {

    private Connection cnx;

    public MessageService() {
        try {
            cnx = DbConnexion.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Connexion BD échouée", e);
        }
    }

    @Override
    public void create(Message message) throws SQLException { insert(message); }

    @Override
    public void insert(Message message) throws SQLException {
        String sql = "INSERT INTO message (content, created_at, is_pinned, is_edited, chatroom_id, author_id) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, message.getContent());
        ps.setTimestamp(2, Timestamp.valueOf(message.getCreatedAt()));
        ps.setBoolean(3, message.isPinned());
        ps.setBoolean(4, message.isEdited());
        ps.setInt(5, message.getChatroomId());
        ps.setInt(6, message.getAuthorId());
        ps.executeUpdate();
    }

    @Override
    public void update(Message message) throws SQLException {
        String sql = "UPDATE message SET content = ?, is_pinned = ?, is_edited = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, message.getContent());
        ps.setBoolean(2, message.isPinned());
        ps.setBoolean(3, message.isEdited());
        ps.setInt(4, message.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM message WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public List<Message> getAll() throws SQLException {
        List<Message> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM message");
        while (rs.next()) {
            Message m = new Message();
            m.setId(rs.getInt("id"));
            m.setContent(rs.getString("content"));
            m.setChatroomId(rs.getInt("chatroom_id"));
            m.setAuthorId(rs.getInt("author_id"));
            m.setPinned(rs.getBoolean("is_pinned"));
            m.setEdited(rs.getBoolean("is_edited"));
            m.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            list.add(m);
        }
        return list;
    }
}
