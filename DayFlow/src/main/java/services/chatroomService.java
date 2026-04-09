package services;

import model.Chatroom;
import utils.DbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChatroomService implements CRUD<Chatroom, Integer> {

    private Connection cnx;

    public ChatroomService() {
        try {
            cnx = DbConnexion.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Connexion BD échouée", e);
        }
    }

    @Override
    public void create(Chatroom chatroom) throws SQLException { insert(chatroom); }

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

    public List<Chatroom> getAll() throws SQLException {
        List<Chatroom> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM chatroom");
        while (rs.next()) {
            Chatroom c = new Chatroom();
            c.setId(rs.getInt("id"));
            c.setGoalId(rs.getInt("goal_id"));
            c.setState(rs.getString("state"));
            c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            list.add(c);
        }
        return list;
    }
}
