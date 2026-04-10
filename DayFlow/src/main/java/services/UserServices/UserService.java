package services.UserServices;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import enums.UserRole;
import model.user.User;
import org.postgresql.util.PGobject;
import services.CRUD;
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
            SELECT id, first_name, last_name, email, password, roles,
                   phone_number, age, status, speciality, availability,
                   rating, review_count, price_per_session, bio, photo_url
            FROM "user" WHERE LOWER(email) = LOWER(?)
            """;

<<<<<<< Updated upstream
=======
    private static final String SELECT_BY_ID = """
            SELECT id, first_name, last_name, email, password, roles,
                   phone_number, age, status, speciality, availability,
                   rating, review_count, price_per_session, bio, photo_url
            FROM "user" WHERE id = ?
            """;

>>>>>>> Stashed changes
    private static final String INSERT_USER = """
            INSERT INTO "user" (
                first_name, last_name, email, password, roles, status, review_count,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

    private static final String UPDATE_USER = """
            UPDATE "user" SET
                first_name = ?, last_name = ?, email = ?, password = ?,
                roles = ?, phone_number = ?, age = ?, status = ?, speciality = ?,
                availability = ?, rating = ?, review_count = ?,
                price_per_session = ?, bio = ?, photo_url = ?
            WHERE id = ?
            """;

    private static final String DELETE_USER = """
            DELETE FROM "user" WHERE id = ?
            """;

<<<<<<< Updated upstream
=======
    /** Colonnes complètes utilisateur (recherche coachs / API Symfony {@code CoachSearchController}). */
    private static final String USER_FULL_SELECT = """
            SELECT id, first_name, last_name, email, password, roles,
                   phone_number, age, status, speciality, availability,
                   rating, review_count, price_per_session, bio, photo_url
            """;

>>>>>>> Stashed changes
    public User signUp(String firstName, String lastName, String email, String rawPassword) throws SQLException {
        return signUp(firstName, lastName, email, rawPassword, UserRole.USER);
    }

    /**
     * Inscription avec rôle demandé (ex. interface graphique). Le rôle {@code ADMIN} n'est pas attribuable à l'inscription.
     */
    public User signUp(String firstName, String lastName, String email, String rawPassword, UserRole role) throws SQLException {
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
        UserRole assigned = role != null ? role : UserRole.USER;
        if (assigned == UserRole.ADMIN) {
            assigned = UserRole.USER;
        }
        user.setRoles(List.of(assigned.getValue()));
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
            throw new IllegalArgumentException("L'email est obligatoire.");
        }
        if (!EMAIL_SIMPLE.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("L'email n'est pas valide.");
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
    public void insert(User user) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {
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
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPDATE_USER)) {
            int i = 1;
            ps.setString(i++, user.getFirstName());
            ps.setString(i++, user.getLastName());
            ps.setString(i++, user.getEmail());
            ps.setString(i++, user.getPassword());
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
            ps.setInt(i, user.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null) {
            throw new SQLException("id obligatoire pour DELETE");
        }
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(DELETE_USER)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BY_EMAIL)) {
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
