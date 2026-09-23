
CREATE TABLE IF NOT EXISTS accounts (
    account_number   VARCHAR(20)    PRIMARY KEY,
    pin              VARCHAR(4)     NOT NULL,
    name             VARCHAR(100)   NOT NULL,
    account_type     VARCHAR(10)    NOT NULL CHECK (account_type IN ('SAVINGS', 'CHECKING')),
    balance          NUMERIC(14,2)  NOT NULL DEFAULT 0,
    locked           BOOLEAN        NOT NULL DEFAULT FALSE,
    failed_attempts  INTEGER        NOT NULL DEFAULT 0,

    interest_rate    NUMERIC(6,4),

    daily_limit      NUMERIC(14,2),
    withdrawn_today  NUMERIC(14,2)  DEFAULT 0,
    last_withdraw_date DATE
);

CREATE TABLE IF NOT EXISTS transactions (
    id               SERIAL PRIMARY KEY,
    account_number   VARCHAR(20)    NOT NULL REFERENCES accounts(account_number) ON DELETE CASCADE,
    type             VARCHAR(20)    NOT NULL,
    amount           NUMERIC(14,2)  NOT NULL,
    balance_after    NUMERIC(14,2)  NOT NULL,
    txn_date         TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_transactions_account ON transactions(account_number);
