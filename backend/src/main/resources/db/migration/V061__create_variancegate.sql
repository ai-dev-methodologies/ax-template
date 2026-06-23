-- variance-tolerance-band reference workload — realizes specs/variance-tolerance-band-l0.yaml
-- (P1-48: a standard-vs-actual appraisal whose variance is DERIVED (actual − standard), gated by an
-- ASYMMETRIC tolerance band PINNED on the row, that BLOCKS a dependent operation on a breach until an
-- explicit accountable disposition (who/when/reason) is recorded; concurrent dispositions serialize).

CREATE TABLE variance_appraisals (
    id              UUID          NOT NULL PRIMARY KEY,
    subject         VARCHAR(200)  NOT NULL,
    standard_value  NUMERIC(19,4) NOT NULL,                  -- the recorded standard (budget/spec/expected)
    actual_value    NUMERIC(19,4) NOT NULL,                  -- the measured actual
    variance        NUMERIC(19,4) NOT NULL,                  -- DERIVED: actual_value − standard_value
    lower_tolerance NUMERIC(19,4) NOT NULL,                  -- favorable-side allowance magnitude (pinned)
    upper_tolerance NUMERIC(19,4) NOT NULL,                  -- unfavorable-side allowance magnitude (pinned)
    verdict         VARCHAR(20)   NOT NULL,                  -- WITHIN_TOLERANCE | OUT_OF_TOLERANCE
    disposed        BOOLEAN       NOT NULL DEFAULT FALSE,
    version         BIGINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMP     NOT NULL,
    -- VG-DERIVE/GATE/DISPOSE-001 — tolerance magnitudes non-negative; the variance MUST equal the
    -- derived difference (a hand-entered variance that disagrees is unrepresentable); a disposed
    -- flag implies a breach (a within-tolerance appraisal cannot be "disposed").
    CONSTRAINT chk_variance_appraisal CHECK (
        lower_tolerance >= 0 AND upper_tolerance >= 0
        AND variance = actual_value - standard_value
        AND (disposed = FALSE OR verdict = 'OUT_OF_TOLERANCE')
    )
);

-- VG-DISPOSE/CONCURRENT-001 — one immutable accountable disposition per appraisal; the uq is the
-- exactly-once backstop that makes a re-dispose / concurrent-dispose loser a deterministic 409.
CREATE TABLE variance_dispositions (
    id           UUID          NOT NULL PRIMARY KEY,
    appraisal_id UUID          NOT NULL REFERENCES variance_appraisals(id),
    decision     VARCHAR(20)   NOT NULL,                     -- OVERRIDE | REJECT
    actor        VARCHAR(200)  NOT NULL,
    reason       VARCHAR(1000) NOT NULL,
    decided_at   TIMESTAMP     NOT NULL
);

CREATE UNIQUE INDEX uq_variance_appraisal ON variance_dispositions (appraisal_id);
