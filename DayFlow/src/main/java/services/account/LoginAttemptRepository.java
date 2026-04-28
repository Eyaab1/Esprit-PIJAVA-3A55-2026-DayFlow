package services.account;

import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginAttemptRepository {

    private static final String INSERT_SQL = """
            INSERT INTO login_attempt(identifier, success, attempted_at)
            VALUES (?, ?, CURRENT_TIMESTAMP)
            """;

    private static final String COUNT_FAILED_SINCE_SQL = """
            SELECT COUNT(*)
            FROM login_attempt
            WHERE LOWER(identifier) = LOWER(?)
              AND success = false
              AND attempted_at >= (CURRENT_TIMESTAMP - (? || ' minutes')::interval)
            """;

    public void save(String identifier, boolean success) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT_SQL)) {
            ps.setString(1, identifier);
            ps.setBoolean(2, success);
            ps.executeUpdate();
        }
    }

    public int countFailedAttempts(String identifier, int lookbackMinutes) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(COUNT_FAILED_SINCE_SQL)) {
            ps.setString(1, identifier);
            ps.setInt(2, lookbackMinutes);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}
