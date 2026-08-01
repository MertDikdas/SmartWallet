ALTER TABLE account_transfers
    ADD COLUMN idempotency_key VARCHAR(100);

ALTER TABLE account_transfers
    ADD COLUMN request_fingerprint VARCHAR(64);

UPDATE account_transfers
SET idempotency_key = 'legacy-' || id,
    request_fingerprint =
        '0000000000000000000000000000000000000000000000000000000000000000'
WHERE idempotency_key IS NULL;

ALTER TABLE account_transfers
    ALTER COLUMN idempotency_key SET NOT NULL;

ALTER TABLE account_transfers
    ALTER COLUMN request_fingerprint SET NOT NULL;

ALTER TABLE account_transfers
    ADD CONSTRAINT uk_account_transfers_user_idempotency
        UNIQUE (user_id, idempotency_key);