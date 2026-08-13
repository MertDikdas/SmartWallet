ALTER TABLE recurring_transaction_executions
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_retry_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE recurring_transaction_executions
    ADD CONSTRAINT chk_recurring_execution_attempt_count
        CHECK (attempt_count >= 0);