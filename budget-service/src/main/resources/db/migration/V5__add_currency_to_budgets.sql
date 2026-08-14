ALTER TABLE budgets
    ADD COLUMN currency VARCHAR(3);

UPDATE budgets
SET currency = 'TRY'
WHERE currency IS NULL;

ALTER TABLE budgets
    ALTER COLUMN currency SET NOT NULL;


ALTER TABLE monthly_category_spending
    ADD COLUMN currency VARCHAR(3);

UPDATE monthly_category_spending
SET currency = 'TRY'
WHERE currency IS NULL;

ALTER TABLE monthly_category_spending
    ALTER COLUMN currency SET NOT NULL;

ALTER TABLE monthly_category_spending
DROP CONSTRAINT uk_monthly_category_spending_user_category_period;

ALTER TABLE monthly_category_spending
    ADD CONSTRAINT uk_monthly_category_spending_user_category_period_currency
        UNIQUE (user_id, category_id, year, month, currency);

ALTER TABLE budgets
DROP CONSTRAINT uk_budgets_user_category_period;

ALTER TABLE budgets
    ADD CONSTRAINT uk_budgets_user_category_period_currency
        UNIQUE (user_id, category_id, year, month, currency);