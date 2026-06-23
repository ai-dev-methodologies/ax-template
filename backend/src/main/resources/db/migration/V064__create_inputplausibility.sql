-- self-reported-input-plausibility reference workload — realizes specs/self-reported-input-plausibility-l0.yaml
-- (P1-42 / IDW9-G11: a plausibility gate for SELF-REPORTED, server-unverifiable input — a RANGE
-- bound + a RATE-OF-CHANGE limit vs the prior accepted reading; an accepted value is recorded as
-- SELF_REPORTED_UNVERIFIED with its plausibility basis; an implausible submission is rejected AND
-- recorded as an auditable attempt; concurrent submissions serialize on the channel row).

CREATE TABLE plausibility_channels (
    id                   UUID          NOT NULL PRIMARY KEY,
    subject_ref          VARCHAR(200)  NOT NULL,
    min_value            NUMERIC(38,9) NOT NULL,        -- configured plausible lower bound (inclusive)
    max_value            NUMERIC(38,9) NOT NULL,        -- configured plausible upper bound (inclusive)
    max_delta_per_second NUMERIC(38,9) NOT NULL,        -- configured rate-of-change ceiling
    prior_value          NUMERIC(38,9),                 -- prior ACCEPTED value the rate gate uses; null until first reading
    prior_at             TIMESTAMP,                     -- the prior accepted reading's instant
    version              BIGINT        NOT NULL DEFAULT 0,
    created_at           TIMESTAMP     NOT NULL,
    -- PLAUSIBILITY-RANGE/RATE-001 — a non-inverted plausible range, a non-negative rate ceiling,
    -- and a prior pointer whose value/instant are set together (both null, or both present).
    CONSTRAINT chk_plausibility_channel CHECK (
        min_value <= max_value AND max_delta_per_second >= 0
        AND (prior_value IS NULL) = (prior_at IS NULL)
    )
);

-- PLAUSIBILITY-PROVENANCE-001 — one immutable ACCEPTED reading; the value travels with its
-- UNVERIFIED provenance and the full plausibility basis on which it was admitted.
CREATE TABLE plausibility_readings (
    id                  UUID          NOT NULL PRIMARY KEY,
    channel_id          UUID          NOT NULL REFERENCES plausibility_channels(id),
    reported_value      NUMERIC(38,9) NOT NULL,
    verification_status VARCHAR(40)   NOT NULL,         -- only ever SELF_REPORTED_UNVERIFIED
    checks_ran          VARCHAR(40)   NOT NULL,         -- RANGE | RANGE,RATE
    had_prior           BOOLEAN       NOT NULL,
    prior_value         NUMERIC(38,9),                  -- the prior the rate gate used (null on the first reading)
    elapsed_seconds     BIGINT        NOT NULL,
    computed_rate       NUMERIC(38,9),                  -- |delta|/elapsed (null when no prior to compute against)
    actor               VARCHAR(200)  NOT NULL,
    occurred_at         TIMESTAMP     NOT NULL,
    -- PLAUSIBILITY-PROVENANCE-001 — an accepted value is NEVER server-confirmed; elapsed is non-negative.
    CONSTRAINT chk_plausibility_reading CHECK (
        verification_status = 'SELF_REPORTED_UNVERIFIED' AND elapsed_seconds >= 0
    )
);

-- PLAUSIBILITY-REJECT-001 — one immutable RECORDED rejected attempt per implausible submission; the
-- rejection is auditable (fraud signal / calibration), never silently dropped. Append-only.
CREATE TABLE plausibility_rejected_attempts (
    id              UUID          NOT NULL PRIMARY KEY,
    channel_id      UUID          NOT NULL REFERENCES plausibility_channels(id),
    reported_value  NUMERIC(38,9) NOT NULL,
    reason          VARCHAR(40)   NOT NULL,             -- IMPLAUSIBLE_RANGE | IMPLAUSIBLE_RATE
    prior_value     NUMERIC(38,9),
    elapsed_seconds BIGINT        NOT NULL,
    computed_rate   NUMERIC(38,9),
    actor           VARCHAR(200)  NOT NULL,
    occurred_at     TIMESTAMP     NOT NULL
);

CREATE INDEX idx_plausibility_readings_channel ON plausibility_readings (channel_id, occurred_at);
CREATE INDEX idx_plausibility_rejected_channel ON plausibility_rejected_attempts (channel_id, occurred_at);
