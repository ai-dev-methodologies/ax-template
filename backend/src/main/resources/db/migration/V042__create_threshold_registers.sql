-- threshold-terminal-derivation reference workload — realizes specs/threshold-terminal-derivation-l0.yaml
-- (IDW17 aviation/MRO dogfood flagship: a cumulative anchor with a mandatory limit; the crossing accrual
-- drives the IRREVERSIBLE terminal in the SAME transaction, and the implication anchor >= limit =>
-- EXPIRED is DB-backstopped so a live over-limit row is unrepresentable).
-- Column names: limit_value / anchor_value — bare "limit" is a SQL reserved word.

CREATE TABLE threshold_registers (
    id           UUID          NOT NULL PRIMARY KEY,
    scope_key    VARCHAR(200)  NOT NULL,
    limit_value  NUMERIC(19,4) NOT NULL,        -- mandatory replacement limit (14 CFR 43.10)
    anchor_value NUMERIC(19,4) NOT NULL,        -- accumulated life status (cycles / hours / uses)
    status       VARCHAR(20)   NOT NULL,        -- ACTIVE | EXPIRED (terminal, zero outgoing edges)
    version      BIGINT        NOT NULL DEFAULT 0,
    created_at   TIMESTAMP     NOT NULL,
    -- TTD-CHECK-001 — anchor >= limit IMPLIES terminal; limit positive; anchor non-negative.
    CONSTRAINT chk_threshold_terminal CHECK (
        limit_value > 0 AND anchor_value >= 0
        AND (anchor_value < limit_value OR status = 'EXPIRED')
    )
);

CREATE UNIQUE INDEX uq_threshold_register_scope ON threshold_registers (scope_key);
