CREATE TABLE IF NOT EXISTS oauth_account (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(40) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_oauth_provider_user
    ON oauth_account(provider, provider_user_id);
