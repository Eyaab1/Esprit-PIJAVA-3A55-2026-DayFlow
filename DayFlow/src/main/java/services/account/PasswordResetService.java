package services.account;

import model.user.User;
import utils.PasswordHasher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_VALIDITY_MINUTES = 30;

    private final UserService userService;
    private final PasswordResetTokenRepository tokenRepository;
    private final ResetMailSenderService resetMailSenderService;

    public PasswordResetService() {
        this(new UserService(), new PasswordResetTokenRepository(), new ResetMailSenderService());
    }

    public PasswordResetService(UserService userService,
                                PasswordResetTokenRepository tokenRepository,
                                ResetMailSenderService resetMailSenderService) {
        this.userService = userService;
        this.tokenRepository = tokenRepository;
        this.resetMailSenderService = resetMailSenderService;
    }

    public void requestReset(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            return;
        }
        Optional<User> found = userService.findByEmail(email.trim().toLowerCase());
        if (found.isEmpty() || found.get().getId() == null) {
            return;
        }
        String token = generateToken();
        String tokenHash = sha256(token);
        tokenRepository.create(found.get().getId(), tokenHash, LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES));

        boolean sent = resetMailSenderService.sendPasswordResetEmail(email, token, TOKEN_VALIDITY_MINUTES);
        if (!sent) {
            // Fallback for local development when SMTP is not configured.
            System.out.println("[MOCK-EMAIL] Password reset token for " + email + ": " + token);
            System.out.println("[MOCK-EMAIL] Token valid for " + TOKEN_VALIDITY_MINUTES + " minutes.");
            System.out.println("[MOCK-EMAIL] SMTP disabled or invalid config. Configure app.mail.* to send real emails.");
        }
    }

    public boolean resetPassword(String rawToken, String newPassword) throws SQLException {
        if (rawToken == null || rawToken.isBlank() || newPassword == null || newPassword.length() < 8) {
            return false;
        }
        String tokenHash = sha256(rawToken.trim());
        Optional<PasswordResetTokenRepository.ResetTokenRow> tokenRow = tokenRepository.findValidByHash(tokenHash);
        if (tokenRow.isEmpty()) {
            return false;
        }
        PasswordResetTokenRepository.ResetTokenRow row = tokenRow.get();
        User user = userService.findById(row.userId()).orElse(null);
        if (user == null || user.getId() == null) {
            return false;
        }
        user.setPassword(PasswordHasher.hash(newPassword));
        userService.update(user);
        tokenRepository.markUsed(row.id());
        return true;
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
