-- ═══════════════════════════════════════════════════════════════════════════
-- V17: Fix routine table schema to match Routine model
-- ═══════════════════════════════════════════════════════════════════════════

-- Remove NOT NULL constraint from frequency column (make it optional)
ALTER TABLE routine ALTER COLUMN frequency DROP NOT NULL;

-- Remove NOT NULL constraint from start_date column (make it optional)  
ALTER TABLE routine ALTER COLUMN start_date DROP NOT NULL;

-- Add deadline column to match Routine model
ALTER TABLE routine ADD COLUMN IF NOT EXISTS deadline TIMESTAMP;

-- Create index for deadline column
CREATE INDEX IF NOT EXISTS idx_routine_deadline ON routine(deadline);

-- Add comment
COMMENT ON COLUMN routine.deadline IS 'Routine deadline timestamp';