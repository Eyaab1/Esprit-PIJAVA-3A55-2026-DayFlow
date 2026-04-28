-- Migration: Add post_id to reclamation table
-- This allows reclamations to reference specific posts for moderation

-- Add post_id column (nullable, since not all reclamations are about posts)
ALTER TABLE reclamation 
ADD COLUMN IF NOT EXISTS post_id INTEGER;

-- Add foreign key constraint to post table
ALTER TABLE reclamation 
ADD CONSTRAINT fk_reclamation_post 
FOREIGN KEY (post_id) REFERENCES post(id) 
ON DELETE SET NULL;

-- Add index for better query performance
CREATE INDEX IF NOT EXISTS idx_reclamation_post_id ON reclamation(post_id);

-- Comment for documentation
COMMENT ON COLUMN reclamation.post_id IS 'Reference to the post being reported (nullable - only for post-related reclamations)';
