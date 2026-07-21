CREATE TABLE transactions
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT                   NOT NULL,
    account_id       BIGINT                   NOT NULL,
    category_id      BIGINT                   NOT NULL,
    type             VARCHAR(20)              NOT NULL,
    amount           NUMERIC(19, 2)           NOT NULL,
    description      VARCHAR(255),
    transaction_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_id)
            REFERENCES accounts (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_transactions_category
        FOREIGN KEY (category_id)
            REFERENCES categories (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_transactions_type
        CHECK (type IN ('INCOME', 'EXPENSE')),

    CONSTRAINT chk_transactions_amount
        CHECK (amount > 0)
);

CREATE INDEX idx_transactions_user_id
    ON transactions (user_id);

CREATE INDEX idx_transactions_account_id
    ON transactions (account_id);

CREATE INDEX idx_transactions_category_id
    ON transactions (category_id);

CREATE INDEX idx_transactions_date
    ON transactions (transaction_date);