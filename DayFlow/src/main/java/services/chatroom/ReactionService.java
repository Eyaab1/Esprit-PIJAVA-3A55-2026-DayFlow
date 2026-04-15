package services.chatroom;

import model.chatroom.Reaction;
import utils.DbConnexion;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CRUD réactions sur les messages.
 * Règle : 1 réaction par (message, user) — upsert via ON CONFLICT.
 */
public class ReactionService {

    private final Connection cnx;

    public ReactionService() {
        cnx = DbConnexion.getConnection();
    }

    // ── CREATE / UPDATE ───────────────────────────────────────────────────

    /**
     * Ajoute ou change la réaction d'un utilisateur sur un message.
     * Si l'utilisateur a déjà réagi → met à jour le type.
     */
    public void addOrUpdate(int messageId, int userId, String type) throws SQLException {
        // Validation
        Reaction r = new Reaction(messageId, userId, type); // lance IllegalArgumentException si invalide

        String sql = """
                INSERT INTO reaction (message_id, user_id, type, created_at)
                VALUES (?, ?, ?, NOW())
                ON CONFLICT (message_id, user_id)
                DO UPDATE SET type = EXCLUDED.type
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, r.getMessageId());
            ps.setInt(2, r.getUserId());
            ps.setString(3, r.getType());
            ps.executeUpdate();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    /** Supprime la réaction d'un utilisateur sur un message. */
    public void delete(int messageId, int userId) throws SQLException {
        if (messageId <= 0 || userId <= 0)
            throw new IllegalArgumentException("IDs invalides.");
        String sql = "DELETE FROM reaction WHERE message_id = ? AND user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────

    /**
     * Retourne le comptage des réactions par type pour un message.
     * Ex : { "👍" → 3, "❤️" → 1 }
     */
    public Map<String, Integer> countByMessage(int messageId) throws SQLException {
        String sql = """
                SELECT type, COUNT(*)::int AS cnt
                FROM reaction WHERE message_id = ?
                GROUP BY type ORDER BY cnt DESC
                """;
        Map<String, Integer> result = new LinkedHashMap<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("type"), rs.getInt("cnt"));
                }
            }
        }
        return result;
    }

    /** Retourne la réaction de l'utilisateur courant sur un message (null si aucune). */
    public String getUserReaction(int messageId, int userId) throws SQLException {
        String sql = "SELECT type FROM reaction WHERE message_id = ? AND user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("type") : null;
            }
        }
    }
}
