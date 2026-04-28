CREATE TABLE IF NOT EXISTS user_ai_profile (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES "user"(id) ON DELETE CASCADE,
    answers_json JSONB NOT NULL,
    profile_json JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_ai_profile_user
    ON user_ai_profile(user_id);
