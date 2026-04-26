CREATE TABLE IF NOT EXISTS profile_analysis (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    score INTEGER NOT NULL CHECK (score >= 0 AND score <= 100),
    recommendations_json JSONB NOT NULL,
    analyzed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_profile_analysis_user_date
    ON profile_analysis(user_id, analyzed_at DESC);

CREATE TABLE IF NOT EXISTS password_reset_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_password_reset_token_hash
    ON password_reset_token(token_hash);

CREATE TABLE IF NOT EXISTS login_attempt (
    id BIGSERIAL PRIMARY KEY,
    identifier VARCHAR(190) NOT NULL,
    success BOOLEAN NOT NULL,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_login_attempt_identifier_time
    ON login_attempt(identifier, attempted_at DESC);
