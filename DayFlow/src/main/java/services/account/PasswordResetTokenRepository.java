package services.account;

import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

public class PasswordResetTokenRepository {

    private static final String INSERT_SQL = """
            INSERT INTO password_reset_token(user_id, token_hash, expires_at, used, created_at)
            VALUES (?, ?, ?, false, CURRENT_TIMESTAMP)
            """;

    private static final String FIND_VALID_SQL = """
            SELECT id, user_id, token_hash, expires_at, used, created_at
            FROM password_reset_token
            WHERE token_hash = ?
              AND used = false
              AND expires_at > CURRENT_TIMESTAMP
            ORDER BY id DESC
            LIMIT 1
            """;

    private static final String MARK_USED_SQL = """
            UPDATE password_reset_token
            SET used = true
            WHERE id = ?
            """;

    public void create(int userId, String tokenHash, LocalDateTime expiresAt) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(INSERT_SQL)) {
            ps.setInt(1, userId);
            ps.setString(2, tokenHash);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        }
    }

    public Optional<ResetTokenRow> findValidByHash(String tokenHash) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(FIND_VALID_SQL)) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ResetTokenRow(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("token_hash")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    public void markUsed(int id) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(MARK_USED_SQL)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public record ResetTokenRow(int id, int userId, String tokenHash) {
    }
}
