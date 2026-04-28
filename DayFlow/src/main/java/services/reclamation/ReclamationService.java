package services.reclamation;

import enums.ReclamationStatus;
import enums.ReclamationType;
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
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class ReclamationService implements CRUD<Reclamation, Integer> {

    public static final String AUTO_ACK_MESSAGE = """
            Votre réclamation a été reçue et est en cours d'examen. Notre équipe vous répondra dans les plus brefs délais.""";

    private static final String INSERT = """
            INSERT INTO reclamation (content, type, status, created_at, photo_path, user_id)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE = """
            UPDATE reclamation SET content = ?, type = ?, status = ?, photo_path = ? WHERE id = ?
            """;

    private static final String UPDATE_STATUS = "UPDATE reclamation SET status = ? WHERE id = ?";

    private static final String DELETE_RESPONSES = "DELETE FROM response WHERE reclamation_id = ?";
    private static final String DELETE = "DELETE FROM reclamation WHERE id = ?";

    private static final String SELECT_BY_ID = """
            SELECT id, content, type, status, created_at, photo_path, user_id
            FROM reclamation WHERE id = ?
            """;

    private static final String SELECT_BY_USER_BASE = """
            SELECT id, content, type, status, created_at, photo_path, user_id
            FROM reclamation WHERE user_id = ?
            ORDER BY created_at DESC
            """;

    private final ReclamationResponseService responseService;

    public ReclamationService() {
        this(new ReclamationResponseService());
    }

    public ReclamationService(ReclamationResponseService responseService) {
        this.responseService = responseService;
    }

    public ReclamationResponseService getResponseService() {
        return responseService;
    }

    @Override
    public void create(Reclamation entity) throws SQLException {
        insert(entity);
    }

    @Override
    public void insert(Reclamation r) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getContent());
            ps.setString(2, r.getType().value);
            ps.setString(3, r.getStatus().value);
            ps.setTimestamp(4, Timestamp.valueOf(r.getCreatedAt()));
            if (r.getPhotoPath() == null) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, r.getPhotoPath());
            }
            ps.setInt(6, r.getUserId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    r.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Reclamation r) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, r.getContent());
            ps.setString(2, r.getType().value);
            ps.setString(3, r.getStatus().value);
            if (r.getPhotoPath() == null) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, r.getPhotoPath());
            }
            ps.setInt(5, r.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null) {
            throw new SQLException("id obligatoire");
        }
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps1 = c.prepareStatement(DELETE_RESPONSES)) {
            ps1.setInt(1, id);
            ps1.executeUpdate();
        }
        try (PreparedStatement ps2 = c.prepareStatement(DELETE)) {
            ps2.setInt(1, id);
            ps2.executeUpdate();
        }
    }

    public Optional<Reclamation> findById(int id) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    
    public Optional<Reclamation> findByIdWithResponses(int id) throws SQLException {
        Optional<Reclamation> opt = findById(id);
        if (opt.isEmpty()) {
            return opt;
        }
        Reclamation r = opt.get();
        for (Response resp : responseService.findByReclamationId(id)) {
            r.addResponse(resp);
        }
        return Optional.of(r);
    }

    public boolean belongsToUser(int reclamationId, int userId) throws SQLException {
        String sql = "SELECT 1 FROM reclamation WHERE id = ? AND user_id = ?";
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reclamationId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Reclamation> findByUserId(int userId, int limit, int offset) throws SQLException {
        String sql = SELECT_BY_USER_BASE + " LIMIT ? OFFSET ?";
        List<Reclamation> list = new ArrayList<>();
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public int countByUserId(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reclamation WHERE user_id = ?";
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    
    public List<Reclamation> findForAdmin(ReclamationStatus status, ReclamationType type,
                                          String search, int limit, int offset) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT r.id, r.content, r.type, r.status, r.created_at, r.photo_path, r.user_id
                FROM reclamation r
                LEFT JOIN "user" u ON u.id = r.user_id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();
        if (status != null) {
            sql.append(" AND r.status = ?");
            params.add(status.value);
        }
        if (type != null) {
            sql.append(" AND r.type = ?");
            params.add(type.value);
        }
        if (search != null && !search.isBlank()) {
            sql.append("""
                     AND (
                        r.content ILIKE ? OR u.first_name ILIKE ? OR u.last_name ILIKE ? OR u.email ILIKE ?
                    )
                    """);
            String p = "%" + search.trim() + "%";
            params.add(p);
            params.add(p);
            params.add(p);
            params.add(p);
        }
        sql.append(" ORDER BY r.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<Reclamation> list = new ArrayList<>();
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object v = params.get(i);
                if (v instanceof Integer intVal) {
                    ps.setInt(i + 1, intVal);
                } else {
                    ps.setString(i + 1, (String) v);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public int countForAdmin(ReclamationStatus status, ReclamationType type, String search) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM reclamation r
                LEFT JOIN "user" u ON u.id = r.user_id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();
        if (status != null) {
            sql.append(" AND r.status = ?");
            params.add(status.value);
        }
        if (type != null) {
            sql.append(" AND r.type = ?");
            params.add(type.value);
        }
        if (search != null && !search.isBlank()) {
            sql.append("""
                     AND (
                        r.content ILIKE ? OR u.first_name ILIKE ? OR u.last_name ILIKE ? OR u.email ILIKE ?
                    )
                    """);
            String p = "%" + search.trim() + "%";
            params.add(p);
            params.add(p);
            params.add(p);
            params.add(p);
        }
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object v = params.get(i);
                if (v instanceof Integer intVal) {
                    ps.setInt(i + 1, intVal);
                } else {
                    ps.setString(i + 1, (String) v);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    
    public void createForUserWithAutoAck(Reclamation reclamation, ReclamationNotificationService notifications)
            throws SQLException {
        reclamation.setStatus(ReclamationStatus.PENDING);
        insert(reclamation);
        Response auto = new Response();
        auto.setContent(AUTO_ACK_MESSAGE);
        auto.setReclamation(reclamation);
        responseService.insert(auto);
        if (notifications != null) {
            notifications.notifyNewReclamation(reclamation);
        }
    }

    
    public void addAdminReply(int reclamationId, String replyContent, ReclamationNotificationService notifications)
            throws SQLException {
        Optional<Reclamation> opt = findById(reclamationId);
        if (opt.isEmpty()) {
            throw new SQLException("Réclamation introuvable : " + reclamationId);
        }
        Reclamation r = opt.get();
        r.setStatus(ReclamationStatus.ANSWERED);
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, ReclamationStatus.ANSWERED.value);
            ps.setInt(2, reclamationId);
            ps.executeUpdate();
        }
        Response resp = new Response();
        resp.setContent(replyContent);
        resp.setReclamation(r);
        responseService.insert(resp);
        if (notifications != null) {
            notifications.notifyReclamationResponse(r, resp);
        }
    }

    private static Reclamation mapRow(ResultSet rs) throws SQLException {
        Reclamation r = new Reclamation();
        r.setId(rs.getInt("id"));
        r.setContent(rs.getString("content"));
        r.setType(mapTypeLoose(rs.getString("type")));
        r.setStatus(mapStatusLoose(rs.getString("status")));
        Timestamp ts = rs.getTimestamp("created_at");
        r.setCreatedAt(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
        String photo = rs.getString("photo_path");
        r.setPhotoPath(rs.wasNull() ? null : photo);
        r.setUserId(rs.getInt("user_id"));
        return r;
    }

    
    private static ReclamationType mapTypeLoose(String raw) {
        String v = raw != null ? raw.trim() : null;
        ReclamationType t = ReclamationType.fromValue(v);
        return t != null ? t : ReclamationType.OTHER;
    }

    private static ReclamationStatus mapStatusLoose(String raw) {
        String v = raw != null ? raw.trim() : null;
        ReclamationStatus s = ReclamationStatus.fromValue(v);
        return s != null ? s : ReclamationStatus.PENDING;
    }
}
