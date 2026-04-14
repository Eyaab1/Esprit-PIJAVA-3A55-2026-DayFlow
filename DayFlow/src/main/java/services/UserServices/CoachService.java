package services.UserServices;

import enums.UserRole;
import model.user.User;
import services.CRUD;
import services.coaching_session_module.CoachPriceRange;
import services.coaching_session_module.CoachSearchParams;
import utils.DbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service dédié à la gestion des coachs (utilisateurs avec le rôle COACH).
 * Fournit des méthodes spécifiques pour rechercher, filtrer et gérer les coachs.
 */
public class CoachService implements CRUD<User, Integer> {

    private final Connection cnx;
    private final UserService userService;

    private static final String SELECT_ALL_COACHES = """
            SELECT id, first_name, last_name, email, password, roles,
                   phone_number, age, status, speciality, specialities, availability,
                   rating, review_count, price_per_session, bio, photo_url,
                   profile_picture_name, profile_picture_size
            FROM "user"
            WHERE roles::text LIKE '%ROLE_COACH%'
            ORDER BY rating DESC NULLS LAST, review_count DESC
            """;

    private static final String SELECT_COACH_BY_ID = """
            SELECT id, first_name, last_name, email, password, roles,
                   phone_number, age, status, speciality, specialities, availability,
                   rating, review_count, price_per_session, bio, photo_url,
                   profile_picture_name, profile_picture_size
            FROM "user"
            WHERE id = ? AND roles::text LIKE '%ROLE_COACH%'
            """;

    public CoachService() {
        this.cnx = DbConnexion.getInstance().getCnx();
        this.userService = new UserService();
    }

