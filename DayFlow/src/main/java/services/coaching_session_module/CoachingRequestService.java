package services.coaching_session_module;

import dto.coaching_session.CoachingRequestAIResponse;
import model.coaching_session.CoachingRequest;
import model.user.User;
import services.CRUD;
import utils.DbConnexion;

import java.io.IOException;
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

    private final Connection cnx;
    private final boolean hasDetectedNeedColumn;
    private final boolean hasCompatibilityScoreColumn;
    private final boolean hasJustificationColumn;
    private final boolean hasAssignedCoachIdColumn;
    private final AIService aiService;

    public CoachingRequestService() {
        cnx = DbConnexion.getInstance().getCnx();
        hasDetectedNeedColumn = hasColumn("coaching_request", "detected_need");
        hasCompatibilityScoreColumn = hasColumn("coaching_request", "compatibility_score");
        hasJustificationColumn = hasColumn("coaching_request", "justification");
        hasAssignedCoachIdColumn = hasColumn("coaching_request", "assigned_coach_id");
        aiService = new AIService();
    }

  public CoachingRequestAIResponse analyzeMessage(String userMessage) {
    try {
        AIService.RecommendationResult aiResult = aiService.recommendCoach(userMessage);

        if (aiResult == null
                || aiResult.recommendedCoach() == null
                || aiResult.recommendedCoach().getId() == null) {
            return CoachingRequestAIResponse.failure(
                    "Aucun coach adapté n'a été trouvé automatiquement."
            );
        }

        String coachName = aiResult.recommendedCoach().getFirstName()
                + " "
                + aiResult.recommendedCoach().getLastName();

        return CoachingRequestAIResponse.success(
                aiResult.detectedNeed(),
                aiResult.recommendedCoach().getId(),
                coachName.trim(),
                aiResult.compatibilityScore(),
                aiResult.justification()
        );

    } catch (IllegalArgumentException e) {
        return CoachingRequestAIResponse.failure(e.getMessage());

    } catch (IllegalStateException e) {
        return CoachingRequestAIResponse.failure(
                "Service IA indisponible: " + e.getMessage()
        );

    } catch (SQLException e) {
        return CoachingRequestAIResponse.failure(
                "Erreur accès coachs: " + e.getMessage()
        );

    } catch (IOException e) {
        return CoachingRequestAIResponse.failure(
                "Erreur Hugging Face: " + e.getMessage()
        );

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return CoachingRequestAIResponse.failure(
                "Analyse interrompue."
        );
    }
}

       
        
    

    @Override
    public void create(CoachingRequest entity) throws SQLException {
        insert(entity);
    }

    @Override
    public void insert(CoachingRequest r) throws SQLException {
        StringBuilder columns = new StringBuilder("""
                user_id, coach_id, message, status, created_at, responded_at,
                goal, level, frequency, budget, coaching_type, priority, time_slot_id
                """);
        StringBuilder placeholders = new StringBuilder("?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?");
        if (hasDetectedNeedColumn) {
            columns.append(", detected_need");
            placeholders.append(", ?");
        }
        if (hasCompatibilityScoreColumn) {
            columns.append(", compatibility_score");
            placeholders.append(", ?");
        }
        if (hasJustificationColumn) {
            columns.append(", justification");
            placeholders.append(", ?");
        }
        if (hasAssignedCoachIdColumn) {
            columns.append(", assigned_coach_id");
            placeholders.append(", ?");
        }
        String sql = "INSERT INTO coaching_request (" + columns + ") VALUES (" + placeholders + ")";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int idx = 1;
            ps.setInt(idx++, r.getUserId());
            ps.setInt(idx++, r.getCoachId());
            ps.setString(idx++, r.getMessage());
            ps.setString(idx++, r.getStatus());
            ps.setTimestamp(idx++, toTimestamp(r.getCreatedAt()));
            ps.setTimestamp(idx++, toTimestamp(r.getRespondedAt()));
            ps.setString(idx++, r.getGoal());
            ps.setString(idx++, r.getLevel());
            ps.setString(idx++, r.getFrequency());
            if (r.getBudget() != null) {
                ps.setDouble(idx++, r.getBudget());
            } else {
                ps.setNull(idx++, Types.DOUBLE);
            }
            ps.setString(idx++, r.getCoachingType());
            ps.setString(idx++, r.getPriority());
            if (r.getTimeSlotId() != null) {
                ps.setInt(idx++, r.getTimeSlotId());
            } else {
                ps.setNull(idx++, Types.INTEGER);
            }
            if (hasDetectedNeedColumn) {
                ps.setString(idx++, r.getDetectedNeed());
            }
            if (hasCompatibilityScoreColumn) {
                if (r.getCompatibilityScore() != null) {
                    ps.setInt(idx++, r.getCompatibilityScore());
                } else {
                    ps.setNull(idx++, Types.INTEGER);
                }
            }
            if (hasJustificationColumn) {
                ps.setString(idx++, r.getJustification());
            }
            if (hasAssignedCoachIdColumn) {
                if (r.getAssignedCoachId() != null) {
                    ps.setInt(idx++, r.getAssignedCoachId());
                } else {
                    ps.setNull(idx++, Types.INTEGER);
                }
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
        StringBuilder sql = new StringBuilder("""
                UPDATE coaching_request SET
                    message = ?, status = ?, goal = ?, level = ?, frequency = ?, budget = ?,
                    coaching_type = ?, priority = ?, responded_at = ?, time_slot_id = ?
                """);
        if (hasDetectedNeedColumn) {
            sql.append(", detected_need = ?");
        }
        if (hasCompatibilityScoreColumn) {
            sql.append(", compatibility_score = ?");
        }
        if (hasJustificationColumn) {
            sql.append(", justification = ?");
        }
        if (hasAssignedCoachIdColumn) {
            sql.append(", assigned_coach_id = ?");
        }
        sql.append(" WHERE id = ?");
        try (PreparedStatement ps = cnx.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setString(idx++, r.getMessage());
            ps.setString(idx++, r.getStatus());
            ps.setString(idx++, r.getGoal());
            ps.setString(idx++, r.getLevel());
            ps.setString(idx++, r.getFrequency());
            if (r.getBudget() != null) {
                ps.setDouble(idx++, r.getBudget());
            } else {
                ps.setNull(idx++, Types.DOUBLE);
            }
            ps.setString(idx++, r.getCoachingType());
            ps.setString(idx++, r.getPriority());
            ps.setTimestamp(idx++, toTimestamp(r.getRespondedAt()));
            if (r.getTimeSlotId() != null) {
                ps.setInt(idx++, r.getTimeSlotId());
            } else {
                ps.setNull(idx++, Types.INTEGER);
            }
            if (hasDetectedNeedColumn) {
                ps.setString(idx++, r.getDetectedNeed());
            }
            if (hasCompatibilityScoreColumn) {
                if (r.getCompatibilityScore() != null) {
                    ps.setInt(idx++, r.getCompatibilityScore());
                } else {
                    ps.setNull(idx++, Types.INTEGER);
                }
            }
            if (hasJustificationColumn) {
                ps.setString(idx++, r.getJustification());
            }
            if (hasAssignedCoachIdColumn) {
                if (r.getAssignedCoachId() != null) {
                    ps.setInt(idx++, r.getAssignedCoachId());
                } else {
                    ps.setNull(idx++, Types.INTEGER);
                }
            }
            ps.setInt(idx, r.getId());

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
        String sql = "SELECT * FROM coaching_request WHERE id = ?";
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
        String sql = "SELECT * FROM coaching_request WHERE " + column + " = ? ORDER BY created_at DESC";
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
        try {
            r.setDetectedNeed(rs.getString("detected_need"));
        } catch (SQLException ignored) {
            // Colonne absente : compat legacy
        }
        try {
            int score = rs.getInt("compatibility_score");
            r.setCompatibilityScore(rs.wasNull() ? null : score);
        } catch (SQLException ignored) {
            // Colonne absente : compat legacy
        }
        try {
            r.setJustification(rs.getString("justification"));
        } catch (SQLException ignored) {
            // Colonne absente : compat legacy
        }
        try {
            int assigned = rs.getInt("assigned_coach_id");
            r.setAssignedCoachId(rs.wasNull() ? null : assigned);
        } catch (SQLException ignored) {
            // Colonne absente : compat legacy
        }
        r.setStatus(rs.getString("status"));
        return r;
    }

    private static Timestamp toTimestamp(Date d) {
        return d == null ? null : new Timestamp(d.getTime());
    }

    private boolean hasColumn(String tableName, String columnName) {
        try {
            DatabaseMetaData metaData = cnx.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
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

    /** Ligne enrichie pour l’écran admin « Demandes coach » (client + coach + spécialité). */
    public record AdminCoachRequestRow(
            int id,
            String clientFirstName,
            String clientLastName,
            String coachFirstName,
            String coachLastName,
            String coachSpeciality,
            String status,
            String message,
            Date createdAt
    ) {
        public String clientFullName() {
            return (nz(clientFirstName) + " " + nz(clientLastName)).trim();
        }

        public String coachFullName() {
            return (nz(coachFirstName) + " " + nz(coachLastName)).trim();
        }

        private static String nz(String s) {
            return s == null ? "" : s;
        }
    }

    public List<AdminCoachRequestRow> findAllForAdmin() throws SQLException {
        String sql = """
                SELECT cr.id, cr.status, cr.message, cr.created_at,
                       uf.first_name AS ufn, uf.last_name AS uln,
                       cf.first_name AS cfn, cf.last_name AS cln, cf.speciality AS cspec
                FROM coaching_request cr
                INNER JOIN "user" uf ON uf.id = cr.user_id
                INNER JOIN "user" cf ON cf.id = cr.coach_id
                ORDER BY cr.created_at DESC
                """;
        List<AdminCoachRequestRow> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("created_at");
                list.add(new AdminCoachRequestRow(
                        rs.getInt("id"),
                        rs.getString("ufn"),
                        rs.getString("uln"),
                        rs.getString("cfn"),
                        rs.getString("cln"),
                        rs.getString("cspec"),
                        rs.getString("status"),
                        rs.getString("message"),
                        ts != null ? new Date(ts.getTime()) : null
                ));
            }
        }
        return list;
    }
}

