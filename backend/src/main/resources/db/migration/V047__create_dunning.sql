-- dunning-collections reference workload — realizes specs/dunning-collections-l0.yaml
-- (P1-20~23: a one-way dunning ladder REMINDER→NOTICE→FINAL_NOTICE→SUSPENDED with EXACTLY-ONCE
-- stage transitions (uq(case_id, stage) backstop); a deterministic recorded aging bucket; a
-- cure-period grace window that halts/resumes the ladder; serialized concurrent advance).

CREATE TABLE dunning_cases (
    id                    UUID          NOT NULL PRIMARY KEY,
    receivable_ref        VARCHAR(200)  NOT NULL,
    due_date              DATE          NOT NULL,
    overdue_amount        NUMERIC(19,2) NOT NULL,
    paid_amount           NUMERIC(19,2) NOT NULL DEFAULT 0,
    stage                 VARCHAR(20)   NOT NULL,        -- REMINDER | NOTICE | FINAL_NOTICE | SUSPENDED
    aging_bucket          VARCHAR(20)   NOT NULL,        -- CURRENT | B1_30 | B2_60 | B3_90_PLUS
    aging_as_of           TIMESTAMP,                     -- recorded basis for aging_bucket
    days_overdue          BIGINT        NOT NULL DEFAULT 0,
    cure_window_opened_at TIMESTAMP,
    cure_deadline         TIMESTAMP,
    ladder_halted         BOOLEAN       NOT NULL DEFAULT FALSE,
    version               BIGINT        NOT NULL DEFAULT 0,
    created_at            TIMESTAMP     NOT NULL,
    -- DUNNING-AGING/CURE-001 — amounts non-negative; a cure deadline implies an opened window;
    -- a halted ladder implies the case has been cured back to CURRENT.
    CONSTRAINT chk_dunning_case CHECK (
        overdue_amount >= 0 AND paid_amount >= 0
        AND (cure_deadline IS NULL OR cure_window_opened_at IS NOT NULL)
        AND (ladder_halted = FALSE OR aging_bucket = 'CURRENT')
    )
);

-- DUNNING-LADDER/CONCURRENT-001 — one immutable transition per (case, stage); the uq is the
-- exactly-once backstop that makes a re-emit / concurrent-advance loser a deterministic 409.
CREATE TABLE dunning_stage_transitions (
    id           UUID         NOT NULL PRIMARY KEY,
    case_id      UUID         NOT NULL REFERENCES dunning_cases(id),
    stage        VARCHAR(20)  NOT NULL,                  -- the rung reached
    kind         VARCHAR(20)  NOT NULL,                  -- ADVANCE | CURED
    days_overdue BIGINT       NOT NULL,
    actor        VARCHAR(200) NOT NULL,
    occurred_at  TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX uq_dunning_case_stage ON dunning_stage_transitions (case_id, stage, kind);
