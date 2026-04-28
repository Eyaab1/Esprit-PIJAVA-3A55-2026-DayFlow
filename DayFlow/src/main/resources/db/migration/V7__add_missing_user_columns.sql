-- Add missing user table columns if they don't exist
ALTER TABLE "user" 
ADD COLUMN IF NOT EXISTS profile_picture_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS profile_picture_size INTEGER,
ADD COLUMN IF NOT EXISTS specialities TEXT,
ADD COLUMN IF NOT EXISTS availability VARCHAR(255),
ADD COLUMN IF NOT EXISTS rating DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS price_per_session DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS bio TEXT,
ADD COLUMN IF NOT EXISTS photo_url VARCHAR(500),
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Add indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_user_profile_picture ON "user"(profile_picture_name) WHERE profile_picture_name IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_user_rating ON "user"(rating) WHERE rating IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_user_availability ON "user"(availability) WHERE availability IS NOT NULL;
