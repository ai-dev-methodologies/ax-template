-- external-reconciliation reference workload — realizes specs/external-reconciliation-l0.yaml
-- (P1-32 IDW15-G12: match an internal record set against an external feed snapshot, CLASSIFY each
-- pair exactly once with its recorded basis (internal/external amount + delta), require explicit
-- human DISPOSITION of every BREAK before a run can be RESOLVED, and re-run IDEMPOTENTLY on the
-- feed snapshot hash; concurrent dispose on one break serializes — exactly one wins).

CREATE TABLE reconciliation_runs (
    id                 UUID         NOT NULL PRIMARY KEY,
    source_key         VARCHAR(200) NOT NULL,
    feed_snapshot_hash VARCHAR(200) NOT NULL,
    status             VARCHAR(20)  NOT NULL,          -- OPEN | RESOLVED
    resolved_at        TIMESTAMP,
    version            BIGINT       NOT NULL DEFAULT 0,
    created_at         TIMESTAMP    NOT NULL
);

-- RECON-IDEMPOTENT-001 — the (source, feed-hash) identity: a re-run on the SAME feed returns the
-- existing run; a CHANGED feed appends a new run (a different hash → a different row).
CREATE UNIQUE INDEX uq_recon_source_feed ON reconciliation_runs (source_key, feed_snapshot_hash);

-- RECON-CLASSIFY/DISPOSE-001 — one immutable classified pair per (run, key); the disposition is
-- written once. The @Check backstops: a disposed item must be a BREAK, and a disposed item must
-- carry every disposition field (no half-written disposition, no disposed non-break).
CREATE TABLE reconciliation_items (
    id                 UUID          NOT NULL PRIMARY KEY,
    run_id             UUID          NOT NULL REFERENCES reconciliation_runs(id),
    item_key           VARCHAR(200)  NOT NULL,
    classification     VARCHAR(20)   NOT NULL,         -- MATCHED | BREAK | INTERNAL_ONLY | EXTERNAL_ONLY
    internal_amount    NUMERIC(19,4),
    external_amount    NUMERIC(19,4),
    delta              NUMERIC(19,4),
    disposed           BOOLEAN       NOT NULL DEFAULT FALSE,
    disposition_type   VARCHAR(20),                    -- ACCEPT_INTERNAL | ACCEPT_EXTERNAL | ADJUST
    disposed_by        VARCHAR(200),
    disposed_at        TIMESTAMP,
    disposition_reason VARCHAR(1000),
    version            BIGINT        NOT NULL DEFAULT 0,
    created_at         TIMESTAMP     NOT NULL,
    CONSTRAINT chk_recon_item CHECK (
        (disposed = FALSE OR classification = 'BREAK')
        AND (disposed = FALSE OR (disposition_type IS NOT NULL
            AND disposed_by IS NOT NULL AND disposed_at IS NOT NULL AND disposition_reason IS NOT NULL))
    )
);

-- RECON-CLASSIFY-001 — exactly one classified item per (run, key); a duplicate key is impossible.
CREATE UNIQUE INDEX uq_recon_run_item ON reconciliation_items (run_id, item_key);
