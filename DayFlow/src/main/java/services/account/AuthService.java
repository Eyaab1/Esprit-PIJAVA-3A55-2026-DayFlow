package services.account;

import enums.UserRole;
import model.user.User;
import utils.PasswordHasher;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class AuthService {

    private final UserService userService;
    private final AccountLockoutService lockoutService;
    private final GoogleOAuthService googleOAuthService;
    private final OAuthAccountRepository oAuthAccountRepository;

    public AuthService() {
        this(new UserService(), new AccountLockoutService(), new GoogleOAuthService(), new OAuthAccountRepository());
    }

    public AuthService(UserService userService,
                       AccountLockoutService lockoutService,
                       GoogleOAuthService googleOAuthService,
                       OAuthAccountRepository oAuthAccountRepository) {
        this.userService = userService;
        this.lockoutService = lockoutService;
        this.googleOAuthService = googleOAuthService;
        this.oAuthAccountRepository = oAuthAccountRepository;
    }

    public Optional<User> login(String email, String rawPassword) throws SQLException {
        if (email == null || email.isBlank() || rawPassword == null) {
            return Optional.empty();
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (lockoutService.isLocked(normalized)) {
            throw new IllegalStateException("Too many failed attempts. Please wait 15 minutes and try again.");
        }

        Optional<User> user = userService.login(normalized, rawPassword);
        lockoutService.recordAttempt(normalized, user.isPresent());
        return user;
    }

    public User loginWithGoogle() throws Exception {
        GoogleOAuthService.GoogleProfile profile = googleOAuthService.authenticateWithLocalCallback();
        String email = profile.email().trim().toLowerCase(Locale.ROOT);
        Optional<User> existing = userService.findByEmail(email);
        User user;
        if (existing.isPresent()) {
            user = existing.get();
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                user.setPassword(PasswordHasher.hash(UUID.randomUUID().toString() + "!Aa1"));
                userService.update(user);
            }
        } else {
            String firstName = (profile.givenName() == null || profile.givenName().isBlank()) ? "Google" : profile.givenName();
            String lastName = (profile.familyName() == null || profile.familyName().isBlank()) ? "User" : profile.familyName();
            String generatedPassword = UUID.randomUUID() + "_Aa1!";
            user = userService.signUp(firstName, lastName, email, generatedPassword, UserRole.USER);
        }
        if (user.getId() != null && profile.googleUserId() != null && !profile.googleUserId().isBlank()) {
            oAuthAccountRepository.upsertGoogleLink(profile.googleUserId(), email, user.getId());
        }
        user.setPassword(null);
        return user;
    }
}
