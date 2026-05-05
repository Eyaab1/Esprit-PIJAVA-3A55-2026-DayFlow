package services.coaching_session_module;

import dto.coaching_session.ProgressReport;
import model.coaching_session.CoachingRequest;
import model.coaching_session.Session;
import model.coaching_session.SessionEvaluation;
import model.coaching_session.SessionFeedback;
import utils.DbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProgressService {

    private final Connection cnx;
    private final SessionService sessionService;
    private final CoachingRequestService coachingRequestService;
    private final EvaluationService evaluationService;

    public ProgressService() {
        this.cnx = DbConnexion.getInstance().getCnx();
        this.sessionService = new SessionService(false);
        this.coachingRequestService = new CoachingRequestService();
        this.evaluationService = new EvaluationService();
        ensureTablesExist();
    }

    private void ensureTablesExist() {
        String trackingTable = """
                CREATE TABLE IF NOT EXISTS progress_tracking (
                    id SERIAL PRIMARY KEY,
                    coaching_request_id INTEGER NOT NULL UNIQUE,
                    user_id INTEGER NOT NULL,
                    coach_id INTEGER NOT NULL,
                    current_score INTEGER NOT NULL DEFAULT 0 CHECK (current_score BETWEEN 0 AND 100),
                    previous_score INTEGER NOT NULL DEFAULT 0 CHECK (previous_score BETWEEN 0 AND 100),
                    score_change INTEGER NOT NULL DEFAULT 0,
                    last_session_id INTEGER,
                    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_progress_request FOREIGN KEY (coaching_request_id) REFERENCES coaching_request(id) ON DELETE CASCADE,
                    CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
                    CONSTRAINT fk_progress_coach FOREIGN KEY (coach_id) REFERENCES "user"(id) ON DELETE CASCADE,
                    CONSTRAINT fk_progress_last_session FOREIGN KEY (last_session_id) REFERENCES session(id) ON DELETE SET NULL
                )
                """;
        String historyTable = """
                CREATE TABLE IF NOT EXISTS progress_history (
                    id SERIAL PRIMARY KEY,
                    progress_tracking_id INTEGER NOT NULL,
                    session_id INTEGER NOT NULL UNIQUE,
                    score_before INTEGER NOT NULL CHECK (score_before BETWEEN 0 AND 100),
                    score_after INTEGER NOT NULL CHECK (score_after BETWEEN 0 AND 100),
                    score_change INTEGER NOT NULL,
                    summary TEXT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_progress_history_tracking FOREIGN KEY (progress_tracking_id) REFERENCES progress_tracking(id) ON DELETE CASCADE,
                    CONSTRAINT fk_progress_history_session FOREIGN KEY (session_id) REFERENCES session(id) ON DELETE CASCADE
                )
                """;
        try (Statement st = cnx.createStatement()) {
            st.execute(trackingTable);
            st.execute(historyTable);
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'initialiser le suivi de progression", e);
        }
    }

    public void processCompletedSession(int sessionId) throws SQLException {
        Session session = sessionService.findById(sessionId);
        if (session == null || !Session.STATUS_COMPLETED.equals(session.getStatus())) {
            return;
        }
        CoachingRequest request = coachingRequestService.findById(session.getCoachingRequestId())
                .orElseThrow(() -> new IllegalStateException("CoachingRequest introuvable"));

        ProgressState state = getOrCreateProgressState(request);
        int previousScore = state.currentScore();
        int newScore = computeProgressScore(request.getId(), sessionId);
        int change = newScore - previousScore;

        updateProgressState(state.id(), newScore, previousScore, change, sessionId);
        upsertProgressHistory(state.id(), sessionId, previousScore, newScore, buildSummary(sessionId, change));
    }

    public int computeProgressScore(int coachingRequestId, int lastSessionId) throws SQLException {
        int totalSessions = countSessionsByRequest(coachingRequestId);
        int completedSessions = countCompletedSessionsByRequest(coachingRequestId);
        int participationScore = totalSessions == 0 ? 0 : clamp((int) Math.round((completedSessions * 100.0) / totalSessions));

        Optional<SessionEvaluation> evalOpt = evaluationService.findEvaluationBySessionId(lastSessionId);
        Optional<SessionFeedback> feedbackOpt = evaluationService.findFeedbackBySessionId(lastSessionId);

        int objectiveScore = evalOpt.map(SessionEvaluation::getGoalAchievementScore).orElse(50);
        int disciplineScore = evalOpt.map(SessionEvaluation::getDisciplineScore).orElse(participationScore);
        int evolutionScore = evalOpt.map(SessionEvaluation::getEvolutionScore).orElse(50);
        int coachFeedbackScore = evalOpt.map(SessionEvaluation::getCoachFeedbackScore).orElse(50);

        int userRatingScore = feedbackOpt
                .map(SessionFeedback::getCoachRating)
                .map(rating -> clamp(rating * 20))
                .orElse(50);

        int weighted = (int) Math.round(
                participationScore * 0.25
                        + objectiveScore * 0.25
                        + disciplineScore * 0.20
                        + evolutionScore * 0.20
                        + ((coachFeedbackScore + userRatingScore) / 2.0) * 0.10
        );

        int delta = evalOpt.map(SessionEvaluation::getProgressDelta).orElse(0);
        return clamp(weighted + delta);
    }

    public ProgressReport generateProgressReport(int coachingRequestId) throws SQLException {
        CoachingRequest request = coachingRequestService.findById(coachingRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Demande de coaching introuvable"));

        ProgressState state = getOrCreateProgressState(request);
        List<ProgressReport.ProgressSnapshot> snapshots = new ArrayList<>();

        String sql = """
                SELECT s.id AS session_id, s.status, ph.score_before, ph.score_after, ph.score_change,
                       sf.coach_rating, sf.user_feedback, sf.user_comment,
                       se.coach_remarks, se.recommendations, se.next_action, se.program_adjustment
                FROM session s
                LEFT JOIN progress_history ph ON ph.session_id = s.id
                LEFT JOIN session_feedback sf ON sf.session_id = s.id
                LEFT JOIN session_evaluation se ON se.session_id = s.id
                WHERE s.coaching_request_id = ?
                ORDER BY s.created_at ASC, s.id ASC
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coachingRequestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    snapshots.add(new ProgressReport.ProgressSnapshot(
                            rs.getInt("session_id"),
                            rs.getString("status"),
                            getNullableInt(rs, "score_before"),
                            getNullableInt(rs, "score_after"),
                            getNullableInt(rs, "score_change"),
                            getNullableInt(rs, "coach_rating"),
                            rs.getString("user_feedback"),
                            rs.getString("user_comment"),
                            rs.getString("coach_remarks"),
                            rs.getString("recommendations"),
                            rs.getString("next_action"),
                            rs.getString("program_adjustment")
                    ));
                }
            }
        }

        String recommendation = inferOverallRecommendation(state.currentScore(), snapshots);
        return new ProgressReport(
                coachingRequestId,
                request.getUserId(),
                request.getCoachId(),
                state.currentScore(),
                state.previousScore(),
                state.scoreChange(),
                snapshots,
                recommendation
        );
    }

    private ProgressState getOrCreateProgressState(CoachingRequest request) throws SQLException {
        String select = "SELECT id, current_score, previous_score, score_change FROM progress_tracking WHERE coaching_request_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(select)) {
            ps.setInt(1, request.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ProgressState(
                            rs.getInt("id"),
                            rs.getInt("current_score"),
                            rs.getInt("previous_score"),
                            rs.getInt("score_change")
                    );
                }
            }
        }

        String insert = """
                INSERT INTO progress_tracking (coaching_request_id, user_id, coach_id, current_score, previous_score, score_change)
                VALUES (?, ?, ?, 0, 0, 0)
                RETURNING id, current_score, previous_score, score_change
                """;
        try (PreparedStatement ps = cnx.prepareStatement(insert)) {
            ps.setInt(1, request.getId());
            ps.setInt(2, request.getUserId());
            ps.setInt(3, request.getCoachId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new ProgressState(
                        rs.getInt("id"),
                        rs.getInt("current_score"),
                        rs.getInt("previous_score"),
                        rs.getInt("score_change")
                );
            }
        }
    }

    private void updateProgressState(int trackingId, int current, int previous, int change, int sessionId) throws SQLException {
        String sql = """
                UPDATE progress_tracking
                SET current_score = ?, previous_score = ?, score_change = ?, last_session_id = ?, last_updated = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, current);
            ps.setInt(2, previous);
            ps.setInt(3, change);
            ps.setInt(4, sessionId);
            ps.setInt(5, trackingId);
            ps.executeUpdate();
        }
    }

    private void upsertProgressHistory(int trackingId, int sessionId, int before, int after, String summary) throws SQLException {
        String sql = """
                INSERT INTO progress_history (progress_tracking_id, session_id, score_before, score_after, score_change, summary)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (session_id) DO UPDATE SET
                    score_before = EXCLUDED.score_before,
                    score_after = EXCLUDED.score_after,
                    score_change = EXCLUDED.score_change,
                    summary = EXCLUDED.summary
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, trackingId);
            ps.setInt(2, sessionId);
            ps.setInt(3, before);
            ps.setInt(4, after);
            ps.setInt(5, after - before);
            ps.setString(6, summary);
            ps.executeUpdate();
        }
    }

    private String buildSummary(int sessionId, int scoreChange) throws SQLException {
        Optional<SessionEvaluation> evaluation = evaluationService.findEvaluationBySessionId(sessionId);
        Optional<SessionFeedback> feedback = evaluationService.findFeedbackBySessionId(sessionId);

        String recommendations = evaluation.map(SessionEvaluation::getRecommendations).orElse(null);
        String userFeedback = feedback.map(SessionFeedback::getUserFeedback).orElse(null);

        StringBuilder sb = new StringBuilder();
        sb.append("Variation score: ").append(scoreChange >= 0 ? "+" : "").append(scoreChange).append(". ");
        if (recommendations != null && !recommendations.isBlank()) {
            sb.append("Reco coach: ").append(recommendations).append(". ");
        }
        if (userFeedback != null && !userFeedback.isBlank()) {
            sb.append("Feedback user: ").append(userFeedback);
        }
        return sb.toString().trim();
    }

    private int countSessionsByRequest(int coachingRequestId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM session WHERE coaching_request_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coachingRequestId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int countCompletedSessionsByRequest(int coachingRequestId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM session WHERE coaching_request_id = ? AND status = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coachingRequestId);
            ps.setString(2, Session.STATUS_COMPLETED);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private String inferOverallRecommendation(int score, List<ProgressReport.ProgressSnapshot> snapshots) {
        if (score < 40) {
            return "Reinitialiser le plan avec des objectifs plus courts et un suivi hebdomadaire strict.";
        }
        if (score < 70) {
            return "Conserver le plan actuel, renforcer la discipline et valider un objectif mesurable par session.";
        }
        if (snapshots.stream().anyMatch(s -> s.programAdjustment() != null && !s.programAdjustment().isBlank())) {
            return "Progression solide, appliquer les derniers ajustements proposes par le coach.";
        }
        return "Excellent rythme: maintenir la regularite et passer a des objectifs de niveau superieur.";
    }

    private static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private record ProgressState(int id, int currentScore, int previousScore, int scoreChange) {
    }
}
