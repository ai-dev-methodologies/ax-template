-- self-reported-input-plausibility-l0 extension — DATE-typed asserted fact plausibility
-- (BACKLOG P3-16: PLAUSIBILITY-DATE-RANGE-001 + PLAUSIBILITY-DATE-FUTURE-001). Distinct from
-- the numeric plausibility_channels: no prior/rate-of-change concept, just a window check of
-- an asserted date against the reference instant (injected Clock), inclusive at both edges.

CREATE TABLE date_plausibility_channels (
    id                    UUID         NOT NULL PRIMARY KEY,
    subject_ref           VARCHAR(200) NOT NULL,
    max_lookback_seconds  BIGINT       NOT NULL,        -- how far in the past an asserted date may lie
    max_lookahead_seconds BIGINT       NOT NULL,        -- how far in the future an asserted date may lie
    version               BIGINT       NOT NULL DEFAULT 0,
    created_at            TIMESTAMP    NOT NULL,
    CONSTRAINT chk_date_plausibility_channel CHECK (
        max_lookback_seconds >= 0 AND max_lookahead_seconds >= 0
    )
);

-- PLAUSIBILITY-DATE-RANGE-001 — one immutable ACCEPTED reading; SELF_REPORTED_UNVERIFIED, same
-- unverified-provenance contract as the numeric channel; carries the reference instant used.
CREATE TABLE date_plausibility_readings (
    id                  UUID        NOT NULL PRIMARY KEY,
    channel_id          UUID        NOT NULL REFERENCES date_plausibility_channels(id),
    asserted_at         TIMESTAMP   NOT NULL,
    reference_at        TIMESTAMP   NOT NULL,
    verification_status VARCHAR(40) NOT NULL,           -- only ever SELF_REPORTED_UNVERIFIED
    actor               VARCHAR(200) NOT NULL,
    occurred_at         TIMESTAMP   NOT NULL,
    CONSTRAINT chk_date_plausibility_reading CHECK (
        verification_status = 'SELF_REPORTED_UNVERIFIED'
    )
);

-- same reject/flag semantics as plausibility_rejected_attempts — never silently dropped.
CREATE TABLE date_plausibility_rejected_attempts (
    id           UUID        NOT NULL PRIMARY KEY,
    channel_id   UUID        NOT NULL REFERENCES date_plausibility_channels(id),
    asserted_at  TIMESTAMP   NOT NULL,
    reference_at TIMESTAMP   NOT NULL,
    reason       VARCHAR(40) NOT NULL,                  -- IMPLAUSIBLE_DATE_RANGE
    actor        VARCHAR(200) NOT NULL,
    occurred_at  TIMESTAMP   NOT NULL
);

CREATE INDEX idx_date_plausibility_readings_channel ON date_plausibility_readings (channel_id, occurred_at);
CREATE INDEX idx_date_plausibility_rejected_channel ON date_plausibility_rejected_attempts (channel_id, occurred_at);
