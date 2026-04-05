package services;

import enums.UserRole;
import model.User;
import utils.PasswordHasher;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class UserService implements CRUD<User, Integer> {

    private static final Pattern EMAIL_SIMPLE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User signUp(String firstName, String lastName, String email, String rawPassword) throws SQLException {
        validateSignUp(firstName, lastName, email, rawPassword);
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userDao.findByEmail(normalizedEmail).isPresent()) {
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
            userDao.insert(user);
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
        Optional<User> found = userDao.findByEmail(email.trim().toLowerCase(Locale.ROOT));
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
        userDao.insert(entity);
    }

    @Override
    public void update(User entity) throws SQLException {
        if (entity.getId() == null) {
            throw new IllegalArgumentException("id obligatoire pour update");
        }
        userDao.update(entity);
    }

    @Override
    public void delete(Integer id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("id obligatoire pour delete");
        }
        userDao.delete(id);
    }
}
