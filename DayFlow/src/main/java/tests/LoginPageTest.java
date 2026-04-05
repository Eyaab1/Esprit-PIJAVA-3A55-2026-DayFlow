package tests;

import model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import services.UserDao;
import services.UserService;
import utils.PasswordHasher;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Page de tests : vérifie que la chaîne login (BCrypt + {@link UserService}) est correctement câblée.
 */
@ExtendWith(MockitoExtension.class)
class LoginPageTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserService userService;

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

        when(userDao.findByEmail("user@dayflow.test")).thenReturn(Optional.of(stored));

        Optional<User> result = userService.login("user@dayflow.test", plainPassword);

        assertTrue(result.isPresent());
        User out = result.get();
        assertEquals(42, out.getId());
        assertEquals("user@dayflow.test", out.getEmail());
        assertNull(out.getPassword(), "Le mot de passe ne doit pas être exposé après login");
        verify(userDao).findByEmail(eq("user@dayflow.test"));
    }

    @Test
    void login_normaliseEmailEnMinuscules() throws SQLException {
        String plain = "validpass12";
        User stored = new User();
        stored.setEmail("me@test.org");
        stored.setPassword(PasswordHasher.hash(plain));

        when(userDao.findByEmail("me@test.org")).thenReturn(Optional.of(stored));

        Optional<User> result = userService.login("Me@Test.ORG", plain);

        assertTrue(result.isPresent());
        verify(userDao).findByEmail("me@test.org");
    }

    @Test
    void login_echoue_siMotDePasseIncorrect() throws SQLException {
        User stored = new User();
        stored.setEmail("x@test.org");
        stored.setPassword(PasswordHasher.hash("bonMotDePasse1"));

        when(userDao.findByEmail("x@test.org")).thenReturn(Optional.of(stored));

        Optional<User> result = userService.login("x@test.org", "mauvaisMotDePasse");

        assertFalse(result.isPresent());
    }

    @Test
    void login_echoue_siUtilisateurInconnu() throws SQLException {
        when(userDao.findByEmail("inconnu@test.org")).thenReturn(Optional.empty());

        Optional<User> result = userService.login("inconnu@test.org", "nimporte");

        assertFalse(result.isPresent());
    }

    @Test
    void login_echoue_siCompteSansMotDePasse() throws SQLException {
        User googleOnly = new User();
        googleOnly.setEmail("oauth@test.org");
        googleOnly.setPassword(null);

        when(userDao.findByEmail("oauth@test.org")).thenReturn(Optional.of(googleOnly));

        Optional<User> result = userService.login("oauth@test.org", "quelconque");

        assertFalse(result.isPresent());
    }

    @Test
    void login_accepteHashSymfony_2y() throws SQLException {
        String plain = "symfonyCompatible8";
        String realHash = PasswordHasher.hash(plain);
        String asY = "$2y$" + realHash.substring(4);

        User stored = new User();
        stored.setEmail("symfony@test.org");
        stored.setPassword(asY);

        when(userDao.findByEmail("symfony@test.org")).thenReturn(Optional.of(stored));

        assertTrue(userService.login("symfony@test.org", plain).isPresent());
    }
}
