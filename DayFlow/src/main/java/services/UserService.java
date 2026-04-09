package services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import enums.UserRole;
import model.User;
import org.postgresql.util.PGobject;
import utils.DbConnexion;
import utils.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class UserService implements CRUD<User, Integer> {

    private static final Pattern EMAIL_SIMPLE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
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

    public User signUp(String firstName, String lastName, String email, String rawPassword) throws SQLException {
        validateSignUp(firstName, lastName, email, rawPassword);
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Cet email est déjà utilisé. Utilisez un autre email ou connectez-vous.");
        }
        User user = new User();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(normalizedEmail);
        user.setPassword(PasswordHasher.hash(rawPassword));
        user.setRoles(List.of(UserRole.USER.getValue()));
        user.setStatus("active");
        user.setReviewCount(0);
        try {
            insert(user);
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new IllegalArgumentException("Cet email est déjà utilisé.", e);
            }
            throw e;
        }
        user.setPassword(null);
        return user;
    }

    public Optional<User> login(String email, String rawPassword) throws SQLException {
        if (email == null || email.isBlank() || rawPassword == null) {
            return Optional.empty();
        }
        Optional<User> found = findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        User user = found.get();
        String hash = user.getPassword();
        if (hash == null || hash.isBlank() || !PasswordHasher.matches(rawPassword, hash)) {
            return Optional.empty();
        }
        user.setPassword(null);
        return Optional.of(user);
    }

    private static void validateSignUp(String firstName, String lastName, String email, String rawPassword) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("Le prénom est obligatoire.");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Le nom est obligatoire.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("L’email est obligatoire.");
        }
        if (!EMAIL_SIMPLE.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("L’email n’est pas valide.");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 8 caractères.");
        }
    }

    @Override
    public void create(User entity) throws SQLException {
        insert(entity);
    }

    @Override
    public void insert(User entity) throws SQLException {
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getFirstName());
            ps.setString(2, entity.getLastName());
            ps.setString(3, entity.getEmail());
            ps.setString(4, entity.getPassword());
            ps.setObject(5, toJsonRoles(entity.getRoles()));
            ps.setString(6, entity.getStatus());
            int reviewCount = entity.getReviewCount() != null ? entity.getReviewCount() : 0;
            ps.setInt(7, reviewCount);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(User entity) throws SQLException {
        if (entity.getId() == null) {
            throw new IllegalArgumentException("id obligatoire pour update");
        }
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE_USER)) {
            int i = 1;
            ps.setString(i++, entity.getFirstName());
            ps.setString(i++, entity.getLastName());
            ps.setString(i++, entity.getEmail());
            ps.setString(i++, entity.getPassword());
            if (entity.getGoogleId() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, entity.getGoogleId());
            }
            ps.setObject(i++, toJsonRoles(entity.getRoles()));
            if (entity.getPhoneNumber() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, entity.getPhoneNumber());
            }
            if (entity.getAge() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, entity.getAge());
            }
            ps.setString(i++, entity.getStatus());
            if (entity.getSpeciality() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, entity.getSpeciality());
            }
            if (entity.getSpecialities() == null) {
                ps.setNull(i++, Types.OTHER);
            } else {
                ps.setObject(i++, toJsonList(entity.getSpecialities()));
            }
            if (entity.getAvailability() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, entity.getAvailability());
            }
            if (entity.getRating() == null) {
                ps.setNull(i++, Types.DOUBLE);
            } else {
                ps.setDouble(i++, entity.getRating());
            }
            if (entity.getReviewCount() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, entity.getReviewCount());
            }
            if (entity.getPricePerSession() == null) {
                ps.setNull(i++, Types.DOUBLE);
            } else {
                ps.setDouble(i++, entity.getPricePerSession());
            }
            if (entity.getBio() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, entity.getBio());
            }
            if (entity.getPhotoUrl() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, entity.getPhotoUrl());
            }
            if (entity.getProfilePictureName() == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, entity.getProfilePictureName());
            }
            if (entity.getProfilePictureSize() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, entity.getProfilePictureSize());
            }
            ps.setInt(i, entity.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("id obligatoire pour delete");
        }
        try (Connection c = DbConnexion.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE_USER)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

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
