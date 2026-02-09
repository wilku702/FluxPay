-- Add database-level constraints for financial correctness
ALTER TABLE accounts ADD CONSTRAINT chk_balance_non_negative CHECK (balance >= 0);
ALTER TABLE transactions ADD CONSTRAINT chk_amount_positive CHECK (amount > 0);

-- Remove redundant index (UNIQUE constraint on idempotency_key already creates an index)
DROP INDEX IF EXISTS idx_transactions_idempotency_key;
