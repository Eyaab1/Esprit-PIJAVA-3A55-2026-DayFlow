DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_activity_reminder_consistency'
    ) THEN
        ALTER TABLE activity
            ADD CONSTRAINT chk_activity_reminder_consistency
            CHECK (has_reminder = FALSE OR reminder_at IS NOT NULL) NOT VALID;
    END IF;
END $$;

-- Normalize legacy inconsistent data before validation.
UPDATE activity
SET has_reminder = FALSE
WHERE has_reminder = TRUE
  AND reminder_at IS NULL;

ALTER TABLE activity VALIDATE CONSTRAINT chk_activity_reminder_consistency;
