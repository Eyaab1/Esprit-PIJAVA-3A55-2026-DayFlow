-- ═══════════════════════════════════════════════════════════════════════════
-- V13: Add missing visibility column to routine table
-- ═══════════════════════════════════════════════════════════════════════════

-- Add visibility column to routine table
ALTER TABLE routine ADD COLUMN IF NOT EXISTS visibility VARCHAR(50) NOT NULL DEFAULT 'private';

-- Create index for visibility column
CREATE INDEX IF NOT EXISTS idx_routine_visibility ON routine(visibility);

-- Update existing routines to have default visibility
UPDATE routine SET visibility = 'private' WHERE visibility IS NULL;

-- Add comment
COMMENT ON COLUMN routine.visibility IS 'Routine visibility: public or private';