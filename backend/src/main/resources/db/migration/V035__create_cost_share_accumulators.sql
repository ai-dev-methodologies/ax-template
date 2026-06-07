-- cost-share reference workload — realizes specs/accumulator-consume-l0.yaml +
-- specs/ordered-waterfall-l0.yaml (IDW10 dogfood signature cluster: NON-REJECTING ordered
-- money-accumulation / the member-liability cost-share waterfall). version = JPA @Version.
-- BigDecimal money columns: precision 19, scale 4 (exact decimal, never float).

CREATE TABLE cost_share_accumulators (
    id           UUID           NOT NULL PRIMARY KEY,
    scope_key    VARCHAR(200)   NOT NULL,
    limit_amount NUMERIC(19, 4) NOT NULL,
    used         NUMERIC(19, 4) NOT NULL,
    version      BIGINT         NOT NULL DEFAULT 0,
    created_at   TIMESTAMP      NOT NULL,
    -- ACC-CLAWBACK-001 — a reversal can never drive accumulated usage below zero (solvency floor).
    CONSTRAINT chk_cost_share_used_nonneg CHECK (used >= 0),
    -- ACC-RACE-001 — defense-in-depth: a future code path that bypasses the atomic consume cannot
    -- persist an over-draw past the limit (the non-rejecting analogue of CHECK(taken<=capacity)).
    CONSTRAINT chk_cost_share_used_within_limit CHECK (used <= limit_amount)
);

-- one accumulator per scope (member + benefit-period + tier) — a draw locks this row FOR UPDATE.
CREATE UNIQUE INDEX uq_cost_share_scope_key ON cost_share_accumulators (scope_key);
