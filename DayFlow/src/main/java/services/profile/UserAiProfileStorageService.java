package services.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.profile.AiArchetypeProfile;
import model.profile.OnboardingAnswers;
import org.postgresql.util.PGobject;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserAiProfileStorageService {
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String UPSERT_SQL = """
            INSERT INTO user_ai_profile (user_id, answers_json, profile_json, created_at, updated_at)
            VALUES (?, ?::jsonb, ?::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (user_id) DO UPDATE SET
                answers_json = EXCLUDED.answers_json,
                profile_json = EXCLUDED.profile_json,
                updated_at = CURRENT_TIMESTAMP
            """;

    private static final String FIND_SQL = """
            SELECT answers_json, profile_json
            FROM user_ai_profile
            WHERE user_id = ?
            """;

    public void saveOrUpdate(int userId, OnboardingAnswers answers, AiArchetypeProfile profile) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPSERT_SQL)) {
            ps.setInt(1, userId);
            ps.setObject(2, toJson(answers));
            ps.setObject(3, toJson(profile));
            ps.executeUpdate();
        }
    }

    public Optional<StoredAiProfile> findByUserId(int userId) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(FIND_SQL)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String answersJson = rs.getString("answers_json");
                String profileJson = rs.getString("profile_json");
                OnboardingAnswers answers = fromJson(answersJson, OnboardingAnswers.class);
                AiArchetypeProfile profile = fromJson(profileJson, AiArchetypeProfile.class);
                return Optional.of(new StoredAiProfile(answers, profile));
            }
        }
    }

    private static PGobject toJson(Object value) throws SQLException {
        PGobject json = new PGobject();
        json.setType("jsonb");
        try {
            json.setValue(JSON.writeValueAsString(value));
            return json;
        } catch (JsonProcessingException e) {
            throw new SQLException("Cannot serialize AI profile JSON", e);
        }
    }

    private static <T> T fromJson(String value, Class<T> clazz) throws SQLException {
        if (value == null || value.isBlank()) {
            throw new SQLException("Missing JSON data in user_ai_profile");
        }
        try {
            return JSON.readValue(value, clazz);
        } catch (JsonProcessingException e) {
            throw new SQLException("Cannot parse AI profile JSON", e);
        }
    }

    public record StoredAiProfile(OnboardingAnswers answers, AiArchetypeProfile profile) {
    }
}
