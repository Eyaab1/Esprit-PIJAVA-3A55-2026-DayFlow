package services.chatroom_module;

import model.chatroom.Chatroom;
import services.CRUD;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ChatroomService implements CRUD<Chatroom, Integer> {

    private final Connection cnx;

    public ChatroomService() {
        cnx = DbConnexion.getInstance().getCnx();
    }

    @Override
    public void create(Chatroom chatroom) throws SQLException {
        insert(chatroom);
    }

    @Override
    public void insert(Chatroom chatroom) throws SQLException {
        String sql = "INSERT INTO chatroom (goal_id, state, created_at) VALUES (?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, chatroom.getGoalId());
        ps.setString(2, chatroom.getState());
        ps.setTimestamp(3, Timestamp.valueOf(chatroom.getCreatedAt()));

        ps.executeUpdate();
    }

    @Override
    public void update(Chatroom chatroom) throws SQLException {
        String sql = "UPDATE chatroom SET goal_id = ?, state = ? WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, chatroom.getGoalId());
        ps.setString(2, chatroom.getState());
        ps.setInt(3, chatroom.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM chatroom WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
    }
}
