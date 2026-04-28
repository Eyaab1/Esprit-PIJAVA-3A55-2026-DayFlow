ALTER TABLE "user"
    ADD COLUMN IF NOT EXISTS banned_until TIMESTAMP NULL;

ALTER TABLE "user"
    ADD COLUMN IF NOT EXISTS ban_reason TEXT NULL;

CREATE TABLE IF NOT EXISTS moderation_incident (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id INTEGER NULL REFERENCES "user"(id) ON DELETE SET NULL,
    user_email VARCHAR(255) NULL,
    entity_type VARCHAR(80) NOT NULL,
    content_text TEXT NULL,
    content_preview TEXT NULL,
    detected_reason TEXT NULL,
    flagged_attributes TEXT NULL,
    highest_attribute VARCHAR(120) NULL,
    highest_score DOUBLE PRECISION NULL,
    threshold_used DOUBLE PRECISION NULL,
    source VARCHAR(120) NULL,
    warning_status VARCHAR(40) NOT NULL DEFAULT 'WARNED',
    incident_status VARCHAR(40) NOT NULL DEFAULT 'NEW',
    action_taken VARCHAR(40) NULL,
    action_reason TEXT NULL,
    ban_days INTEGER NULL,
    action_by_admin_id INTEGER NULL REFERENCES "user"(id) ON DELETE SET NULL,
    action_at TIMESTAMP NULL,
    account_status_snapshot VARCHAR(80) NULL
);

CREATE INDEX IF NOT EXISTS idx_moderation_incident_created_at ON moderation_incident(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_moderation_incident_user_id ON moderation_incident(user_id);
