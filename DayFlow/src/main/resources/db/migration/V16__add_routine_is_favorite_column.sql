-- ═══════════════════════════════════════════════════════════════════════════
-- V16: Add is_favorite column to routine table
-- ═══════════════════════════════════════════════════════════════════════════

-- Add is_favorite column to routine table
ALTER TABLE routine ADD COLUMN IF NOT EXISTS is_favorite BOOLEAN NOT NULL DEFAULT FALSE;

-- Create index for is_favorite column
CREATE INDEX IF NOT EXISTS idx_routine_is_favorite ON routine(is_favorite) WHERE is_favorite = TRUE;

-- Add comment
COMMENT ON COLUMN routine.is_favorite IS 'Whether the routine is marked as favorite';