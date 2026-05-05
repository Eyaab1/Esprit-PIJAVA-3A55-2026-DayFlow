package services.coaching_session_module;

import model.coaching_session.CoachingRequest;
import model.coaching_session.Session;
import model.coaching_session.SessionEvaluation;
import model.coaching_session.SessionFeedback;
import utils.DbConnexion;

import java.sql.*;
import java.util.Optional;

public class EvaluationService {

    private final Connection cnx;
    private final CoachingRequestService coachingRequestService;
    private final SessionService sessionService;

    public EvaluationService() {
        this.cnx = DbConnexion.getInstance().getCnx();
        this.coachingRequestService = new CoachingRequestService();
        this.sessionService = new SessionService(false);
        ensureTablesExist();
    }

    private void ensureTablesExist() {
        String feedbackTable = """
                CREATE TABLE IF NOT EXISTS session_feedback (
                    id SERIAL PRIMARY KEY,
                    session_id INTEGER NOT NULL UNIQUE,
                    coaching_request_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    coach_id INTEGER NOT NULL,
                    coach_rating INTEGER CHECK (coach_rating BETWEEN 1 AND 5),
                    user_feedback TEXT,
                    user_comment TEXT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_feedback_session FOREIGN KEY (session_id) REFERENCES session(id) ON DELETE CASCADE,
                    CONSTRAINT fk_feedback_request FOREIGN KEY (coaching_request_id) REFERENCES coaching_request(id) ON DELETE CASCADE,
                    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
                    CONSTRAINT fk_feedback_coach FOREIGN KEY (coach_id) REFERENCES "user"(id) ON DELETE CASCADE
                )
                """;
        String evaluationTable = """
                CREATE TABLE IF NOT EXISTS session_evaluation (
                    id SERIAL PRIMARY KEY,
                    session_id INTEGER NOT NULL UNIQUE,
                    coaching_request_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    coach_id INTEGER NOT NULL,
                    progress_delta INTEGER CHECK (progress_delta BETWEEN -30 AND 30),
                    discipline_score INTEGER CHECK (discipline_score BETWEEN 0 AND 100),
                    goal_achievement_score INTEGER CHECK (goal_achievement_score BETWEEN 0 AND 100),
                    evolution_score INTEGER CHECK (evolution_score BETWEEN 0 AND 100),
                    coach_feedback_score INTEGER CHECK (coach_feedback_score BETWEEN 0 AND 100),
                    coach_remarks TEXT,
                    recommendations TEXT,
                    next_action TEXT,
                    program_adjustment TEXT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_eval_session FOREIGN KEY (session_id) REFERENCES session(id) ON DELETE CASCADE,
                    CONSTRAINT fk_eval_request FOREIGN KEY (coaching_request_id) REFERENCES coaching_request(id) ON DELETE CASCADE,
                    CONSTRAINT fk_eval_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
                    CONSTRAINT fk_eval_coach FOREIGN KEY (coach_id) REFERENCES "user"(id) ON DELETE CASCADE
                )
                """;
        try (Statement st = cnx.createStatement()) {
            st.execute(feedbackTable);
            st.execute(evaluationTable);
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'initialiser les tables de feedback/evaluation", e);
        }
    }

