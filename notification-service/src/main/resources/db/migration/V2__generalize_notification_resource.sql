ALTER TABLE notifications
    ADD COLUMN resource_type VARCHAR(50),
    ADD COLUMN resource_id BIGINT;

UPDATE notifications
SET resource_type = 'BUDGET',
    resource_id = budget_id
WHERE type = 'BUDGET_EXCEEDED';

ALTER TABLE notifications
DROP COLUMN budget_id,
    DROP COLUMN category_id;

CREATE INDEX idx_notifications_resource
    ON notifications (
                      resource_type,
                      resource_id
        );