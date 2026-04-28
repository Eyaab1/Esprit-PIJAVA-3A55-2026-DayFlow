-- V11: Add Deadline Management and Notification System

-- ============================================================================
-- 1. Add deadline columns to existing tables
-- ============================================================================

-- Add deadline to goal table
ALTER TABLE goal ADD COLUMN IF NOT EXISTS deadline TIMESTAMP;
ALTER TABLE goal ADD COLUMN IF NOT EXISTS is_overdue BOOLEAN NOT NULL DEFAULT FALSE;

-- Add deadline to routine table
ALTER TABLE routine ADD COLUMN IF NOT EXISTS deadline TIMESTAMP;
ALTER TABLE routine ADD COLUMN IF NOT EXISTS is_overdue BOOLEAN NOT NULL DEFAULT FALSE;

-- Add deadline to activity table
ALTER TABLE activity ADD COLUMN IF NOT EXISTS deadline TIMESTAMP;
ALTER TABLE activity ADD COLUMN IF NOT EXISTS is_overdue BOOLEAN NOT NULL DEFAULT FALSE;

-- ============================================================================
-- 2. Create notification table
-- ============================================================================

CREATE TABLE IF NOT EXISTS notification (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    action_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

-- Create indexes for notification table
CREATE INDEX IF NOT EXISTS idx_notification_user ON notification(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_is_read ON notification(is_read);
CREATE INDEX IF NOT EXISTS idx_notification_created_at ON notification(created_at);
CREATE INDEX IF NOT EXISTS idx_notification_entity ON notification(entity_type, entity_id);

-- ============================================================================
-- 3. Create reminder tracking table
-- ============================================================================

CREATE TABLE IF NOT EXISTS reminder_log (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id INTEGER NOT NULL,
    reminder_type VARCHAR(50) NOT NULL,
    deadline TIMESTAMP NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reminder_log_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    CONSTRAINT uq_reminder_log UNIQUE (user_id, entity_type, entity_id, reminder_type, deadline)
);

-- Create indexes for reminder_log table
CREATE INDEX IF NOT EXISTS idx_reminder_log_user ON reminder_log(user_id);
CREATE INDEX IF NOT EXISTS idx_reminder_log_entity ON reminder_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_reminder_log_sent_at ON reminder_log(sent_at);

-- ============================================================================
-- 4. Add constraints for deadline validation
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_goal_deadline_after_start') THEN
        ALTER TABLE goal ADD CONSTRAINT chk_goal_deadline_after_start 
            CHECK (deadline IS NULL OR deadline > start_date);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_routine_deadline_after_start') THEN
        ALTER TABLE routine ADD CONSTRAINT chk_routine_deadline_after_start 
            CHECK (deadline IS NULL OR deadline > start_date);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_activity_deadline_after_scheduled') THEN
        ALTER TABLE activity ADD CONSTRAINT chk_activity_deadline_after_scheduled 
            CHECK (deadline IS NULL OR deadline > scheduled_date);
    END IF;
END $$;

-- ============================================================================
-- 5. Create notification type enum-like check
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_notification_type') THEN
        ALTER TABLE notification ADD CONSTRAINT chk_notification_type 
            CHECK (type IN ('DEADLINE_24H', 'DEADLINE_1H', 'DEADLINE_REACHED', 'DEADLINE_MISSED', 'STATUS_CHANGED'));
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_notification_entity_type') THEN
        ALTER TABLE notification ADD CONSTRAINT chk_notification_entity_type 
            CHECK (entity_type IN ('goal', 'routine', 'activity'));
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_reminder_type') THEN
        ALTER TABLE reminder_log ADD CONSTRAINT chk_reminder_type 
            CHECK (reminder_type IN ('24H', '1H', 'REACHED', 'MISSED'));
    END IF;
END $$;

-- ============================================================================
-- 6. Create view for unread notification count
-- ============================================================================

CREATE OR REPLACE VIEW v_unread_notification_count AS
SELECT 
    user_id,
    COUNT(*) as unread_count
FROM notification
WHERE is_read = FALSE
GROUP BY user_id;

-- ============================================================================
-- 7. Create view for overdue entities
-- ============================================================================

CREATE OR REPLACE VIEW v_overdue_goals AS
SELECT 
    g.id,
    g.title,
    g.deadline,
    g.status,
    g.user_id,
    CURRENT_TIMESTAMP - g.deadline as overdue_duration
FROM goal g
WHERE g.deadline IS NOT NULL
    AND g.deadline < CURRENT_TIMESTAMP
    AND g.status NOT IN ('completed', 'archived')
    AND g.is_overdue = FALSE;

CREATE OR REPLACE VIEW v_overdue_routines AS
SELECT 
    r.id,
    r.title,
    r.deadline,
    r.status,
    r.goal_id,
    CURRENT_TIMESTAMP - r.deadline as overdue_duration
FROM routine r
WHERE r.deadline IS NOT NULL
    AND r.deadline < CURRENT_TIMESTAMP
    AND r.status NOT IN ('completed', 'cancelled')
    AND r.is_overdue = FALSE;

CREATE OR REPLACE VIEW v_overdue_activities AS
SELECT 
    a.id,
    a.title,
    a.deadline,
    a.status,
    a.routine_id,
    CURRENT_TIMESTAMP - a.deadline as overdue_duration
FROM activity a
WHERE a.deadline IS NOT NULL
    AND a.deadline < CURRENT_TIMESTAMP
    AND a.status NOT IN ('completed', 'skipped', 'cancelled')
    AND a.is_overdue = FALSE;

-- ============================================================================
-- 8. Create view for upcoming deadlines (next 7 days)
-- ============================================================================

CREATE OR REPLACE VIEW v_upcoming_deadlines AS
SELECT 
    'goal' as entity_type,
    g.id as entity_id,
    g.title,
    g.deadline,
    g.user_id,
    EXTRACT(EPOCH FROM (g.deadline - CURRENT_TIMESTAMP)) / 3600 as hours_until_deadline
FROM goal g
WHERE g.deadline IS NOT NULL
    AND g.deadline > CURRENT_TIMESTAMP
    AND g.deadline <= CURRENT_TIMESTAMP + INTERVAL '7 days'
    AND g.status IN ('active', 'draft')
UNION ALL
SELECT 
    'routine' as entity_type,
    r.id as entity_id,
    r.title,
    r.deadline,
    gp.user_id,
    EXTRACT(EPOCH FROM (r.deadline - CURRENT_TIMESTAMP)) / 3600 as hours_until_deadline
FROM routine r
JOIN goal g ON r.goal_id = g.id
JOIN goal_participation gp ON g.id = gp.goal_id
WHERE r.deadline IS NOT NULL
    AND r.deadline > CURRENT_TIMESTAMP
    AND r.deadline <= CURRENT_TIMESTAMP + INTERVAL '7 days'
    AND r.status IN ('active', 'paused')
UNION ALL
SELECT 
    'activity' as entity_type,
    a.id as entity_id,
    a.title,
    a.deadline,
    gp.user_id,
    EXTRACT(EPOCH FROM (a.deadline - CURRENT_TIMESTAMP)) / 3600 as hours_until_deadline
FROM activity a
JOIN routine r ON a.routine_id = r.id
JOIN goal g ON r.goal_id = g.id
JOIN goal_participation gp ON g.id = gp.goal_id
WHERE a.deadline IS NOT NULL
    AND a.deadline > CURRENT_TIMESTAMP
    AND a.deadline <= CURRENT_TIMESTAMP + INTERVAL '7 days'
    AND a.status IN ('pending', 'in_progress');

-- ============================================================================
-- 9. Create function to mark overdue entities
-- ============================================================================

CREATE OR REPLACE FUNCTION mark_overdue_entities()
RETURNS TABLE(entity_type VARCHAR, entity_id INTEGER, marked_at TIMESTAMP) AS $$
BEGIN
    -- Mark overdue goals
    UPDATE goal SET is_overdue = TRUE, status = 'failed'
    WHERE deadline IS NOT NULL
        AND deadline < CURRENT_TIMESTAMP
        AND status NOT IN ('completed', 'archived', 'failed')
        AND is_overdue = FALSE
    RETURNING 'goal'::VARCHAR, id, CURRENT_TIMESTAMP;
    
    -- Mark overdue routines
    UPDATE routine SET is_overdue = TRUE, status = 'cancelled'
    WHERE deadline IS NOT NULL
        AND deadline < CURRENT_TIMESTAMP
        AND status NOT IN ('completed', 'cancelled')
        AND is_overdue = FALSE
    RETURNING 'routine'::VARCHAR, id, CURRENT_TIMESTAMP;
    
    -- Mark overdue activities
    UPDATE activity SET is_overdue = TRUE, status = 'cancelled'
    WHERE deadline IS NOT NULL
        AND deadline < CURRENT_TIMESTAMP
        AND status NOT IN ('completed', 'skipped', 'cancelled')
        AND is_overdue = FALSE
    RETURNING 'activity'::VARCHAR, id, CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 10. Create function to clean old notifications
-- ============================================================================

CREATE OR REPLACE FUNCTION clean_old_notifications(days_to_keep INTEGER DEFAULT 30)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM notification
    WHERE is_read = TRUE
        AND created_at < CURRENT_TIMESTAMP - (days_to_keep || ' days')::INTERVAL;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 11. Create indexes for performance
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_goal_deadline ON goal(deadline) WHERE deadline IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_goal_is_overdue ON goal(is_overdue) WHERE is_overdue = TRUE;
CREATE INDEX IF NOT EXISTS idx_routine_deadline ON routine(deadline) WHERE deadline IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_routine_is_overdue ON routine(is_overdue) WHERE is_overdue = TRUE;
CREATE INDEX IF NOT EXISTS idx_activity_deadline ON activity(deadline) WHERE deadline IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_activity_is_overdue ON activity(is_overdue) WHERE is_overdue = TRUE;

-- ============================================================================
-- 12. Add comments for documentation
-- ============================================================================

COMMENT ON TABLE notification IS 'Stores user notifications for deadline events and status changes';
COMMENT ON TABLE reminder_log IS 'Tracks sent reminders to prevent duplicates';
COMMENT ON COLUMN notification.type IS 'Type of notification: DEADLINE_24H, DEADLINE_1H, DEADLINE_REACHED, DEADLINE_MISSED, STATUS_CHANGED';
COMMENT ON COLUMN notification.entity_type IS 'Type of entity: goal, routine, activity';
COMMENT ON COLUMN reminder_log.reminder_type IS 'Type of reminder: 24H, 1H, REACHED, MISSED';
