-- threshold-filing-obligation reference workload — realizes specs/threshold-filing-obligation-l0.yaml
-- (P3-21: crossing a configured threshold binds a filing obligation exactly once, in the SAME
-- transaction as the register flipping to TRIGGERED — composing threshold-terminal-derivation's
-- atomic-crossing shape with deadline-obligation's closed-loop-never-auto-expires shape).

CREATE TABLE filing_registers (
    id             UUID          NOT NULL PRIMARY KEY,
    subject_key    VARCHAR(200)  NOT NULL,
    threshold_value NUMERIC(19,4) NOT NULL,
    accrued_value  NUMERIC(19,4) NOT NULL,
    status         VARCHAR(20)   NOT NULL,        -- ACTIVE | TRIGGERED (no re-trigger — by design)
    version        BIGINT        NOT NULL DEFAULT 0,
    created_at     TIMESTAMP     NOT NULL,
    -- TFO-TRIGGER-001 — accrued ≥ threshold IMPLIES TRIGGERED. LIVE under ddl-auto.
    CONSTRAINT chk_filing_register CHECK (
        threshold_value > 0 AND accrued_value >= 0
        AND (accrued_value < threshold_value OR status = 'TRIGGERED')
    )
);

CREATE UNIQUE INDEX uq_filing_register_subject ON filing_registers (subject_key);

-- TFO-FILING-RECORD-001 — bound exactly once per register (the UNIQUE index is the DB backstop);
-- subject/threshold_snapshot/trigger_instant/due_at are immutable — only status/ack_* ever change.
CREATE TABLE filing_obligations (
    id                  UUID         NOT NULL PRIMARY KEY,
    register_id         UUID         NOT NULL REFERENCES filing_registers(id),
    subject_key         VARCHAR(200) NOT NULL,
    threshold_snapshot  NUMERIC(19,4) NOT NULL,
    trigger_instant     TIMESTAMP    NOT NULL,
    due_at              TIMESTAMP    NOT NULL,
    status              VARCHAR(20)  NOT NULL,     -- OPEN | ACKNOWLEDGED (no EXPIRED — by design)
    ack_by              VARCHAR(200),
    ack_at              TIMESTAMP,
    -- TFO-DEADLINE-001 — a closed loop records who closed it, and when.
    CONSTRAINT chk_filing_obligation_ack CHECK (
        status <> 'ACKNOWLEDGED' OR (ack_by IS NOT NULL AND ack_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_filing_obligation_register ON filing_obligations (register_id);
CREATE INDEX ix_filing_obligation_overdue ON filing_obligations (status, due_at);
