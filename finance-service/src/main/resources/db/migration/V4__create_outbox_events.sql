CREATE TABLE outbox_events
(
    id              UUID PRIMARY KEY,
    aggregate_type  VARCHAR(50)              NOT NULL,
    aggregate_id    BIGINT                   NOT NULL,
    event_type      VARCHAR(50)              NOT NULL,
    routing_key     VARCHAR(100)             NOT NULL,
    payload         TEXT                     NOT NULL,
    status          VARCHAR(20)              NOT NULL DEFAULT 'PENDING',
    attempt_count   INTEGER                  NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at    TIMESTAMP WITH TIME ZONE,
    last_error      TEXT,

    CONSTRAINT chk_outbox_event_status
        CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),

    CONSTRAINT chk_outbox_attempt_count
        CHECK (attempt_count >= 0)
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events (status, next_attempt_at, created_at);