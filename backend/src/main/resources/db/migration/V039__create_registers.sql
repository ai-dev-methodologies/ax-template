-- monotone-register reference workload — realizes specs/monotone-register-l0.yaml
-- (IDW14 energy dogfood flagship: VALUE-monotone cumulative register, RFC 2578 Counter semantics).
-- A register reading only ever increases; consumption is the delta = curr − prior; a decrease is a
-- governed ROLLOVER (wrapped delta) or EXCHANGE (baseline reset) — never a silent negative.

CREATE TABLE registers (
    id           UUID          NOT NULL PRIMARY KEY,
    scope_key    VARCHAR(200)  NOT NULL,
    modulus      NUMERIC(19,4) NOT NULL,        -- wrap ceiling; readings live in [0, modulus)
    anchor_value NUMERIC(19,4) NOT NULL,        -- "anchor" alone risks reserved-word DDL
    version      BIGINT        NOT NULL DEFAULT 0,
    created_at   TIMESTAMP     NOT NULL,
    -- REG-MONOTONE-001 — the anchor stays within [0, modulus); modulus positive.
    CONSTRAINT chk_register_anchor CHECK (anchor_value >= 0 AND modulus > 0 AND anchor_value < modulus)
);

CREATE UNIQUE INDEX uq_register_scope ON registers (scope_key);

-- REG-DELTA-001 — append-only; rows are never UPDATEd or deleted (a correction is a NEW appended read).
CREATE TABLE register_readings (
    id            UUID          NOT NULL PRIMARY KEY,
    register_id   UUID          NOT NULL,
    kind          VARCHAR(20)   NOT NULL,
    reading_value NUMERIC(19,4) NOT NULL,
    prior_anchor  NUMERIC(19,4) NOT NULL,
    delta         NUMERIC(19,4) NOT NULL,
    sequence_no   BIGINT        NOT NULL,
    reason        VARCHAR(1000),
    recorded_at   TIMESTAMP     NOT NULL,
    -- REG-MONOTONE-001 / REG-ROLLOVER-001 — consumption is never negative; the raw read is non-negative.
    CONSTRAINT chk_register_reading_delta CHECK (delta >= 0 AND reading_value >= 0)
);

-- one monotonic sequence per register — causal order, duplicate unrepresentable.
CREATE UNIQUE INDEX uq_register_reading_seq ON register_readings (register_id, sequence_no);
CREATE INDEX ix_register_reading_reg ON register_readings (register_id, sequence_no);
