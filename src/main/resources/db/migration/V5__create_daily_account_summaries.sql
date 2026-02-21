CREATE TABLE daily_account_summaries (
    id                BIGSERIAL PRIMARY KEY,
    account_id        BIGINT NOT NULL REFERENCES accounts(id),
    summary_date      DATE NOT NULL,
    total_credits     DECIMAL(19,4) NOT NULL DEFAULT 0,
    total_debits      DECIMAL(19,4) NOT NULL DEFAULT 0,
    transaction_count INTEGER NOT NULL DEFAULT 0,
    closing_balance   DECIMAL(19,4),
    UNIQUE (account_id, summary_date)
);
CREATE INDEX idx_daily_summaries_account_date ON daily_account_summaries(account_id, summary_date);
