-- completion-reset-recurring-interval reference workload — realizes
-- specs/completion-reset-recurring-interval-l0.yaml (IDW17-G12 / BACKLOG P1-50): a RECURRING
-- obligation whose interval RESETS ON COMPLETION. Completing the current occurrence advances
-- windowStart to the completion instant, so the next window is measured FROM the completion
-- (not a fixed calendar grid). due/overdue is RECOMPUTED on read, never a stored authoritative
-- boolean; the swept_overdue column is a NON-authoritative operational flag only.

CREATE TABLE recurring_obligations (
    id                UUID         NOT NULL PRIMARY KEY,
    obligation_key    VARCHAR(200) NOT NULL,
    status            VARCHAR(20)  NOT NULL,          -- OPEN only (no terminal — it recurs forever)
    interval_seconds  BIGINT       NOT NULL,          -- immutable recurring interval (window width)
    window_start      TIMESTAMP    NOT NULL,          -- CURRENT window start; advances to completion instant
    last_completed_at TIMESTAMP,
    swept_overdue     BOOLEAN      NOT NULL DEFAULT FALSE,  -- NON-authoritative (CRI-DUE-001/CRI-SWEEP-001)
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL,
    -- CRI-RESET-001 — the recurring interval must be positive.
    CONSTRAINT chk_recurring_interval CHECK (interval_seconds > 0)
);

CREATE UNIQUE INDEX uq_recurring_obligation_key ON recurring_obligations (obligation_key);
CREATE INDEX ix_recurring_obligation_status ON recurring_obligations (status, created_at);

-- CRI-ONCE-001 — each window carries at most ONE occurrence: the UNIQUE pair is the exactly-once
-- DB backstop. Because completing advances windowStart, the closed window's start uniquely
-- identifies a window, so a racing second complete on the SAME window cannot append a duplicate.
CREATE TABLE recurring_occurrences (
    id                  UUID         NOT NULL PRIMARY KEY,
    obligation_id       UUID         NOT NULL REFERENCES recurring_obligations(id),
    closed_window_start TIMESTAMP    NOT NULL,
    completed_by        VARCHAR(200) NOT NULL,
    completed_at        TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX uq_recurring_window ON recurring_occurrences (obligation_id, closed_window_start);
CREATE INDEX ix_recurring_occurrence ON recurring_occurrences (obligation_id, completed_at);
