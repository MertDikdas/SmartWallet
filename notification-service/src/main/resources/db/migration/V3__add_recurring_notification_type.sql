ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS chk_notifications_type;

ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_type
        CHECK (
            type IN (
                'BUDGET_EXCEEDED',
                'RECURRING_TRANSACTION_FAILED'
            )
        );