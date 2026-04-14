package services.coaching_session_module;

import model.coaching_session.Session;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Persistance {@code session} alignée sur l’entité Symfony {@code Session}
 * (statuts, propositions d’horaires, priorité, objectif, prix, paiement).
 */
public class SessionService implements CRUD<Session, Integer> {

    private static final String COLUMNS = """
            s.id, s.coaching_request_id, s.status, s.proposed_time_by_user, s.proposed_time_by_coach,
            s.scheduled_at, s.duration, s.priority, s.objective, s.created_at, s.updated_at, s.price, s.payment_status
            """;

    @Override
    public void create(Session s) throws SQLException {
        insert(s);
    }

    @Override
    public void insert(Session s) throws SQLException {
        String sql = """
                INSERT INTO session (
                    coaching_request_id, status, proposed_time_by_user, proposed_time_by_coach,
                    scheduled_at, duration, priority, objective, created_at, updated_at, price, payment_status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        Connection cnx = DbConnexion.getConnection();
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getCoachingRequestId());
            ps.setString(2, s.getStatus());
            ps.setTimestamp(3, toTimestamp(s.getProposedTimeByUser()));
            ps.setTimestamp(4, toTimestamp(s.getProposedTimeByCoach()));
            ps.setTimestamp(5, toTimestamp(s.getScheduledAt()));
            if (s.getDuration() != null) {
                ps.setInt(6, s.getDuration());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            if (s.getPriority() != null) {
                ps.setString(7, s.getPriority());
            } else {
                ps.setNull(7, Types.VARCHAR);
            }
            if (s.getObjective() != null) {
                ps.setString(8, s.getObjective());
            } else {
                ps.setNull(8, Types.VARCHAR);
            }
            ps.setTimestamp(9, toTimestamp(s.getCreatedAt()));
            ps.setTimestamp(10, toTimestamp(s.getUpdatedAt()));
            if (s.getPrice() != null) {
                ps.setDouble(11, s.getPrice());
            } else {
                ps.setNull(11, Types.DOUBLE);
            }
            if (s.getPaymentStatus() != null) {
                ps.setString(12, s.getPaymentStatus());
            } else {
                ps.setNull(12, Types.VARCHAR);
            }

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    s.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Session s) throws SQLException {
        s.setUpdatedAt(new Date());

        String sql = """
                UPDATE session SET
                    coaching_request_id = ?, status = ?, proposed_time_by_user = ?, proposed_time_by_coach = ?,
                    scheduled_at = ?, duration = ?, priority = ?, objective = ?, updated_at = ?, price = ?, payment_status = ?
                WHERE id = ?
                """;

        Connection cnx = DbConnexion.getConnection();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, s.getCoachingRequestId());
            ps.setString(2, s.getStatus());
            ps.setTimestamp(3, toTimestamp(s.getProposedTimeByUser()));
            ps.setTimestamp(4, toTimestamp(s.getProposedTimeByCoach()));
            ps.setTimestamp(5, toTimestamp(s.getScheduledAt()));
            if (s.getDuration() != null) {
                ps.setInt(6, s.getDuration());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            if (s.getPriority() != null) {
                ps.setString(7, s.getPriority());
            } else {
                ps.setNull(7, Types.VARCHAR);
            }
            if (s.getObjective() != null) {
                ps.setString(8, s.getObjective());
            } else {
                ps.setNull(8, Types.VARCHAR);
            }
            ps.setTimestamp(9, toTimestamp(s.getUpdatedAt()));
            if (s.getPrice() != null) {
                ps.setDouble(10, s.getPrice());
            } else {
                ps.setNull(10, Types.DOUBLE);
            }
            if (s.getPaymentStatus() != null) {
                ps.setString(11, s.getPaymentStatus());
            } else {
                ps.setNull(11, Types.VARCHAR);
            }
            ps.setInt(12, s.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM session WHERE id = ?";
        Connection cnx = DbConnexion.getConnection();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Session findById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM session s WHERE s.id = ?";
        Connection cnx = DbConnexion.getConnection();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Session findByCoachingRequestId(int coachingRequestId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM session s WHERE s.coaching_request_id = ?";
        Connection cnx = DbConnexion.getConnection();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coachingRequestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Sessions dont la date affichable tombe « aujourd’hui » pour le coach (équivalent
     * {@code SessionRepository::countSessionsTodayForCoach}).
     */
    public int countSessionsTodayForCoach(int coachId) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM session s
                INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
                WHERE cr.coach_id = ?
                  AND COALESCE(
                        CAST(s.scheduled_at AS date),
                        CAST(s.proposed_time_by_coach AS date),
                        CAST(s.proposed_time_by_user AS date)
                  ) = CURRENT_DATE
                """;
        Connection cnx = DbConnexion.getConnection();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coachId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Liste paginée des sessions où l’utilisateur est client ou coach (équivalent requête {@code SessionController::index}).
     */
    public List<Session> findSessionsForParticipant(int userId, int limit, int offset) throws SQLException {
        String sql = "SELECT " + COLUMNS + """
                 FROM session s
                INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
                WHERE cr.user_id = ? OR cr.coach_id = ?
                ORDER BY s.updated_at DESC NULLS LAST, s.id DESC
                LIMIT ? OFFSET ?
                """;
        List<Session> list = new ArrayList<>();
        Connection cnx = DbConnexion.getConnection();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, limit);
            ps.setInt(4, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private static Session mapRow(ResultSet rs) throws SQLException {
        Session s = new Session();
        s.setId(rs.getInt("id"));
        s.setCoachingRequestId(rs.getInt("coaching_request_id"));
        s.setStatus(rs.getString("status"));
        Timestamp t1 = rs.getTimestamp("proposed_time_by_user");
        s.setProposedTimeByUser(t1 != null ? new Date(t1.getTime()) : null);
        Timestamp t2 = rs.getTimestamp("proposed_time_by_coach");
        s.setProposedTimeByCoach(t2 != null ? new Date(t2.getTime()) : null);
        Timestamp t3 = rs.getTimestamp("scheduled_at");
        s.setScheduledAt(t3 != null ? new Date(t3.getTime()) : null);
        int dur = rs.getInt("duration");
        s.setDuration(rs.wasNull() ? null : dur);
        String pr = rs.getString("priority");
        s.setPriority(rs.wasNull() ? null : pr);
        String obj = rs.getString("objective");
        s.setObjective(rs.wasNull() ? null : obj);
        Timestamp c = rs.getTimestamp("created_at");
        if (c != null) {
            s.setCreatedAt(new Date(c.getTime()));
        }
        Timestamp u = rs.getTimestamp("updated_at");
        s.setUpdatedAt(u != null ? new Date(u.getTime()) : null);
        double price = rs.getDouble("price");
        s.setPrice(rs.wasNull() ? null : price);
        String pay = rs.getString("payment_status");
        s.setPaymentStatus(rs.wasNull() ? null : pay);
        return s;
    }

    private static Timestamp toTimestamp(Date d) {
        return d == null ? null : new Timestamp(d.getTime());
    }

    /**
     * Récupère toutes les sessions d'un coach spécifique.
     */
    public List<Session> getSessionsByCoach(int coachId) throws SQLException {
        System.out.println("[SessionService] getSessionsByCoach() coachId=" + coachId);
        String sql = "SELECT " + COLUMNS + """
                 FROM session s
                INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
                WHERE cr.coach_id = ?
                ORDER BY s.scheduled_at DESC NULLS LAST, s.updated_at DESC
                """;
        List<Session> list = new ArrayList<>();
        Connection cnx = DbConnexion.getConnection();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coachId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        System.out.println("[SessionService] sessions found for coachId=" + coachId + " -> " + list.size());
        if (list.isEmpty()) {
            debugRawSessionsForCoach(coachId);
        }
        return list;
    }

    /**
     * Debug SQL manuel quand la liste est vide, pour vérifier les données brutes.
     */
    private void debugRawSessionsForCoach(int coachId) {
        String sql = """
                SELECT s.id, s.coaching_request_id, s.status, s.scheduled_at, s.duration, s.objective
                FROM session s
                INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
                WHERE cr.coach_id = ?
                ORDER BY s.id DESC
                """;
        try {
            Connection cnx = DbConnexion.getConnection();
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, coachId);
                try (ResultSet rs = ps.executeQuery()) {
                    int rows = 0;
                    while (rs.next()) {
                        rows++;
                        System.out.println("[SessionService][debug] row#" + rows
                                + " sessionId=" + rs.getInt("id")
                                + ", requestId=" + rs.getInt("coaching_request_id")
                                + ", status=" + rs.getString("status")
                                + ", scheduledAt=" + rs.getTimestamp("scheduled_at")
                                + ", duration=" + rs.getInt("duration"));
                    }
                    System.out.println("[SessionService][debug] raw rows for coachId=" + coachId + " -> " + rows);
                }
            }
        } catch (SQLException e) {
            System.out.println("[SessionService][debug] erreur SQL debug coachId=" + coachId + " : " + e.getMessage());
        }
    }

    /**
     * Récupère toutes les sessions d'un utilisateur spécifique.
     */
    public List<Session> getSessionsByUser(int userId) throws SQLException {
        String sql = "SELECT " + COLUMNS + """
                 FROM session s
                INNER JOIN coaching_request cr ON cr.id = s.coaching_request_id
                WHERE cr.user_id = ?
                ORDER BY s.scheduled_at DESC NULLS LAST, s.updated_at DESC
                """;
        List<Session> list = new ArrayList<>();
        Connection cnx = DbConnexion.getConnection();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Ajoute une nouvelle session (alias pour create).
     */
    public void addSession(Session session) throws SQLException {
        create(session);
    }

    /**
     * Met à jour une session existante (alias pour update).
     */
    public void updateSession(Session session) throws SQLException {
        update(session);
    }

    /**
     * Supprime une session (alias pour delete).
     */
    public void deleteSession(int sessionId) throws SQLException {
        delete(sessionId);
    }
}
