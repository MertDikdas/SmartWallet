CREATE TABLE notifications
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT                   NOT NULL,
    type            VARCHAR(50)              NOT NULL,
    title           VARCHAR(150)             NOT NULL,
    message         VARCHAR(500)             NOT NULL,
    budget_id       BIGINT,
    category_id     BIGINT,
    source_event_id UUID                     NOT NULL,
    is_read         BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at         TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uk_notifications_source_event
        UNIQUE (source_event_id),

    CONSTRAINT chk_notifications_type
        CHECK (type IN ('BUDGET_EXCEEDED'))
);

CREATE INDEX idx_notifications_user_created
    ON notifications (user_id, created_at DESC);

CREATE INDEX idx_notifications_user_unread
    ON notifications (user_id, is_read);