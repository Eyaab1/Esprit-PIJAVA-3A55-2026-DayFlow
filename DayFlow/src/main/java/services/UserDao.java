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
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao implements CRUD<User, Integer> {

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

    private static final String UPDATE_USER = """
            UPDATE "user" SET
                first_name = ?, last_name = ?, email = ?, password = ?, google_id = ?,
                roles = ?, phone_number = ?, age = ?, status = ?, speciality = ?,
                specialities = ?, availability = ?, rating = ?, review_count = ?,
                price_per_session = ?, bio = ?, photo_url = ?,
                profile_picture_name = ?, profile_picture_size = ?
            WHERE id = ?
            """;

    private static final String DELETE_USER = """
            DELETE FROM "user" WHERE id = ?
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

    @Override
    public void create(User user) throws SQLException {
        insert(user);
    }

    @Override
    public void insert(User user) throws SQLException {
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFirstName());
            ps.setString(2, user.getLastName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setObject(5, toJsonRoles(user.getRoles()));
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

    @Override
    public void update(User user) throws SQLException {
        if (user.getId() == null) {
            throw new SQLException("id obligatoire pour UPDATE");
        }
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE_USER)) {
            int i = 1;
            ps.setString(i++, user.getFirstName());
            ps.setString(i++, user.getLastName());
            ps.setString(i++, user.getEmail());
            ps.setString(i++, user.getPassword());
            if (user.getGoogleId() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, user.getGoogleId());
            }
            ps.setObject(i++, toJsonRoles(user.getRoles()));
            if (user.getPhoneNumber() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, user.getPhoneNumber());
            }
            if (user.getAge() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, user.getAge());
            }
            ps.setString(i++, user.getStatus());
            if (user.getSpeciality() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, user.getSpeciality());
            }
            if (user.getSpecialities() == null) {
                ps.setNull(i++, Types.OTHER);
            } else {
                ps.setObject(i++, toJsonList(user.getSpecialities()));
            }
            if (user.getAvailability() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, user.getAvailability());
            }
            if (user.getRating() == null) {
                ps.setNull(i++, Types.DOUBLE);
            } else {
                ps.setDouble(i++, user.getRating());
            }
            if (user.getReviewCount() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, user.getReviewCount());
            }
            if (user.getPricePerSession() == null) {
                ps.setNull(i++, Types.DOUBLE);
            } else {
                ps.setDouble(i++, user.getPricePerSession());
            }
            if (user.getBio() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, user.getBio());
            }
            if (user.getPhotoUrl() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, user.getPhotoUrl());
            }
            if (user.getProfilePictureName() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, user.getProfilePictureName());
            }
            if (user.getProfilePictureSize() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, user.getProfilePictureSize());
            }
            ps.setInt(i, user.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null) {
            throw new SQLException("id obligatoire pour DELETE");
        }
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE_USER)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private PGobject toJsonRoles(List<String> roles) throws SQLException {
        PGobject json = new PGobject();
        json.setType("json");
        try {
            json.setValue(JSON.writeValueAsString(roles != null ? roles : List.of()));
        } catch (JsonProcessingException e) {
            throw new SQLException("Sérialisation roles JSON impossible", e);
        }
        return json;
    }

    private PGobject toJsonList(List<String> values) throws SQLException {
        PGobject json = new PGobject();
        json.setType("json");
        try {
            json.setValue(JSON.writeValueAsString(values));
        } catch (JsonProcessingException e) {
            throw new SQLException("Sérialisation JSON impossible", e);
        }
        return json;
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
