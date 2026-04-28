package services.post.moderation;

import java.sql.SQLException;

public class ModerationRejectedException extends SQLException {

    private final ModerationResult moderationResult;

    public ModerationRejectedException(String message, ModerationResult moderationResult) {
        super(message);
        this.moderationResult = moderationResult;
    }

    public ModerationResult getModerationResult() {
        return moderationResult;
    }
}
