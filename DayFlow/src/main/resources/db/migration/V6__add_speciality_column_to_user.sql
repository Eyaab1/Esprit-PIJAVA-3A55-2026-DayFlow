-- Add speciality column to user table if it doesn't exist
ALTER TABLE "user" 
ADD COLUMN IF NOT EXISTS speciality VARCHAR(255);

-- Add index for better query performance when filtering by speciality
CREATE INDEX IF NOT EXISTS idx_user_speciality ON "user"(speciality);
