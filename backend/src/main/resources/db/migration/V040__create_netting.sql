-- collection-conservation reference workload — realizes specs/collection-conservation-l0.yaml
-- (IDW15 capital-markets dogfood flagship: SET-WIDE / population-level conservation = multilateral
-- netting). N×N directed gross obligations reduce to one signed net per member, conserving both
-- per-node (net = Σ received − Σ sent) and set-wide (Σ all member nets == 0 per currency).

CREATE TABLE netting_runs (
    id         UUID          NOT NULL PRIMARY KEY,
    run_key    VARCHAR(200)  NOT NULL,
    currency   VARCHAR(3)    NOT NULL,
    status     VARCHAR(20)   NOT NULL,
    net_total  NUMERIC(19,4) NOT NULL DEFAULT 0,
    version    BIGINT        NOT NULL DEFAULT 0,
    created_at TIMESTAMP     NOT NULL,
    -- NET-SETWIDE-ZERO-001 — the rollup of all member nets is ALWAYS exactly zero (set-wide closure).
    CONSTRAINT chk_netting_setwide_zero CHECK (net_total = 0)
);

CREATE UNIQUE INDEX uq_netting_run_key ON netting_runs (run_key);

-- NET-INPUTS-IMMUTABLE-001 — append-only directed gross obligations (fromMember owes toMember).
CREATE TABLE netting_gross_obligations (
    id          UUID          NOT NULL PRIMARY KEY,
    run_id      UUID          NOT NULL,
    from_member VARCHAR(120)  NOT NULL,
    to_member   VARCHAR(120)  NOT NULL,
    amount      NUMERIC(19,4) NOT NULL,
    currency    VARCHAR(3)    NOT NULL,
    created_at  TIMESTAMP     NOT NULL,
    -- positive amount; no self-obligation.
    CONSTRAINT chk_netting_obligation CHECK (amount > 0 AND from_member <> to_member)
);

CREATE INDEX ix_netting_obligation_run ON netting_gross_obligations (run_id);

-- computed signed net per member (positive = net creditor, negative = net debtor).
CREATE TABLE netting_net_positions (
    id         UUID          NOT NULL PRIMARY KEY,
    run_id     UUID          NOT NULL,
    member_id  VARCHAR(120)  NOT NULL,
    net_amount NUMERIC(19,4) NOT NULL
);

-- NET-ONCE-001 — one position per (run, member): a concurrent double-reduction is unrepresentable.
CREATE UNIQUE INDEX uq_net_position_run_member ON netting_net_positions (run_id, member_id);
