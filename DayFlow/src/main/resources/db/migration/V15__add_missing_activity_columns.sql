-- ═══════════════════════════════════════════════════════════════════════════
-- V15: Add missing columns to activity table
-- ═══════════════════════════════════════════════════════════════════════════

-- Add missing columns to activity table
ALTER TABLE activity ADD COLUMN IF NOT EXISTS start_time TIMESTAMP;
ALTER TABLE activity ADD COLUMN IF NOT EXISTS duration TIME;
ALTER TABLE activity ADD COLUMN IF NOT EXISTS priority VARCHAR(50);
ALTER TABLE activity ADD COLUMN IF NOT EXISTS has_reminder BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE activity ADD COLUMN IF NOT EXISTS reminder_at TIMESTAMP;
ALTER TABLE activity ADD COLUMN IF NOT EXISTS deadline TIMESTAMP;
ALTER TABLE activity ADD COLUMN IF NOT EXISTS is_favorite BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE activity ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;
ALTER TABLE activity ADD COLUMN IF NOT EXISTS actual_duration_minutes INTEGER;
ALTER TABLE activity ADD COLUMN IF NOT EXISTS planned_duration_minutes INTEGER;

-- Create indexes for new columns
CREATE INDEX IF NOT EXISTS idx_activity_start_time ON activity(start_time);
CREATE INDEX IF NOT EXISTS idx_activity_priority ON activity(priority);
CREATE INDEX IF NOT EXISTS idx_activity_deadline ON activity(deadline);
CREATE INDEX IF NOT EXISTS idx_activity_is_favorite ON activity(is_favorite) WHERE is_favorite = TRUE;
CREATE INDEX IF NOT EXISTS idx_activity_has_reminder ON activity(has_reminder) WHERE has_reminder = TRUE;
CREATE INDEX IF NOT EXISTS idx_activity_completed_at ON activity(completed_at);

-- Add comments
COMMENT ON COLUMN activity.start_time IS 'Scheduled start time for the activity';
COMMENT ON COLUMN activity.duration IS 'Expected duration of the activity';
COMMENT ON COLUMN activity.priority IS 'Activity priority: low, medium, or high';
COMMENT ON COLUMN activity.has_reminder IS 'Whether the activity has a reminder set';
COMMENT ON COLUMN activity.reminder_at IS 'When to send the reminder';
COMMENT ON COLUMN activity.deadline IS 'Activity deadline';
COMMENT ON COLUMN activity.is_favorite IS 'Whether the activity is marked as favorite';
COMMENT ON COLUMN activity.completed_at IS 'When the activity was completed';
COMMENT ON COLUMN activity.actual_duration_minutes IS 'Actual time spent on activity in minutes';
COMMENT ON COLUMN activity.planned_duration_minutes IS 'Planned time for activity in minutes';