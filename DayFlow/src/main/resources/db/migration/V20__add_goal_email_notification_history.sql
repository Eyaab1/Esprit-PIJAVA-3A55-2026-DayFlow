CREATE TABLE IF NOT EXISTS goal_email_notification_history (
    id SERIAL PRIMARY KEY,
    goal_id INTEGER NOT NULL REFERENCES goal(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    reminder_type VARCHAR(50) NOT NULL,
    deadline_snapshot TIMESTAMP NOT NULL,
    last_reminder_sent TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_goal_email_notification_history
        UNIQUE (goal_id, user_id, reminder_type, deadline_snapshot)
);

CREATE INDEX IF NOT EXISTS idx_goal_email_history_goal_id
    ON goal_email_notification_history(goal_id);
CREATE INDEX IF NOT EXISTS idx_goal_email_history_user_id
    ON goal_email_notification_history(user_id);
CREATE INDEX IF NOT EXISTS idx_goal_email_history_reminder_type
    ON goal_email_notification_history(reminder_type);
CREATE INDEX IF NOT EXISTS idx_goal_email_history_email_sent
    ON goal_email_notification_history(email_sent);
