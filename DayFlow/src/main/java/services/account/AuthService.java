package services.account;

import enums.UserRole;
import model.user.User;
import utils.PasswordHasher;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class AuthService {

    private final UserService userService;
    private final AccountLockoutService lockoutService;
    private final AccountSecurityService accountSecurityService;
    private final GoogleOAuthService googleOAuthService;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final IpGeolocationService ipGeolocationService;
    private final SecurityAlertMailService securityAlertMailService;

    public AuthService() {
        this(
                new UserService(),
                new AccountLockoutService(),
                new AccountSecurityService(),
                new GoogleOAuthService(),
                new OAuthAccountRepository(),
                new IpGeolocationService(),
                new SecurityAlertMailService()
        );
    }

    public AuthService(UserService userService,
                       AccountLockoutService lockoutService,
                       AccountSecurityService accountSecurityService,
                       GoogleOAuthService googleOAuthService,
                       OAuthAccountRepository oAuthAccountRepository) {
        this(
                userService,
                lockoutService,
                accountSecurityService,
                googleOAuthService,
                oAuthAccountRepository,
                new IpGeolocationService(),
                new SecurityAlertMailService()
        );
    }

    public AuthService(UserService userService,
                       AccountLockoutService lockoutService,
                       AccountSecurityService accountSecurityService,
                       GoogleOAuthService googleOAuthService,
                       OAuthAccountRepository oAuthAccountRepository,
                       IpGeolocationService ipGeolocationService,
                       SecurityAlertMailService securityAlertMailService) {
        this.userService = userService;
        this.lockoutService = lockoutService;
        this.accountSecurityService = accountSecurityService;
        this.googleOAuthService = googleOAuthService;
        this.oAuthAccountRepository = oAuthAccountRepository;
        this.ipGeolocationService = ipGeolocationService;
        this.securityAlertMailService = securityAlertMailService;
    }

    public Optional<User> login(String email, String rawPassword) throws SQLException {
        return loginDetailed(email, rawPassword).user();
    }

    public LoginResult loginDetailed(String email, String rawPassword) throws SQLException {
        if (email == null || email.isBlank() || rawPassword == null) {
            return new LoginResult(Optional.empty(), null);
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        int lockMinutes = lockoutService.currentLockMinutes(normalized);
        if (lockMinutes > 0) {
            throw new IllegalStateException("Too many failed attempts. Account locked for " + lockMinutes + " minutes.");
        }

        Optional<User> user = userService.login(normalized, rawPassword);
        lockoutService.recordAttempt(normalized, user.isPresent());
        if (user.isEmpty() || user.get().getId() == null) {
            return new LoginResult(Optional.empty(), null);
        }
        ensureAccountIsAllowedToLogin(user.get());

        int recentFailed = lockoutService.recentFailedAttempts(normalized);
        AccountSecurityService.LoginSuccessMeta loginMeta = accountSecurityService.registerSuccessfulLogin(
                user.get().getId(),
                normalized,
                recentFailed
        );
        notifySuspiciousLogin(user.get(), loginMeta);
        return new LoginResult(user, loginMeta);
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

    private void notifySuspiciousLogin(User user, AccountSecurityService.LoginSuccessMeta loginMeta) {
        if (user == null || loginMeta == null || !loginMeta.suspicious()) {
            return;
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        IpGeolocationService.GeoInfo geo = ipGeolocationService.resolve(loginMeta.ipAddress());
        String displayName = (user.getFirstName() == null ? "" : user.getFirstName().trim()) + " "
                + (user.getLastName() == null ? "" : user.getLastName().trim());
        securityAlertMailService.sendSuspiciousLoginAlert(
                user.getEmail(),
                displayName.trim(),
                loginMeta.suspiciousReason(),
                loginMeta.deviceLabel(),
                geo.ipAddress(),
                geo.locationLabel()
        );
    }

    public record LoginResult(Optional<User> user, AccountSecurityService.LoginSuccessMeta securityMeta) {
    }

    private void ensureAccountIsAllowedToLogin(User user) throws SQLException {
        if (user == null || user.getId() == null) {
            return;
        }
        String status = user.getStatus() == null ? "" : user.getStatus().trim().toLowerCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        if ("temp_banned".equals(status)) {
            LocalDateTime until = user.getBannedUntil();
            if (until == null || now.isBefore(until)) {
                String formattedDate = until != null 
                    ? until.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))
                    : "date inconnue";
                throw new IllegalStateException("Votre compte est temporairement suspendu jusqu'au " + formattedDate + " pour non-respect des règles de la communauté.");
            }
            // Ban expiré - réactiver le compte automatiquement
            userService.updateModerationStatus(user.getId(), "active", null, null);
            user.setStatus("active");
            user.setBannedUntil(null);
            user.setBanReason(null);
            return;
        }
        if ("banned".equals(status) || "permanent_banned".equals(status)) {
            throw new IllegalStateException("Votre compte a été banni définitivement pour non-respect des règles de la communauté.");
        }
    }
}
