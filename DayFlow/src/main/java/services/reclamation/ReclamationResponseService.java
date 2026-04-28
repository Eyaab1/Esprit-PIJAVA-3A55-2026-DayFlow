package services.reclamation;

import model.reclamation.Reclamation;
import model.reclamation.Response;
import services.CRUD;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class ReclamationResponseService implements CRUD<Response, Integer> {

    
    private static final String INSERT = """
            INSERT INTO response (content, created_at, reclamation_id)
            VALUES (?, ?, ?)
            """;

    private static final String UPDATE = """
            UPDATE response SET content = ? WHERE id = ?
            """;

    private static final String DELETE = "DELETE FROM response WHERE id = ?";

    private static final String SELECT_BY_RECLAMATION = """
            SELECT id, content, created_at, reclamation_id
            FROM response WHERE reclamation_id = ? ORDER BY created_at ASC
            """;

    public ReclamationResponseService() {
    }

    @Override
    public void create(Response entity) throws SQLException {
        insert(entity);
    }

    @Override
    public void insert(Response response) throws SQLException {
        Integer rid = response.getReclamation() != null ? response.getReclamation().getId() : null;
        if (rid == null) {
            throw new SQLException("reclamation_id obligatoire pour insérer une réponse");
        }
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, response.getContent());
            ps.setTimestamp(2, Timestamp.valueOf(response.getCreatedAt()));
            ps.setInt(3, rid);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    response.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Response response) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, response.getContent());
            ps.setInt(2, response.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Response> findByReclamationId(int reclamationId) throws SQLException {
        List<Response> list = new ArrayList<>();
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_RECLAMATION)) {
            ps.setInt(1, reclamationId);
            try (ResultSet rs = ps.executeQuery()) {
                Reclamation stub = new Reclamation();
                stub.setId(reclamationId);
                while (rs.next()) {
                    Response r = mapRow(rs, stub);
                    list.add(r);
                }
            }
        }
        return list;
    }

    private static Response mapRow(ResultSet rs, Reclamation parent) throws SQLException {
        Response r = new Response();
        r.setId(rs.getInt("id"));
        r.setContent(rs.getString("content"));
        Timestamp ts = rs.getTimestamp("created_at");
        r.setCreatedAt(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
        r.setReclamation(parent);
        return r;
    }
}
