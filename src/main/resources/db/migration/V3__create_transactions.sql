CREATE TABLE transactions (
    id              BIGSERIAL      PRIMARY KEY,
    account_id      BIGINT         NOT NULL REFERENCES accounts(id),
    type            VARCHAR(10)    NOT NULL,
    amount          DECIMAL(19, 4) NOT NULL,
    description     VARCHAR(255),
    correlation_id  UUID,
    idempotency_key VARCHAR(64)    NOT NULL UNIQUE,
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    balance_after   DECIMAL(19, 4) NOT NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_correlation_id ON transactions(correlation_id);
CREATE INDEX idx_transactions_idempotency_key ON transactions(idempotency_key);
