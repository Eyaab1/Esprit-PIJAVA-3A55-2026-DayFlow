package tests;

import model.User;
import org.junit.jupiter.api.Test;
import services.UserService;
import utils.PasswordHasher;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class LoginPageTest {

    @Test
    void login_reussit_avecIdentifiantsValides() throws SQLException {
        String plainPassword = "secret1234";
        String hash = PasswordHasher.hash(plainPassword);

        User stored = new User();
        stored.setId(42);
        stored.setEmail("user@dayflow.test");
        stored.setFirstName("Ada");
        stored.setLastName("Lovelace");
        stored.setPassword(hash);

        UserService userService = serviceReturning(Optional.of(stored));

        Optional<User> result = userService.login("user@dayflow.test", plainPassword);

        assertTrue(result.isPresent());
        User out = result.get();
        assertEquals(42, out.getId());
        assertEquals("user@dayflow.test", out.getEmail());
        assertNull(out.getPassword(), "Le mot de passe ne doit pas être exposé après login");
    }

    @Test
    void login_normaliseEmailEnMinuscules() throws SQLException {
        String plain = "validpass12";
        User stored = new User();
        stored.setEmail("me@test.org");
        stored.setPassword(PasswordHasher.hash(plain));

        UserService userService = serviceReturning(Optional.of(stored));

        Optional<User> result = userService.login("Me@Test.ORG", plain);

        assertTrue(result.isPresent());
    }

    @Test
    void login_echoue_siMotDePasseIncorrect() throws SQLException {
        User stored = new User();
        stored.setEmail("x@test.org");
        stored.setPassword(PasswordHasher.hash("bonMotDePasse1"));

        UserService userService = serviceReturning(Optional.of(stored));

        Optional<User> result = userService.login("x@test.org", "mauvaisMotDePasse");

        assertFalse(result.isPresent());
    }

    @Test
    void login_echoue_siUtilisateurInconnu() throws SQLException {
        UserService userService = serviceReturning(Optional.empty());

        Optional<User> result = userService.login("inconnu@test.org", "nimporte");

        assertFalse(result.isPresent());
    }

    @Test
    void login_echoue_siCompteSansMotDePasse() throws SQLException {
        User googleOnly = new User();
        googleOnly.setEmail("oauth@test.org");
        googleOnly.setPassword(null);

        UserService userService = serviceReturning(Optional.of(googleOnly));

        Optional<User> result = userService.login("oauth@test.org", "quelconque");

        assertFalse(result.isPresent());
    }

    private UserService serviceReturning(Optional<User> toReturn) {
        return new UserService() {
            @Override
            public Optional<User> findByEmail(String email) {
                return toReturn;
            }
        };
    }
}
