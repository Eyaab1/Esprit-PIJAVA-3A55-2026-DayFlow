CREATE TABLE IF NOT EXISTS profile_analysis (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    score INTEGER NOT NULL CHECK (score >= 0 AND score <= 100),
    recommendations_json JSONB NOT NULL,
    analyzed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_profile_analysis_user_date
    ON profile_analysis(user_id, analyzed_at DESC);
