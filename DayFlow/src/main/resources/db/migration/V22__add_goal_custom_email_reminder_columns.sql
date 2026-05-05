ALTER TABLE goal
    ADD COLUMN IF NOT EXISTS email_reminder_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE goal
    ADD COLUMN IF NOT EXISTS email_reminder_at TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_goal_email_reminder_consistency'
    ) THEN
        ALTER TABLE goal
            ADD CONSTRAINT chk_goal_email_reminder_consistency
            CHECK (
                (email_reminder_enabled = FALSE AND email_reminder_at IS NULL)
                OR
                (email_reminder_enabled = TRUE AND email_reminder_at IS NOT NULL)
            );
    END IF;
END $$;
