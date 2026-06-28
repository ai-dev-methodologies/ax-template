-- currency-arithmetic domain (CCY-FAILCLOSED-ADD / CCY-FAILCLOSED-SUBTRACT / CCY-SAMECCY-OK / CCY-EXPLICIT-CONVERT)
-- A currency-TAGGED monetary value object (CurrencyMoney) whose arithmetic is FAIL-CLOSED across
-- currencies: adding/subtracting amounts of different ISO-4217 currencies absent an explicit
-- recorded conversion THROWS. This persisted ledger lets a black-box test exercise the invariant.
-- Money is in integer minor units. The exchange RATE is out of scope (converted amount is supplied).

-- The single-currency balance (CurrencyLedger aggregate root). The currency_code tag is immutable
-- (@Column(updatable=false)) so a cross-currency add can never be retroactively legitimized.
CREATE TABLE currency_ledgers (
    id            UUID    NOT NULL,
    version       BIGINT  NOT NULL DEFAULT 0,
    currency_code VARCHAR(3) NOT NULL,
    balance_minor BIGINT  NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_currency_ledgers PRIMARY KEY (id)
);

-- Recorded cross-currency conversions (@ElementCollection on the CurrencyLedger aggregate root):
-- the audit trail that makes the sanctioned conversion path explicit, never implicit.
CREATE TABLE currency_ledger_conversions (
    ledger_id       UUID    NOT NULL,
    from_currency   VARCHAR(3) NOT NULL,
    to_currency     VARCHAR(3) NOT NULL,
    source_minor    BIGINT  NOT NULL,
    converted_minor BIGINT  NOT NULL,
    recorded_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_currency_ledger_conversions_ledger
        FOREIGN KEY (ledger_id) REFERENCES currency_ledgers(id)
);

CREATE INDEX idx_currency_ledger_conversions_ledger ON currency_ledger_conversions (ledger_id);
