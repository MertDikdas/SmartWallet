CREATE TABLE budgets
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT                   NOT NULL,
    category_id  BIGINT                   NOT NULL,
    limit_amount NUMERIC(19, 2)           NOT NULL,
    spent_amount NUMERIC(19, 2)           NOT NULL DEFAULT 0,
    year         INTEGER                  NOT NULL,
    month        INTEGER                  NOT NULL,
    status       VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_budgets_user_category_period
        UNIQUE (user_id, category_id, year, month),

    CONSTRAINT chk_budgets_limit_amount
        CHECK (limit_amount > 0),

    CONSTRAINT chk_budgets_spent_amount
        CHECK (spent_amount >= 0),

    CONSTRAINT chk_budgets_month
        CHECK (month BETWEEN 1 AND 12),

    CONSTRAINT chk_budgets_year
        CHECK (year >= 2000),

    CONSTRAINT chk_budgets_status
        CHECK (status IN ('ACTIVE', 'EXCEEDED'))
);

CREATE INDEX idx_budgets_user_id
    ON budgets (user_id);

CREATE INDEX idx_budgets_period
    ON budgets (year, month);

CREATE INDEX idx_budgets_category_id
    ON budgets (category_id);