CREATE TABLE accounts
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT                   NOT NULL,
    name       VARCHAR(100)             NOT NULL,
    type       VARCHAR(30)              NOT NULL,
    balance    NUMERIC(19, 2)           NOT NULL DEFAULT 0,
    currency   VARCHAR(3)               NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_accounts_type
        CHECK (
            type IN (
                     'CHECKING',
                     'SAVINGS',
                     'CASH',
                     'CREDIT_CARD'
                )
            ),

    CONSTRAINT chk_accounts_currency
        CHECK (
            currency IN (
                         'TRY',
                         'USD',
                         'EUR'
                )
            )
);

CREATE INDEX idx_accounts_user_id
    ON accounts (user_id);