CREATE TABLE processed_transaction_events
(
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(20)              NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_processed_transaction_events_processed_at
    ON processed_transaction_events (processed_at);