package services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.User;
import org.postgresql.util.PGobject;
import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SELECT_BY_EMAIL = """
            SELECT id, first_name, last_name, email, password, google_id, roles,
                   phone_number, age, status, speciality, specialities, availability,
                   rating, review_count, price_per_session, bio, photo_url,
                   profile_picture_name, profile_picture_size
            FROM "user" WHERE LOWER(email) = LOWER(?)
            """;

    private static final String INSERT_USER = """
            INSERT INTO "user" (
                first_name, last_name, email, password, roles, status, review_count
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    public Optional<User> findByEmail(String email) throws SQLException {
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void insert(User user) throws SQLException {
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFirstName());
            ps.setString(2, user.getLastName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            PGobject jsonRoles = new PGobject();
            jsonRoles.setType("json");
            try {
                jsonRoles.setValue(JSON.writeValueAsString(user.getRoles()));
            } catch (JsonProcessingException e) {
                throw new SQLException("Sérialisation roles JSON impossible", e);
            }
            ps.setObject(5, jsonRoles);
            ps.setString(6, user.getStatus());
            int reviewCount = user.getReviewCount() != null ? user.getReviewCount() : 0;
            ps.setInt(7, reviewCount);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setFirstName(rs.getString("first_name"));
        u.setLastName(rs.getString("last_name"));
        u.setEmail(rs.getString("email"));
        String pwd = rs.getString("password");
        u.setPassword(rs.wasNull() ? null : pwd);
        u.setGoogleId(rs.getString("google_id"));
        if (rs.wasNull()) {
            u.setGoogleId(null);
        }
        u.setRoles(readStringList(rs.getString("roles")));
        u.setPhoneNumber(rs.getString("phone_number"));
        if (rs.wasNull()) {
            u.setPhoneNumber(null);
        }
        int age = rs.getInt("age");
        u.setAge(rs.wasNull() ? null : age);
        u.setStatus(rs.getString("status"));
        u.setSpeciality(rs.getString("speciality"));
        if (rs.wasNull()) {
            u.setSpeciality(null);
        }
        u.setSpecialities(readStringListNullable(rs.getString("specialities")));
        u.setAvailability(rs.getString("availability"));
        if (rs.wasNull()) {
            u.setAvailability(null);
        }
        double rating = rs.getDouble("rating");
        u.setRating(rs.wasNull() ? null : rating);
        int reviewCount = rs.getInt("review_count");
        u.setReviewCount(rs.wasNull() ? null : reviewCount);
        double price = rs.getDouble("price_per_session");
        u.setPricePerSession(rs.wasNull() ? null : price);
        u.setBio(rs.getString("bio"));
        if (rs.wasNull()) {
            u.setBio(null);
        }
        u.setPhotoUrl(rs.getString("photo_url"));
        if (rs.wasNull()) {
            u.setPhotoUrl(null);
        }
        u.setProfilePictureName(rs.getString("profile_picture_name"));
        if (rs.wasNull()) {
            u.setProfilePictureName(null);
        }
        int picSize = rs.getInt("profile_picture_size");
        u.setProfilePictureSize(rs.wasNull() ? null : picSize);
        return u;
    }

    private static List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return JSON.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private static List<String> readStringListNullable(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JSON.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