    public SessionFeedback submitUserFeedback(
            int sessionId,
            Integer coachRating,
            String userFeedback,
            String userComment
    ) throws SQLException {
        Session session = getCompletedSession(sessionId);
        CoachingRequest request = getRequestOrFail(session.getCoachingRequestId());

        String sql = """
                INSERT INTO session_feedback (
                    session_id, coaching_request_id, user_id, coach_id, coach_rating, user_feedback, user_comment
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (session_id) DO UPDATE SET
                    coach_rating = EXCLUDED.coach_rating,
                    user_feedback = EXCLUDED.user_feedback,
                    user_comment = EXCLUDED.user_comment,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING *
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, request.getId());
            ps.setInt(3, request.getUserId());
            ps.setInt(4, request.getCoachId());
            if (coachRating != null) {
                ps.setInt(5, coachRating);
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setString(6, userFeedback);
            ps.setString(7, userComment);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapFeedback(rs);
            }
        }
    }

    public SessionEvaluation submitCoachEvaluation(int sessionId, SessionEvaluation evaluation) throws SQLException {
        Session session = getCompletedSession(sessionId);
        CoachingRequest request = getRequestOrFail(session.getCoachingRequestId());

        String sql = """
                INSERT INTO session_evaluation (
                    session_id, coaching_request_id, user_id, coach_id, progress_delta, discipline_score,
                    goal_achievement_score, evolution_score, coach_feedback_score, coach_remarks,
                    recommendations, next_action, program_adjustment
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (session_id) DO UPDATE SET
                    progress_delta = EXCLUDED.progress_delta,
                    discipline_score = EXCLUDED.discipline_score,
                    goal_achievement_score = EXCLUDED.goal_achievement_score,
                    evolution_score = EXCLUDED.evolution_score,
                    coach_feedback_score = EXCLUDED.coach_feedback_score,
                    coach_remarks = EXCLUDED.coach_remarks,
                    recommendations = EXCLUDED.recommendations,
                    next_action = EXCLUDED.next_action,
                    program_adjustment = EXCLUDED.program_adjustment,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING *
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, request.getId());
            ps.setInt(3, request.getUserId());
            ps.setInt(4, request.getCoachId());
            bindNullableInt(ps, 5, evaluation.getProgressDelta());
            bindNullableInt(ps, 6, evaluation.getDisciplineScore());
            bindNullableInt(ps, 7, evaluation.getGoalAchievementScore());
            bindNullableInt(ps, 8, evaluation.getEvolutionScore());
            bindNullableInt(ps, 9, evaluation.getCoachFeedbackScore());
            ps.setString(10, evaluation.getCoachRemarks());
            ps.setString(11, evaluation.getRecommendations());
            ps.setString(12, evaluation.getNextAction());
            ps.setString(13, evaluation.getProgramAdjustment());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapEvaluation(rs);
            }
        }
    }

    public Optional<SessionFeedback> findFeedbackBySessionId(int sessionId) throws SQLException {
        String sql = "SELECT * FROM session_feedback WHERE session_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapFeedback(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<SessionEvaluation> findEvaluationBySessionId(int sessionId) throws SQLException {
        String sql = "SELECT * FROM session_evaluation WHERE session_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapEvaluation(rs));
                }
            }
        }
        return Optional.empty();
    }

    public String generateRecommendationTemplate(SessionEvaluation evaluation) {
        int discipline = evaluation.getDisciplineScore() != null ? evaluation.getDisciplineScore() : 50;
        int goals = evaluation.getGoalAchievementScore() != null ? evaluation.getGoalAchievementScore() : 50;
        int evolution = evaluation.getEvolutionScore() != null ? evaluation.getEvolutionScore() : 50;

        if (discipline < 40) {
            return "Renforcer la regularite: fixer un planning hebdomadaire et suivre les engagements.";
        }
        if (goals < 50) {
            return "Reviser les objectifs en sous-objectifs mesurables pour la prochaine session.";
        }
        if (evolution < 50) {
            return "Ajuster le programme avec davantage de pratique guidee entre les sessions.";
        }
        return "Maintenir le rythme actuel et augmenter progressivement la difficulte des objectifs.";
    }

    private Session getCompletedSession(int sessionId) throws SQLException {
        Session session = sessionService.findById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session introuvable");
        }
        if (!Session.STATUS_COMPLETED.equals(session.getStatus())) {
            throw new IllegalStateException("Le feedback/evaluation est disponible uniquement pour une session terminee");
        }
        return session;
    }

    private CoachingRequest getRequestOrFail(int coachingRequestId) throws SQLException {
        return coachingRequestService.findById(coachingRequestId)
                .orElseThrow(() -> new IllegalStateException("CoachingRequest introuvable"));
    }

    private SessionFeedback mapFeedback(ResultSet rs) throws SQLException {
        SessionFeedback feedback = new SessionFeedback();
        feedback.setId(rs.getInt("id"));
        feedback.setSessionId(rs.getInt("session_id"));
        feedback.setCoachingRequestId(rs.getInt("coaching_request_id"));
        feedback.setUserId(rs.getInt("user_id"));
        feedback.setCoachId(rs.getInt("coach_id"));
        int rating = rs.getInt("coach_rating");
        feedback.setCoachRating(rs.wasNull() ? null : rating);
        feedback.setUserFeedback(rs.getString("user_feedback"));
        feedback.setUserComment(rs.getString("user_comment"));
        feedback.setCreatedAt(toDate(rs.getTimestamp("created_at")));
        feedback.setUpdatedAt(toDate(rs.getTimestamp("updated_at")));
        return feedback;
    }

    private SessionEvaluation mapEvaluation(ResultSet rs) throws SQLException {
        SessionEvaluation evaluation = new SessionEvaluation();
        evaluation.setId(rs.getInt("id"));
        evaluation.setSessionId(rs.getInt("session_id"));
        evaluation.setCoachingRequestId(rs.getInt("coaching_request_id"));
        evaluation.setUserId(rs.getInt("user_id"));
        evaluation.setCoachId(rs.getInt("coach_id"));
        evaluation.setProgressDelta(getNullableInt(rs, "progress_delta"));
        evaluation.setDisciplineScore(getNullableInt(rs, "discipline_score"));
        evaluation.setGoalAchievementScore(getNullableInt(rs, "goal_achievement_score"));
        evaluation.setEvolutionScore(getNullableInt(rs, "evolution_score"));
        evaluation.setCoachFeedbackScore(getNullableInt(rs, "coach_feedback_score"));
        evaluation.setCoachRemarks(rs.getString("coach_remarks"));
        evaluation.setRecommendations(rs.getString("recommendations"));
        evaluation.setNextAction(rs.getString("next_action"));
        evaluation.setProgramAdjustment(rs.getString("program_adjustment"));
        evaluation.setCreatedAt(toDate(rs.getTimestamp("created_at")));
        evaluation.setUpdatedAt(toDate(rs.getTimestamp("updated_at")));
        return evaluation;
    }

    private static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static void bindNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private static java.util.Date toDate(Timestamp timestamp) {
        return timestamp == null ? null : new java.util.Date(timestamp.getTime());
    }
}
