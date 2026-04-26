package services.account;

import java.sql.SQLException;

public class AccountLockoutService {

    private static final int SHORT_WINDOW_MINUTES = 15;
    private static final int MEDIUM_WINDOW_MINUTES = 60;
    private static final int LONG_WINDOW_MINUTES = 24 * 60;

    private final LoginAttemptRepository loginAttemptRepository;

    public AccountLockoutService() {
        this(new LoginAttemptRepository());
    }

    public AccountLockoutService(LoginAttemptRepository loginAttemptRepository) {
        this.loginAttemptRepository = loginAttemptRepository;
    }

    public boolean isLocked(String identifier) throws SQLException {
        return currentLockMinutes(identifier) > 0;
    }

    public int currentLockMinutes(String identifier) throws SQLException {
        int failed15 = loginAttemptRepository.countFailedAttempts(identifier, SHORT_WINDOW_MINUTES);
        int failed60 = loginAttemptRepository.countFailedAttempts(identifier, MEDIUM_WINDOW_MINUTES);
        int failed24h = loginAttemptRepository.countFailedAttempts(identifier, LONG_WINDOW_MINUTES);

        if (failed24h >= 12) {
            return LONG_WINDOW_MINUTES;
        }
        if (failed60 >= 8) {
            return MEDIUM_WINDOW_MINUTES;
        }
        if (failed15 >= 5) {
            return SHORT_WINDOW_MINUTES;
        }
        return 0;
    }

    public int recentFailedAttempts(String identifier) throws SQLException {
        return loginAttemptRepository.countFailedAttempts(identifier, SHORT_WINDOW_MINUTES);
    }

    public void recordAttempt(String identifier, boolean success) throws SQLException {
        loginAttemptRepository.save(identifier, success);
    }
}
