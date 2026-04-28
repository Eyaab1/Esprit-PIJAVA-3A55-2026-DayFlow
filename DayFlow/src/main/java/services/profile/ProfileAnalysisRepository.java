package services.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.profile.ProfileAnalysisHistoryItem;
import model.profile.ProfileAnalysisResult;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProfileAnalysisRepository {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String INSERT_SQL = """
            INSERT INTO profile_analysis (user_id, score, recommendations_json, analyzed_at)
            VALUES (?, ?, ?::jsonb, CURRENT_TIMESTAMP)
            """;

    private static final String FIND_BY_USER_SQL = """
            SELECT id, user_id, score, recommendations_json, analyzed_at
            FROM profile_analysis
            WHERE user_id = ?
            ORDER BY analyzed_at DESC
            """;

    private static final String FIND_FILTERED_SQL = """
            SELECT id, user_id, score, recommendations_json, analyzed_at
            FROM profile_analysis
            WHERE user_id = ?
              AND (? IS NULL OR score >= ?)
              AND (? IS NULL OR analyzed_at::date >= ?)
            ORDER BY
              CASE WHEN ? = 'SCORE' THEN score END DESC,
              analyzed_at DESC
            """;

    public void saveAnalysis(int userId, ProfileAnalysisResult result) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setInt(2, result.getScore());
            ps.setString(3, toJson(result.getRecommendations()));
            ps.executeUpdate();
        }
    }

    public List<ProfileAnalysisHistoryItem> findByUserId(int userId) throws SQLException {
        Connection c = DbConnexion.getConnection();
        List<ProfileAnalysisHistoryItem> history = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(FIND_BY_USER_SQL)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProfileAnalysisHistoryItem item = new ProfileAnalysisHistoryItem();
                    item.setId(rs.getInt("id"));
                    item.setUserId(rs.getInt("user_id"));
                    item.setScore(rs.getInt("score"));
                    item.setRecommendationsJson(rs.getString("recommendations_json"));
                    Timestamp analyzedAt = rs.getTimestamp("analyzed_at");
                    item.setAnalyzedAt(analyzedAt == null ? null : analyzedAt.toLocalDateTime());
                    history.add(item);
                }
            }
        }
        return history;
    }

    public List<ProfileAnalysisHistoryItem> findByUserIdFiltered(int userId, Integer minScore, LocalDate fromDate, String sortBy)
            throws SQLException {
        Connection c = DbConnexion.getConnection();
        List<ProfileAnalysisHistoryItem> history = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(FIND_FILTERED_SQL)) {
            ps.setInt(1, userId);
            if (minScore == null) {
                ps.setNull(2, Types.INTEGER);
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(2, minScore);
                ps.setInt(3, minScore);
            }
            if (fromDate == null) {
                ps.setNull(4, Types.DATE);
                ps.setNull(5, Types.DATE);
            } else {
                ps.setDate(4, java.sql.Date.valueOf(fromDate));
                ps.setDate(5, java.sql.Date.valueOf(fromDate));
            }
            ps.setString(6, "SCORE".equalsIgnoreCase(sortBy) ? "SCORE" : "DATE");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProfileAnalysisHistoryItem item = new ProfileAnalysisHistoryItem();
                    item.setId(rs.getInt("id"));
                    item.setUserId(rs.getInt("user_id"));
                    item.setScore(rs.getInt("score"));
                    item.setRecommendationsJson(rs.getString("recommendations_json"));
                    Timestamp analyzedAt = rs.getTimestamp("analyzed_at");
                    item.setAnalyzedAt(analyzedAt == null ? null : analyzedAt.toLocalDateTime());
                    history.add(item);
                }
            }
        }
        return history;
    }

    public List<String> parseRecommendations(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return JSON.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String toJson(List<String> values) throws SQLException {
        try {
            return JSON.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            throw new SQLException("Cannot serialize recommendations to JSON", e);
        }
    }
}
