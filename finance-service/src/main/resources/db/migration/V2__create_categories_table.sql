CREATE TABLE categories
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT                   NOT NULL,
    name       VARCHAR(100)             NOT NULL,
    type       VARCHAR(20)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_categories_type
        CHECK (type IN ('INCOME', 'EXPENSE')),

    CONSTRAINT uk_categories_user_name_type
        UNIQUE (user_id, name, type)
);

CREATE INDEX idx_categories_user_id
    ON categories (user_id);