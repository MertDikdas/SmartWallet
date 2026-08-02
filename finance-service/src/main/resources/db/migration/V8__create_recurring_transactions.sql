CREATE TABLE recurring_transactions (
                                        id BIGSERIAL PRIMARY KEY,

                                        user_id BIGINT NOT NULL,

                                        account_id BIGINT NOT NULL,
                                        category_id BIGINT NOT NULL,

                                        type VARCHAR(20) NOT NULL,

                                        amount NUMERIC(19, 2) NOT NULL,

                                        description VARCHAR(255),

                                        frequency VARCHAR(20) NOT NULL,
                                        status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                        start_date DATE NOT NULL,
                                        end_date DATE,

                                        next_execution_date DATE NOT NULL,
                                        last_execution_date DATE,

                                        created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                        updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                        CONSTRAINT fk_recurring_transactions_account
                                            FOREIGN KEY (account_id)
                                                REFERENCES accounts (id)
                                                ON DELETE RESTRICT,

                                        CONSTRAINT fk_recurring_transactions_category
                                            FOREIGN KEY (category_id)
                                                REFERENCES categories (id)
                                                ON DELETE RESTRICT,

                                        CONSTRAINT chk_recurring_transactions_type
                                            CHECK (type IN ('INCOME', 'EXPENSE')),

                                        CONSTRAINT chk_recurring_transactions_amount
                                            CHECK (amount > 0),

                                        CONSTRAINT chk_recurring_transactions_frequency
                                            CHECK (frequency IN ('WEEKLY', 'MONTHLY')),

                                        CONSTRAINT chk_recurring_transactions_status
                                            CHECK (status IN ('ACTIVE', 'PAUSED', 'CANCELLED')),

                                        CONSTRAINT chk_recurring_transactions_dates
                                            CHECK (
                                                end_date IS NULL
                                                    OR end_date >= start_date
                                                )
);

CREATE INDEX idx_recurring_transactions_due
    ON recurring_transactions (
                               status,
                               next_execution_date
        );

CREATE INDEX idx_recurring_transactions_user
    ON recurring_transactions (user_id);

CREATE INDEX idx_recurring_transactions_account
    ON recurring_transactions (account_id);