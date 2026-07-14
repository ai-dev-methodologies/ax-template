-- saturating-balance-l0 (specs/saturating-balance-l0.yaml)
-- SATBAL-CEILING-001 / SATBAL-FLOOR-002: current_value is DB-clamped to [0, cap].
-- SATBAL-LEDGER-003: every accrual/debit records requested AND applied (signed) amounts, append-only.

CREATE TABLE saturating_balances (
    id UUID PRIMARY KEY,
    owner_id VARCHAR(200) NOT NULL,
    cap DECIMAL(15,4) NOT NULL,
    current_value DECIMAL(15,4) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_satbal_range CHECK (current_value >= 0 AND current_value <= cap)
);

CREATE TABLE saturating_ledger_entries (
    id UUID PRIMARY KEY,
    balance_id UUID NOT NULL,
    op VARCHAR(10) NOT NULL,
    requested_amount DECIMAL(15,4) NOT NULL,
    applied_amount DECIMAL(15,4) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_satbal_ledger_balance FOREIGN KEY (balance_id) REFERENCES saturating_balances(id)
);
CREATE INDEX idx_satbal_ledger_balance ON saturating_ledger_entries(balance_id);
