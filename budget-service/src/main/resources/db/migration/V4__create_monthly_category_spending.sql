CREATE TABLE monthly_category_spending
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT                   NOT NULL,
    category_id  BIGINT                   NOT NULL,
    spent_amount NUMERIC(19, 2)           NOT NULL DEFAULT 0,
    year         INTEGER                  NOT NULL,
    month        INTEGER                  NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_monthly_category_spending_user_category_period
        UNIQUE (user_id, category_id, year, month),

    CONSTRAINT chk_monthly_category_spending_spent_amount
        CHECK (spent_amount >= 0),

    CONSTRAINT chk_monthly_category_spending_month
        CHECK (month BETWEEN 1 AND 12),

    CONSTRAINT chk_monthly_category_spending_year
        CHECK (year >= 2000)
);

CREATE INDEX idx_monthly_category_spending_user_id
    ON monthly_category_spending (user_id);

CREATE INDEX idx_monthly_category_spending_period
    ON monthly_category_spending (year, month);

CREATE INDEX idx_monthly_category_spending_category_id
    ON monthly_category_spending (category_id);