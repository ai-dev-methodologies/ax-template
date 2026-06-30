-- tokenized-securities TRANSFER pilot — realizes specs/tokenized-securities-l0.yaml
-- Chain-agnostic STO compliance-gated transfer. No blockchain; backend invariant model.
-- (V076 intentionally reserved for a concurrent bundle-pricing migration to avoid a Flyway version collision.)

CREATE TABLE security_token_registers (
    id                          UUID         NOT NULL PRIMARY KEY,
    token_code                  VARCHAR(100) NOT NULL,
    underlying_asset_id         VARCHAR(200) NOT NULL,
    security_type               VARCHAR(30)  NOT NULL,   -- TRUST_BENEFICIARY | INVESTMENT_CONTRACT
    total_units                 BIGINT       NOT NULL,
    issuer_holder_id            VARCHAR(200) NOT NULL,
    lockup_until                TIMESTAMP    NOT NULL,
    holding_limit_per_investor  BIGINT       NOT NULL,
    version                     BIGINT       NOT NULL DEFAULT 0,
    created_at                  TIMESTAMP    NOT NULL,
    CONSTRAINT chk_security_token_units CHECK (total_units > 0 AND holding_limit_per_investor > 0)
);
CREATE UNIQUE INDEX uq_security_token_code ON security_token_registers (token_code);
CREATE UNIQUE INDEX uq_security_token_underlying_asset ON security_token_registers (underlying_asset_id);

CREATE TABLE token_holdings (
    id           UUID         NOT NULL PRIMARY KEY,
    register_id  UUID         NOT NULL REFERENCES security_token_registers (id),
    holder_id    VARCHAR(200) NOT NULL,
    units        BIGINT       NOT NULL,
    CONSTRAINT chk_token_holding_units CHECK (units >= 0)
);
CREATE UNIQUE INDEX uq_token_holding_holder ON token_holdings (register_id, holder_id);

CREATE TABLE transfer_entries (
    id             UUID         NOT NULL PRIMARY KEY,
    register_id    UUID         NOT NULL REFERENCES security_token_registers (id),
    from_holder_id VARCHAR(200) NOT NULL,
    to_holder_id   VARCHAR(200) NOT NULL,
    units          BIGINT       NOT NULL,
    transfer_id    VARCHAR(200) NOT NULL,
    recorded_at    TIMESTAMP    NOT NULL
);
CREATE UNIQUE INDEX uq_transfer_entry_transfer_id ON transfer_entries (register_id, transfer_id);

CREATE TABLE eligible_investors (
    id           UUID         NOT NULL PRIMARY KEY,
    register_id  UUID         NOT NULL,
    holder_id    VARCHAR(200) NOT NULL,
    granted_at   TIMESTAMP    NOT NULL
);
CREATE UNIQUE INDEX uq_eligible_investor ON eligible_investors (register_id, holder_id);
