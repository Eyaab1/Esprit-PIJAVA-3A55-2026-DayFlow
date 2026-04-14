package services.coaching_session_module;

import model.coaching_session.CoachingRequest;
import model.user.User;
import services.CRUD;
import utils.DbConnexion;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Persistance {@code coaching_request} alignée sur l’entité Symfony {@code CoachingRequest}
 * (workflow statuts, métadonnées, {@code responded_at}, {@code time_slot_id}).
 */
public class CoachingRequestService implements CRUD<CoachingRequest, Integer> {

    private static final String COLUMNS = """
            id, user_id, coach_id, message, status, created_at, responded_at,
            goal, level, frequency, budget, coaching_type, priority, time_slot_id
            """;

    private final Connection cnx;

    public CoachingRequestService() {
        cnx = DbConnexion.getInstance().getCnx();
    }

    @Override
    public void create(CoachingRequest entity) throws SQLException {
        insert(entity);
    }

    @Override
    public void insert(CoachingRequest r) throws SQLException {
        String sql = """
                INSERT INTO coaching_request (
                    user_id, coach_id, message, status, created_at, responded_at,
                    goal, level, frequency, budget, coaching_type, priority, time_slot_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getUserId());
            ps.setInt(2, r.getCoachId());
            ps.setString(3, r.getMessage());
            ps.setString(4, r.getStatus());
            ps.setTimestamp(5, toTimestamp(r.getCreatedAt()));
            ps.setTimestamp(6, toTimestamp(r.getRespondedAt()));
            ps.setString(7, r.getGoal());
            ps.setString(8, r.getLevel());
            ps.setString(9, r.getFrequency());
            if (r.getBudget() != null) {
                ps.setDouble(10, r.getBudget());
            } else {
                ps.setNull(10, Types.DOUBLE);
            }
            ps.setString(11, r.getCoachingType());
            ps.setString(12, r.getPriority());
            if (r.getTimeSlotId() != null) {
                ps.setInt(13, r.getTimeSlotId());
            } else {
                ps.setNull(13, Types.INTEGER);
            }

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    r.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(CoachingRequest r) throws SQLException {
        String sql = """
                UPDATE coaching_request SET
                    message = ?, status = ?, goal = ?, level = ?, frequency = ?, budget = ?,
                    coaching_type = ?, priority = ?, responded_at = ?, time_slot_id = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, r.getMessage());
            ps.setString(2, r.getStatus());
            ps.setString(3, r.getGoal());
            ps.setString(4, r.getLevel());
            ps.setString(5, r.getFrequency());
            if (r.getBudget() != null) {
                ps.setDouble(6, r.getBudget());
            } else {
                ps.setNull(6, Types.DOUBLE);
            }
            ps.setString(7, r.getCoachingType());
            ps.setString(8, r.getPriority());
            ps.setTimestamp(9, toTimestamp(r.getRespondedAt()));
            if (r.getTimeSlotId() != null) {
                ps.setInt(10, r.getTimeSlotId());
            } else {
                ps.setNull(10, Types.INTEGER);
            }
            ps.setInt(11, r.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM coaching_request WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Optional<CoachingRequest> findById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM coaching_request WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<CoachingRequest> findByUserId(int userId) throws SQLException {
        return findByColumn("user_id", userId);
    }

    public CoachingRequest createRequest(User coach, User user, String message, String status) throws SQLException {
        if (coach == null || coach.getId() == null) {
            throw new IllegalArgumentException("Coach invalide");
        }
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Utilisateur invalide");
        }

        String safeMessage = (message == null || message.trim().isEmpty())
                ? "Demande sans créneau."
                : message.trim();

        CoachingRequest request = new CoachingRequest();
        request.setCoachId(coach.getId());
        request.setUserId(user.getId());
        request.setMessage(safeMessage);
        request.setStatus((status == null || status.isBlank()) ? CoachingRequest.STATUS_PENDING : status);
        insert(request);
        return request;
    }

