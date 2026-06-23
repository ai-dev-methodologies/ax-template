-- valuation-run-projection reference workload — realizes specs/valuation-run-projection-l0.yaml
-- (P1-28~30: a versioned valuation run with point-in-time AS-OF read (greatest as-of ≤ T); a
-- conserving FAN-OUT to N per-position outputs whose Σ EXACTLY equals the run total (DB @Check on
-- output_sum = total_value + an independent repo SUM cross-check); REBASE-with-history (a new
-- baseline run with a forward rebased_from pointer, prior runs retained verbatim); serialized
-- concurrent recompute/rebase (subject-row PESSIMISTIC_WRITE + uq(subject_id, run_version)).

CREATE TABLE valuation_subjects (
    id               UUID         NOT NULL PRIMARY KEY,
    subject_ref      VARCHAR(200) NOT NULL,
    head_run_version INTEGER      NOT NULL DEFAULT 0,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    NOT NULL
);

-- VALRUN-ASOF/FANOUT/IMMUTABLE/CONCURRENT-001 — an immutable versioned run pinned to an as-of
-- instant + recorded basis; output_sum = total_value is the fan-out conservation DB backstop; the
-- uq(subject_id, run_version) makes a concurrent-recompute loser a deterministic 409.
CREATE TABLE valuation_runs (
    id                       UUID          NOT NULL PRIMARY KEY,
    subject_id               UUID          NOT NULL REFERENCES valuation_subjects(id),
    run_version              INTEGER       NOT NULL,
    as_of                    TIMESTAMP     NOT NULL,
    basis                    VARCHAR(1000) NOT NULL,
    total_value              NUMERIC(19,2) NOT NULL,
    output_sum               NUMERIC(19,2) NOT NULL,
    rebased_from_run_version INTEGER,                       -- null on a plain recompute; set on a rebase baseline
    created_at               TIMESTAMP     NOT NULL,
    CONSTRAINT chk_valuation_run CHECK (
        output_sum = total_value AND run_version >= 1
    )
);

CREATE UNIQUE INDEX uq_valuation_subject_version ON valuation_runs (subject_id, run_version);

-- VALRUN-FANOUT-001 — one immutable per-position output row per run; the uq(run_id, position_ref)
-- makes a duplicated position deterministic. Σ position_value over a run MUST equal the run total.
CREATE TABLE valuation_outputs (
    id             UUID          NOT NULL PRIMARY KEY,
    run_id         UUID          NOT NULL REFERENCES valuation_runs(id),
    position_ref   VARCHAR(200)  NOT NULL,
    position_value NUMERIC(19,2) NOT NULL
);

CREATE UNIQUE INDEX uq_valuation_output_position ON valuation_outputs (run_id, position_ref);
