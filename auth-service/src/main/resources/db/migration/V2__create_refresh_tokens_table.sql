CREATE TABLE refresh_tokens
(
    id         BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64)              NOT NULL,
    user_id    BIGINT                   NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);

CREATE INDEX idx_refresh_tokens_expires_at
    ON refresh_tokens (expires_at);