    public List<CoachingRequest> getRequestsByUser(User user) throws SQLException {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Utilisateur invalide");
        }
        return findByUserId(user.getId());
    }

    public List<CoachingRequest> findByCoachId(int coachId) throws SQLException {
        return findByColumn("coach_id", coachId);
    }

    /**
     * Équivalent {@code CoachingRequestRepository::findForCoachWithFilters} (recherche + filtres).
     */
    public List<CoachingRequest> findForCoachWithFilters(int coachId, CoachRequestListFilters filters) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT cr.id, cr.user_id, cr.coach_id, cr.message, cr.status, cr.created_at, cr.responded_at,
                       cr.goal, cr.level, cr.frequency, cr.budget, cr.coaching_type, cr.priority, cr.time_slot_id
                FROM coaching_request cr
                INNER JOIN "user" client ON client.id = cr.user_id
                WHERE cr.coach_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(coachId);

        String search = filters.search() != null ? filters.search().trim() : "";
        if (!search.isEmpty()) {
            sql.append("""
                     AND (
                        cr.message ILIKE ? OR client.first_name ILIKE ? OR client.last_name ILIKE ?
                        OR client.email ILIKE ? OR LOWER(client.first_name || ' ' || client.last_name) LIKE ?
                    )
                    """);
            String p = "%" + search + "%";
            params.add(p);
            params.add(p);
            params.add(p);
            params.add(p);
            params.add("%" + search.toLowerCase() + "%");
        }

        String st = filters.status() != null ? filters.status().trim() : "";
        if (!st.isEmpty()) {
            sql.append(" AND cr.status = ? ");
            params.add(st);
        }

        String df = filters.dateFrom() != null ? filters.dateFrom().trim() : "";
        if (!df.isEmpty()) {
            sql.append(" AND cr.created_at >= ? ");
            LocalDate d = LocalDate.parse(df);
            params.add(Timestamp.valueOf(d.atStartOfDay()));
        }

        String dt = filters.dateTo() != null ? filters.dateTo().trim() : "";
        if (!dt.isEmpty()) {
            sql.append(" AND cr.created_at < ? ");
            LocalDate d = LocalDate.parse(dt).plusDays(1);
            params.add(Timestamp.valueOf(d.atStartOfDay()));
        }

        String pr = filters.priority() != null ? filters.priority().trim() : "";
        if (!pr.isEmpty()) {
            sql.append(" AND cr.priority = ? ");
            params.add(pr);
        }

        sql.append(" ORDER BY cr.created_at DESC ");

        List<CoachingRequest> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                bindParam(ps, i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private static void bindParam(PreparedStatement ps, int index, Object value) throws SQLException {
        switch (value) {
            case Integer i -> ps.setInt(index, i);
            case String s -> ps.setString(index, s);
            case Timestamp t -> ps.setTimestamp(index, t);
            case Double d -> ps.setDouble(index, d);
            default -> ps.setObject(index, value);
        }
    }

    public int countAllForCoach(int coachId) throws SQLException {
        return countWhereCoach(coachId, null, null);
    }

    public int countByStatusForCoach(int coachId, String status) throws SQLException {
        return countWhereCoach(coachId, "status", status);
    }

    public int countByPriorityForCoach(int coachId, String priority) throws SQLException {
        return countWhereCoach(coachId, "priority", priority);
    }

    private int countWhereCoach(int coachId, String extraColumn, String extraValue) throws SQLException {
        String sql = "SELECT COUNT(*) FROM coaching_request WHERE coach_id = ?";
        if (extraColumn != null && extraValue != null) {
            sql += " AND " + extraColumn + " = ?";
        }
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coachId);
            if (extraColumn != null) {
                ps.setString(2, extraValue);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Statistiques tableau de bord coach (équivalent bloc {@code stats} Symfony).
     */
    public CoachStats buildCoachStats(int coachId, SessionService sessionService) throws SQLException {
        int total = countAllForCoach(coachId);
        int pending = countByStatusForCoach(coachId, model.coaching_session.CoachingRequest.STATUS_PENDING);
        int accepted = countByStatusForCoach(coachId, model.coaching_session.CoachingRequest.STATUS_ACCEPTED);
        int declined = countByStatusForCoach(coachId, model.coaching_session.CoachingRequest.STATUS_DECLINED);
        int urgent = countByPriorityForCoach(coachId, model.coaching_session.CoachingRequest.PRIORITY_URGENT);
        int medium = countByPriorityForCoach(coachId, model.coaching_session.CoachingRequest.PRIORITY_MEDIUM);
        int normal = countByPriorityForCoach(coachId, model.coaching_session.CoachingRequest.PRIORITY_NORMAL);
        int sessionsToday = sessionService.countSessionsTodayForCoach(coachId);
        double conversion = total > 0 ? Math.round(accepted * 1000.0 / total) / 10.0 : 0.0;
        return new CoachStats(total, pending, accepted, declined, sessionsToday, conversion, urgent, medium, normal);
    }

    private List<CoachingRequest> findByColumn(String column, int value) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM coaching_request WHERE " + column + " = ? ORDER BY created_at DESC";
        List<CoachingRequest> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private static CoachingRequest mapRow(ResultSet rs) throws SQLException {
        CoachingRequest r = new CoachingRequest();
        r.setId(rs.getInt("id"));
        r.setUserId(rs.getInt("user_id"));
        r.setCoachId(rs.getInt("coach_id"));
        r.setMessage(rs.getString("message"));
        Timestamp c = rs.getTimestamp("created_at");
        if (c != null) {
            r.setCreatedAt(new Date(c.getTime()));
        }
        Timestamp resp = rs.getTimestamp("responded_at");
        r.setRespondedAt(resp != null ? new Date(resp.getTime()) : null);
        r.setGoal(rs.getString("goal"));
        r.setLevel(rs.getString("level"));
        r.setFrequency(rs.getString("frequency"));
        double b = rs.getDouble("budget");
        r.setBudget(rs.wasNull() ? null : b);
        r.setCoachingType(rs.getString("coaching_type"));
        r.setPriority(rs.getString("priority"));
        int ts = rs.getInt("time_slot_id");
        r.setTimeSlotId(rs.wasNull() ? null : ts);
        r.setStatus(rs.getString("status"));
        return r;
    }

    private static Timestamp toTimestamp(Date d) {
        return d == null ? null : new Timestamp(d.getTime());
    }

    /**
     * Récupère toutes les demandes pour un coach spécifique.
     */
    public List<CoachingRequest> getRequestsByCoach(int coachId) throws SQLException {
        return findByCoachId(coachId);
    }

    /**
     * Met à jour le statut d'une demande.
     */
    public void updateStatus(int requestId, String newStatus) throws SQLException {
        String sql = "UPDATE coaching_request SET status = ?, responded_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, requestId);
            ps.executeUpdate();
        }
    }
}

