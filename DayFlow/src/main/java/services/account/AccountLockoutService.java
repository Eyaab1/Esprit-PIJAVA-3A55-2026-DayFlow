package services.account;

import java.sql.SQLException;

public class AccountLockoutService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int WINDOW_MINUTES = 15;

    private final LoginAttemptRepository loginAttemptRepository;

    public AccountLockoutService() {
        this(new LoginAttemptRepository());
    }

    public AccountLockoutService(LoginAttemptRepository loginAttemptRepository) {
        this.loginAttemptRepository = loginAttemptRepository;
    }

    public boolean isLocked(String identifier) throws SQLException {
        int count = loginAttemptRepository.countFailedAttempts(identifier, WINDOW_MINUTES);
        return count >= MAX_FAILED_ATTEMPTS;
    }

    public void recordAttempt(String identifier, boolean success) throws SQLException {
        loginAttemptRepository.save(identifier, success);
    }
}
