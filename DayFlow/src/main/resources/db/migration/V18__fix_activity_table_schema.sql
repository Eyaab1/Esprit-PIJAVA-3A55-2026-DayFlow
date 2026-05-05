-- ═══════════════════════════════════════════════════════════════════════════
-- V18: Fix activity table schema to match Activity model
-- ═══════════════════════════════════════════════════════════════════════════

-- Remove NOT NULL constraint from scheduled_date column (make it optional)
-- The Activity model uses start_time instead of scheduled_date
ALTER TABLE activity ALTER COLUMN scheduled_date DROP NOT NULL;

-- Add comment to clarify the difference
COMMENT ON COLUMN activity.scheduled_date IS 'Legacy scheduled date (use start_time for new activities)';
COMMENT ON COLUMN activity.start_time IS 'Activity start time (preferred over scheduled_date)';