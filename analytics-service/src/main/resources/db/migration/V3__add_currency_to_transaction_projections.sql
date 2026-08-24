ALTER TABLE transaction_projections
    ADD COLUMN currency VARCHAR(3);

UPDATE transaction_projections
SET currency = 'TRY'
WHERE currency IS NULL;

ALTER TABLE transaction_projections
    ALTER COLUMN currency SET NOT NULL;