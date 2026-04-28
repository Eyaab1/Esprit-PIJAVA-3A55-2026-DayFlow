-- ═══════════════════════════════════════════════════════════════════════════
-- V14: Add missing priority column to routine table
-- ═══════════════════════════════════════════════════════════════════════════

-- Add priority column to routine table
ALTER TABLE routine ADD COLUMN IF NOT EXISTS priority VARCHAR(50);

-- Create index for priority column
CREATE INDEX IF NOT EXISTS idx_routine_priority ON routine(priority);

-- Add comment
COMMENT ON COLUMN routine.priority IS 'Routine priority: low, medium, or high';