-- signed-dual-register (net-metering) reference workload — realizes specs/signed-dual-register-l0.yaml
-- (IDW14 energy residual, P1-18: bidirectional SIGNED dual register). A net meter holds TWO
-- independently value-monotone direction registers — IMPORT (+) and EXPORT (−) — and a DERIVED signed
-- net = cumulativeImport − cumulativeExport. A billing-period close snapshots both cumulatives + the net
-- delta and the period becomes immutable (a backdated reading is a 409).

CREATE TABLE net_meters (
    id                  UUID          NOT NULL PRIMARY KEY,
    meter_key           VARCHAR(200)  NOT NULL,
    cumulative_import   NUMERIC(19,4) NOT NULL,        -- IMPORT register (+); monotone
    cumulative_export   NUMERIC(19,4) NOT NULL,        -- EXPORT register (−); monotone
    net_value           NUMERIC(19,4) NOT NULL,        -- DERIVED net = cumulative_import − cumulative_export
    baseline_net        NUMERIC(19,4) NOT NULL,        -- immutable net at creation (independent-recompute baseline)
    net_at_period_start NUMERIC(19,4) NOT NULL,        -- net at the start of the currently-open period
    closed_through_at   TIMESTAMP     NOT NULL,        -- latest closed period boundary (backdate fence)
    version             BIGINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMP     NOT NULL,
    -- NETM-DIRECTION-001 — each direction cumulative is non-negative.
    CONSTRAINT chk_net_meter_cumulatives CHECK (cumulative_import >= 0 AND cumulative_export >= 0)
);

CREATE UNIQUE INDEX uq_net_meter_key ON net_meters (meter_key);

-- NETM-NET-001 — append-only basis readings; rows are never UPDATEd or deleted (a correction is a NEW reading).
CREATE TABLE net_meter_readings (
    id               UUID          NOT NULL PRIMARY KEY,
    meter_id         UUID          NOT NULL,
    direction        VARCHAR(10)   NOT NULL,
    reading_value    NUMERIC(19,4) NOT NULL,
    prior_cumulative NUMERIC(19,4) NOT NULL,
    delta            NUMERIC(19,4) NOT NULL,
    net_after        NUMERIC(19,4) NOT NULL,           -- post-append derived net (recorded BASIS)
    import_after     NUMERIC(19,4) NOT NULL,
    export_after     NUMERIC(19,4) NOT NULL,
    sequence_no      BIGINT        NOT NULL,
    effective_at     TIMESTAMP     NOT NULL,
    recorded_at      TIMESTAMP     NOT NULL,
    -- NETM-DIRECTION-001 — direction consumption is never negative; the raw read is non-negative.
    CONSTRAINT chk_net_meter_reading_delta CHECK (delta >= 0 AND reading_value >= 0)
);

-- one monotonic sequence per (meter, direction) — causal order, duplicate unrepresentable.
CREATE UNIQUE INDEX uq_net_meter_reading_seq ON net_meter_readings (meter_id, direction, sequence_no);
CREATE INDEX ix_net_meter_reading_meter ON net_meter_readings (meter_id, recorded_at, sequence_no);

-- NETM-PERIOD-001 — immutable billing-period snapshots; a closed period is frozen.
CREATE TABLE net_meter_periods (
    id                UUID          NOT NULL PRIMARY KEY,
    meter_id          UUID          NOT NULL,
    boundary_at       TIMESTAMP     NOT NULL,
    import_cumulative NUMERIC(19,4) NOT NULL,
    export_cumulative NUMERIC(19,4) NOT NULL,
    net_start         NUMERIC(19,4) NOT NULL,
    net_end           NUMERIC(19,4) NOT NULL,
    period_net_delta  NUMERIC(19,4) NOT NULL,          -- net_end − net_start (the SIGNED period offset)
    sequence_no       BIGINT        NOT NULL,
    closed_at         TIMESTAMP     NOT NULL
);

-- period boundaries move strictly forward — one monotonic sequence per meter.
CREATE UNIQUE INDEX uq_net_meter_period_seq ON net_meter_periods (meter_id, sequence_no);
CREATE INDEX ix_net_meter_period_meter ON net_meter_periods (meter_id, boundary_at);