    /**
     * Récupère tous les coachs de la base de données.
     * @return Liste de tous les utilisateurs ayant le rôle COACH
     */
    public List<User> getAllCoaches() throws SQLException {
        List<User> coaches = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(SELECT_ALL_COACHES);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                coaches.add(mapRow(rs));
            }
        }
        return coaches;
    }

    /**
     * Récupère un coach par son ID.
     * @param id L'identifiant du coach
     * @return Optional contenant le coach s'il existe et a le rôle COACH
     */
    public Optional<User> getCoachById(int id) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(SELECT_COACH_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Recherche des coachs avec filtres avancés.
     * @param params Paramètres de recherche (nom, spécialité, prix, note, disponibilité)
     * @return Liste des coachs correspondant aux critères
     */
    public List<User> searchCoaches(CoachSearchParams params) throws SQLException {
        return userService.searchCoaches(params);
    }

    /**
     * Récupère toutes les spécialités disponibles parmi les coachs.
     * @return Liste des spécialités uniques
     */
    public List<String> getAllSpecialities() throws SQLException {
        return userService.findAllCoachSpecialities();
    }

    /**
     * Récupère toutes les disponibilités des coachs.
     * @return Liste des disponibilités
     */
    public List<String> getAllAvailabilities() throws SQLException {
        return userService.findAllCoachAvailabilities();
    }

    /**
     * Récupère la plage de prix des coachs (min et max).
     * @return Objet contenant le prix minimum et maximum
     */
    public CoachPriceRange getPriceRange() throws SQLException {
        return userService.getCoachPriceRange();
    }

    /**
     * Récupère les coachs les mieux notés.
     * @param limit Nombre maximum de coachs à retourner
     * @return Liste des meilleurs coachs
     */
    public List<User> getTopRatedCoaches(int limit) throws SQLException {
        String sql = SELECT_ALL_COACHES + " LIMIT ?";
        List<User> coaches = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    coaches.add(mapRow(rs));
                }
            }
        }
        return coaches;
    }

    /**
     * Récupère les coachs par spécialité.
     * @param speciality La spécialité recherchée
     * @return Liste des coachs ayant cette spécialité
     */
    public List<User> getCoachesBySpeciality(String speciality) throws SQLException {
        String sql = """
                SELECT id, first_name, last_name, email, password, roles,
                       phone_number, age, status, speciality, specialities, availability,
                       rating, review_count, price_per_session, bio, photo_url,
                       profile_picture_name, profile_picture_size
                FROM "user"
                WHERE roles::text LIKE '%ROLE_COACH%'
                  AND (speciality ILIKE ? OR specialities::text ILIKE ?)
                ORDER BY rating DESC NULLS LAST
                """;
        
        List<User> coaches = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            String pattern = "%" + speciality + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    coaches.add(mapRow(rs));
                }
            }
        }
        return coaches;
    }

    /**
     * Récupère les coachs disponibles.
     * @return Liste des coachs avec une disponibilité définie
     */
    public List<User> getAvailableCoaches() throws SQLException {
        String sql = """
                SELECT id, first_name, last_name, email, password, roles,
                       phone_number, age, status, speciality, specialities, availability,
                       rating, review_count, price_per_session, bio, photo_url,
                       profile_picture_name, profile_picture_size
                FROM "user"
                WHERE roles::text LIKE '%ROLE_COACH%'
                  AND availability IS NOT NULL
                  AND availability != ''
                ORDER BY rating DESC NULLS LAST
                """;
        
        List<User> coaches = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                coaches.add(mapRow(rs));
            }
        }
        return coaches;
    }

    /**
     * Compte le nombre total de coachs.
     * @return Nombre de coachs dans la base de données
     */
    public int countCoaches() throws SQLException {
        String sql = "SELECT COUNT(*) FROM \"user\" WHERE roles::text LIKE '%ROLE_COACH%'";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Vérifie si un utilisateur est un coach.
     * @param userId L'identifiant de l'utilisateur
     * @return true si l'utilisateur est un coach, false sinon
     */
    public boolean isCoach(int userId) throws SQLException {
        Optional<User> user = userService.findById(userId);
        return user.isPresent() && user.get().hasRole(UserRole.COACH);
    }

    // Implémentation de l'interface CRUD

    @Override
    public void create(User entity) throws SQLException {
        userService.create(entity);
    }

    @Override
    public void insert(User entity) throws SQLException {
        userService.insert(entity);
    }

    @Override
    public void update(User entity) throws SQLException {
        userService.update(entity);
    }

    @Override
    public void delete(Integer id) throws SQLException {
        userService.delete(id);
    }

    /**
     * Mappe une ligne de ResultSet vers un objet User.
     */
    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        
        // Roles
        String rolesJson = rs.getString("roles");
        if (rolesJson != null) {
            user.setRoles(parseJsonArray(rolesJson));
        }
        
        user.setPhoneNumber(rs.getString("phone_number"));
        
        int age = rs.getInt("age");
        if (!rs.wasNull()) {
            user.setAge(age);
        }
        
        user.setStatus(rs.getString("status"));
        user.setSpeciality(rs.getString("speciality"));
        
        // Specialities
        String specialitiesJson = rs.getString("specialities");
        if (specialitiesJson != null) {
            user.setSpecialities(parseJsonArray(specialitiesJson));
        }
        
        user.setAvailability(rs.getString("availability"));
        
        Double rating = rs.getDouble("rating");
        if (!rs.wasNull()) {
            user.setRating(rating);
        }
        
        int reviewCount = rs.getInt("review_count");
        if (!rs.wasNull()) {
            user.setReviewCount(reviewCount);
        }
        
        Double price = rs.getDouble("price_per_session");
        if (!rs.wasNull()) {
            user.setPricePerSession(price);
        }
        
        user.setBio(rs.getString("bio"));
        user.setPhotoUrl(rs.getString("photo_url"));
        user.setProfilePictureName(rs.getString("profile_picture_name"));
        
        int pictureSize = rs.getInt("profile_picture_size");
        if (!rs.wasNull()) {
            user.setProfilePictureSize(pictureSize);
        }
        
        return user;
    }

    /**
     * Parse un tableau JSON en liste de chaînes.
     */
    private List<String> parseJsonArray(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return new ArrayList<>();
        }
        
        List<String> result = new ArrayList<>();
        String cleaned = json.trim();
        
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        
        if (!cleaned.isEmpty()) {
            String[] parts = cleaned.split(",");
            for (String part : parts) {
                String value = part.trim().replace("\"", "");
                if (!value.isEmpty()) {
                    result.add(value);
                }
            }
        }
        
        return result;
    }
}
