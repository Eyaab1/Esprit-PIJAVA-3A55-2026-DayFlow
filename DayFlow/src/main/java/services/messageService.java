package services;

import model.Message;
import utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class MessageService implements CRUD<Message, Integer> {

    private Connection cnx;

    public MessageService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    @Override
    public void create(Message message) throws SQLException {
        insert(message);
    }

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

        System.out.println("Message sent 💬");
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

        System.out.println("Message updated ✏️");
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM message WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();

        System.out.println("Message deleted ❌");
    }
}