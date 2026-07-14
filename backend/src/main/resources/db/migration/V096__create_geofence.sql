-- geofence-transition reference workload — realizes specs/geofence-transition-l0.yaml
-- (P3-5 NEW domain). A raw containment observation is not immediately a committed
-- transition — it is CONFIRMED only after sustaining, unreversed, for the minimum
-- dwell duration (GEOFENCE-DWELL-001); rapid flapping within the dwell window
-- commits ZERO transitions (GEOFENCE-FLAP-SUPPRESS-001). A confirmed transition is
-- immutable and dual-timestamped (observed-at vs confirmed-at, GEOFENCE-CONFIRM-001).

CREATE TABLE geofence_trackers (
    id                UUID         NOT NULL PRIMARY KEY,
    subject_id        VARCHAR(200) NOT NULL,
    zone_id           VARCHAR(200) NOT NULL,
    confirmed_state   VARCHAR(16)  NOT NULL,             -- OUTSIDE | INSIDE
    pending_direction VARCHAR(16),                       -- ENTER | EXIT | null (no pending)
    pending_since     TIMESTAMP,                         -- event-time — first raw observation of the pending window
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX uq_geofence_tracker_subject_zone ON geofence_trackers (subject_id, zone_id);

-- GEOFENCE-CONFIRM-001 — immutable, dual-timestamped. confirmed_at is never before observed_at
-- (the dwell threshold can only be satisfied by elapsed, non-negative time).
CREATE TABLE geofence_transitions (
    id            UUID         NOT NULL PRIMARY KEY,
    tracker_id    UUID         NOT NULL REFERENCES geofence_trackers(id),
    zone_id       VARCHAR(200) NOT NULL,
    direction     VARCHAR(16)  NOT NULL,                 -- ENTER | EXIT
    observed_at   TIMESTAMP    NOT NULL,
    confirmed_at  TIMESTAMP    NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    CONSTRAINT chk_geofence_transition_dual_timestamp CHECK (confirmed_at >= observed_at)
);
