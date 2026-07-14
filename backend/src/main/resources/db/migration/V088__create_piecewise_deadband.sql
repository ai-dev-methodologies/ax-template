-- piecewise-deadband reference workload — realizes specs/piecewise-deadband-l0.yaml (P3-27, backlog-100
-- Lane B energy wave). A config's segments tile a bounded domain [domain_start, domain_end) exactly; each
-- segment owns its own obligation target AND deadband width; evaluations are append-only and idempotent
-- by a deterministic (config, point, actual) hash.

CREATE TABLE deadband_configs (
    id           UUID          NOT NULL PRIMARY KEY,
    config_key   VARCHAR(200)  NOT NULL,
    domain_start NUMERIC(19,4) NOT NULL,
    domain_end   NUMERIC(19,4) NOT NULL,
    created_at   TIMESTAMP     NOT NULL
);

CREATE UNIQUE INDEX uq_deadband_config_key ON deadband_configs (config_key);

-- PWDB-SEGMENT-001 — start < end; deadband width non-negative.
CREATE TABLE deadband_segments (
    id                UUID          NOT NULL PRIMARY KEY,
    config_id         UUID          NOT NULL,
    ordinal           INTEGER       NOT NULL,
    segment_start     NUMERIC(19,4) NOT NULL,
    segment_end       NUMERIC(19,4) NOT NULL,
    obligation_target NUMERIC(19,4) NOT NULL,
    deadband_width    NUMERIC(19,4) NOT NULL,
    CONSTRAINT chk_deadband_segment_bounds CHECK (segment_start < segment_end AND deadband_width >= 0)
);

CREATE UNIQUE INDEX uq_deadband_segment_ordinal ON deadband_segments (config_id, ordinal);

-- PWDB-IMMUTABLE-001 — append-only evaluations; idempotent replay backstopped by the unique index below.
CREATE TABLE deadband_evaluations (
    id                UUID          NOT NULL PRIMARY KEY,
    config_id         UUID          NOT NULL,
    segment_id        UUID          NOT NULL,
    point_x           NUMERIC(19,4) NOT NULL,
    actual_value      NUMERIC(19,4) NOT NULL,
    obligation_target NUMERIC(19,4) NOT NULL,
    deadband_width    NUMERIC(19,4) NOT NULL,
    deviation         NUMERIC(19,4) NOT NULL,
    compliant         BOOLEAN       NOT NULL,
    idempotency_key   VARCHAR(64)   NOT NULL,
    sequence_no       BIGINT        NOT NULL,
    evaluated_at      TIMESTAMP     NOT NULL
);

CREATE UNIQUE INDEX uq_deadband_evaluation_idempotency ON deadband_evaluations (config_id, idempotency_key);
CREATE INDEX ix_deadband_evaluation_config ON deadband_evaluations (config_id, sequence_no);
