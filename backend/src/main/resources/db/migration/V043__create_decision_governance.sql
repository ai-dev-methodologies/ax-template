-- decision-governance reference workload — realizes specs/decision-governance-l0.yaml
-- (IDW10 insurance dogfood decision-governance trio + IDW16-G2: a computed determination
-- snapshots its appraisal-sufficient basis immutably, re-determines only by appending a
-- reasoned NEW version, and a manual override requires a justification plus a four-eyes
-- approver distinct from the requester — both DB-backstopped).

CREATE TABLE decision_scopes (
    id              UUID         NOT NULL PRIMARY KEY,
    scope_key       VARCHAR(200) NOT NULL,
    current_version INT          NOT NULL,          -- cheap latest pointer (DG-CHAIN-001)
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL,
    CONSTRAINT chk_decision_scope_version CHECK (current_version >= 1)
);

CREATE UNIQUE INDEX uq_decision_scope_key ON decision_scopes (scope_key);

-- Append-only determination chain; rows are never UPDATEd or deleted (DG-RECOMPUTE-001).
CREATE TABLE decision_versions (
    id          UUID          NOT NULL PRIMARY KEY,
    scope_id    UUID          NOT NULL REFERENCES decision_scopes(id),
    version_no  INT           NOT NULL,
    kind        VARCHAR(20)   NOT NULL,             -- COMPUTED | RECOMPUTED | OVERRIDE
    basis_json  VARCHAR(4000) NOT NULL,             -- appraisal-sufficient snapshot (DG-BASIS-001)
    outcome     VARCHAR(500)  NOT NULL,
    reason      VARCHAR(1000),
    decided_by  VARCHAR(200)  NOT NULL,
    approved_by VARCHAR(200),
    decided_at  TIMESTAMP     NOT NULL,
    -- DG-RECOMPUTE-001 reason + DG-OVERRIDE-001 four-eyes (approver present AND distinct).
    CONSTRAINT chk_decision_version CHECK (
        version_no >= 1
        AND (kind = 'COMPUTED' OR LENGTH(TRIM(reason)) > 0)
        AND (kind <> 'OVERRIDE' OR (approved_by IS NOT NULL AND approved_by <> decided_by))
    )
);

-- one monotonic chain per scope — duplicate version unrepresentable (DG-CHAIN-001).
CREATE UNIQUE INDEX uq_decision_scope_version ON decision_versions (scope_id, version_no);
CREATE INDEX ix_decision_version_scope ON decision_versions (scope_id, version_no);
