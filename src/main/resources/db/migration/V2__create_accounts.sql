CREATE TABLE accounts (
    id           BIGSERIAL      PRIMARY KEY,
    user_id      BIGINT         NOT NULL REFERENCES users(id),
    account_name VARCHAR(50)    NOT NULL,
    balance      DECIMAL(19, 4) NOT NULL DEFAULT 0,
    currency     VARCHAR(3)     NOT NULL DEFAULT 'USD',
    status       VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    version      INTEGER        NOT NULL DEFAULT 0,
    created_at   TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
