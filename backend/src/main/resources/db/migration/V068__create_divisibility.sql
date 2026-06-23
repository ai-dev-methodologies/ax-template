-- material-divisibility-constraint reference workload — realizes specs/material-divisibility-constraint-l0.yaml
-- (P1-54 / IDW11-G17: per-material INTEGER-vs-FRACTIONAL divisibility constraint — a REJECT gate,
-- NOT a quantizer: an INTEGER_ONLY material rejects a fractional quantity 422 NON_INTEGRAL_QUANTITY
-- and a FRACTIONAL material rejects a quantity above its recorded max decimal scale 422
-- EXCESS_PRECISION; the quantity is NEVER silently rounded or truncated. The policy is a recorded,
-- versioned per-material property; every quantity check is recorded with the policy version in force.
-- DISTINCT from order-multiple-quantization, which ROUNDS a requirement up to a lot multiple.)

CREATE TABLE material_divisibility_policies (
    id             UUID         NOT NULL PRIMARY KEY,
    material_ref   VARCHAR(200) NOT NULL,
    policy_version BIGINT       NOT NULL,                  -- monotone per-material version (a re-declaration appends)
    policy_kind    VARCHAR(20)  NOT NULL,                  -- INTEGER_ONLY | FRACTIONAL
    max_scale      INT          NOT NULL,                  -- max decimal places for FRACTIONAL; 0 for INTEGER_ONLY
    version        BIGINT       NOT NULL DEFAULT 0,         -- @Version optimistic-lock posture (row otherwise immutable)
    declared_at    TIMESTAMP    NOT NULL,
    -- DIV-POLICY/PRECISION-001 — a version is >= 1, the scale is non-negative, and an INTEGER_ONLY
    -- row carries no meaningful scale (pinned to 0) so the policy basis is unambiguous.
    CONSTRAINT chk_divisibility_policy CHECK (
        policy_version >= 1 AND max_scale >= 0
        AND (policy_kind = 'FRACTIONAL' OR max_scale = 0)
    )
);

-- DIV-POLICY-001 — append-only versioned policy: one row per (material, version); the prior version
-- is retained, never overwritten. The unique index also makes a concurrent re-declaration's
-- residual-race loser a deterministic conflict (belt to the PESSIMISTIC_WRITE suspenders).
CREATE UNIQUE INDEX uq_divisibility_material_version
    ON material_divisibility_policies (material_ref, policy_version);

CREATE TABLE divisibility_checks (
    id                 UUID          NOT NULL PRIMARY KEY,
    material_ref       VARCHAR(200)  NOT NULL,
    submitted_quantity NUMERIC(38,18) NOT NULL,            -- recorded VERBATIM — never rounded/truncated
    verdict            VARCHAR(20)   NOT NULL,             -- ACCEPTED | NON_INTEGRAL | EXCESS_PRECISION
    policy_version     BIGINT        NOT NULL,             -- the policy version in force at the check
    checked_at         TIMESTAMP     NOT NULL
);

CREATE INDEX idx_divisibility_checks_material ON divisibility_checks (material_ref, checked_at);
