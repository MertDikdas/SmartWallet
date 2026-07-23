CREATE TABLE transaction_projections
(
    transaction_id   BIGINT PRIMARY KEY,
    user_id          BIGINT                   NOT NULL,
    account_id       BIGINT                   NOT NULL,
    category_id      BIGINT                   NOT NULL,
    transaction_type VARCHAR(20)              NOT NULL,
    amount           NUMERIC(19, 2)           NOT NULL,
    transaction_date TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_projection_transaction_type
        CHECK (transaction_type IN ('INCOME', 'EXPENSE')),

    CONSTRAINT chk_projection_amount
        CHECK (amount > 0)
);

CREATE INDEX idx_projection_user_date
    ON transaction_projections (user_id, transaction_date);

CREATE INDEX idx_projection_user_category
    ON transaction_projections (user_id, category_id);

CREATE TABLE processed_transaction_events
(
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(20)              NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);