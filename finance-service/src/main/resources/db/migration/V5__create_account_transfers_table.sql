CREATE TABLE account_transfers
(
    id              BIGSERIAL PRIMARY KEY,

    user_id         BIGINT                   NOT NULL,

    from_account_id BIGINT                   NOT NULL,

    to_account_id   BIGINT                   NOT NULL,

    amount          NUMERIC(19, 2)           NOT NULL,

    currency        VARCHAR(3)               NOT NULL,

    description     VARCHAR(255),

    transferred_at  TIMESTAMP WITH TIME ZONE NOT NULL,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_account_transfers_from_account
        FOREIGN KEY (from_account_id)
            REFERENCES accounts (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_account_transfers_to_account
        FOREIGN KEY (to_account_id)
            REFERENCES accounts (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_account_transfers_different_accounts
        CHECK (from_account_id <> to_account_id),

    CONSTRAINT chk_account_transfers_positive_amount
        CHECK (amount > 0),

    CONSTRAINT chk_account_transfers_currency
        CHECK (
            currency IN (
                         'TRY',
                         'USD',
                         'EUR'
                )
            )
);

CREATE INDEX idx_account_transfers_user_date
    ON account_transfers (user_id, transferred_at DESC);

CREATE INDEX idx_account_transfers_from_account
    ON account_transfers (from_account_id);

CREATE INDEX idx_account_transfers_to_account
    ON account_transfers (to_account_id);