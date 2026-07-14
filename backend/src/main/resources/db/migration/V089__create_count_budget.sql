-- periodic-count-budget reference workload — realizes specs/periodic-count-budget-l0.yaml (P3-28,
-- backlog-100 Lane B energy wave). A per-subject recurring count budget: the policy row is the single
-- serialization point for lazy calendar-period creation AND consumption; a period's captured cap is
-- immutable evidence; the consumed count is derived from the append-only consumption ledger, never stored.

CREATE TABLE count_budget_policies (
    id          UUID          NOT NULL PRIMARY KEY,
    subject_key VARCHAR(200)  NOT NULL,
    cadence     VARCHAR(10)   NOT NULL,
    cap         INTEGER       NOT NULL,
    version     BIGINT        NOT NULL DEFAULT 0,
    created_at  TIMESTAMP     NOT NULL,
    CONSTRAINT chk_count_budget_policy_cap CHECK (cap > 0)
);

CREATE UNIQUE INDEX uq_count_budget_policy_subject ON count_budget_policies (subject_key);

-- PCB-RESET-001 / PCB-CAP-001 — lazily created per (policy, period_key); cap_at_period_start is captured
-- ONCE at first touch and never reshaped by a later policy cap change.
CREATE TABLE count_budget_periods (
    id                  UUID          NOT NULL PRIMARY KEY,
    policy_id           UUID          NOT NULL,
    period_key          VARCHAR(20)   NOT NULL,
    cap_at_period_start INTEGER       NOT NULL,
    first_touched_at    TIMESTAMP     NOT NULL,
    CONSTRAINT chk_count_budget_period_cap CHECK (cap_at_period_start > 0)
);

CREATE UNIQUE INDEX uq_count_budget_period_key ON count_budget_periods (policy_id, period_key);

-- PCB-AUDIT-001 — append-only consumption ledger; the consumed count is COUNT(*) of these rows.
CREATE TABLE count_budget_consumptions (
    id           UUID      NOT NULL PRIMARY KEY,
    period_id    UUID      NOT NULL,
    sequence_no  BIGINT    NOT NULL,
    consumed_at  TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uq_count_budget_consumption_seq ON count_budget_consumptions (period_id, sequence_no);
