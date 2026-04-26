package services.account;

import utils.DbConnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class OAuthAccountRepository {

    private static final String UPSERT_SQL = """
            INSERT INTO oauth_account(provider, provider_user_id, email, user_id, linked_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (provider, provider_user_id)
            DO UPDATE SET email = EXCLUDED.email, user_id = EXCLUDED.user_id
            """;

    public void upsertGoogleLink(String googleUserId, String email, int userId) throws SQLException {
        Connection c = DbConnexion.getConnection();
        try (PreparedStatement ps = c.prepareStatement(UPSERT_SQL)) {
            ps.setString(1, "GOOGLE");
            ps.setString(2, googleUserId);
            ps.setString(3, email);
            ps.setInt(4, userId);
            ps.executeUpdate();
        }
    }
}
