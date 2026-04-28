package model.chatroom;

import java.time.LocalDateTime;
import java.util.List;

public class Reaction {

    /** Types autorisés */
    public static final List<String> VALID_TYPES =
            List.of("👍", "❤️", "😂", "😮", "😢", "🔥");

    private int id;
    private int messageId;
    private int userId;
    private String type;
    private LocalDateTime createdAt;

    public Reaction() { this.createdAt = LocalDateTime.now(); }

    public Reaction(int messageId, int userId, String type) {
        setMessageId(messageId);
        setUserId(userId);
        setType(type);
        this.createdAt = LocalDateTime.now();
    }

    // ── Validation ────────────────────────────────────────────────────────
    public void setMessageId(int messageId) {
        if (messageId <= 0) throw new IllegalArgumentException("message_id invalide.");
        this.messageId = messageId;
    }

    public void setUserId(int userId) {
        if (userId <= 0) throw new IllegalArgumentException("user_id invalide.");
        this.userId = userId;
    }

    public void setType(String type) {
        if (type == null || type.isBlank())
            throw new IllegalArgumentException("Type de réaction requis.");
        if (!VALID_TYPES.contains(type))
            throw new IllegalArgumentException("Type invalide. Autorisés : " + VALID_TYPES);
        this.type = type;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────
    public int getId()                    { return id; }
    public void setId(int id)             { this.id = id; }
    public int getMessageId()             { return messageId; }
    public int getUserId()                { return userId; }
    public String getType()               { return type; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
}
