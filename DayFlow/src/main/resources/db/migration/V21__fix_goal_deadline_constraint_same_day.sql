DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_goal_deadline_after_start'
    ) THEN
        ALTER TABLE goal DROP CONSTRAINT chk_goal_deadline_after_start;
    END IF;
END $$;

ALTER TABLE goal
    ADD CONSTRAINT chk_goal_deadline_after_start
    CHECK (deadline IS NULL OR start_date IS NULL OR deadline::date >= start_date);
