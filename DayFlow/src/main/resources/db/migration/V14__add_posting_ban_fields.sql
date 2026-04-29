-- Add posting ban fields to user table
ALTER TABLE "user"
    ADD COLUMN IF NOT EXISTS posting_banned_until TIMESTAMP NULL;

ALTER TABLE "user"
    ADD COLUMN IF NOT EXISTS posting_ban_reason TEXT NULL;

-- Add ban type tracking to moderation_incident
ALTER TABLE moderation_incident
    ADD COLUMN IF NOT EXISTS ban_type VARCHAR(40) NULL;
