-- interval-exclusivity reference workload — realizes specs/interval-exclusivity-l0.yaml
-- (Lane G capital-markets wave: booking a resource for a half-open interval [start,end) rejects any
-- overlap; back-to-back intervals are legal. HONEST DEGRADATION: PostgreSQL's EXCLUDE USING gist is
-- NOT expressible on H2 — exclusivity here rests on a PESSIMISTIC_WRITE row lock on the resource
-- anchor row, acquired by the service before every overlap check, not a DB-level constraint).

CREATE TABLE booking_resources (
    id          UUID          NOT NULL PRIMARY KEY,
    resource_key VARCHAR(200) NOT NULL,
    version     BIGINT        NOT NULL DEFAULT 0,
    created_at  TIMESTAMP     NOT NULL
);

CREATE UNIQUE INDEX uq_ivx_resource_key ON booking_resources (resource_key);

CREATE TABLE bookings (
    id          UUID          NOT NULL PRIMARY KEY,
    resource_key VARCHAR(200) NOT NULL,
    start_at    TIMESTAMP     NOT NULL,
    end_at      TIMESTAMP     NOT NULL,
    status      VARCHAR(20)   NOT NULL,        -- ACTIVE | CANCELLED (terminal)
    version     BIGINT        NOT NULL DEFAULT 0,
    created_at  TIMESTAMP     NOT NULL,
    CONSTRAINT chk_ivx_booking_interval CHECK (start_at < end_at)
);

CREATE INDEX idx_bookings_resource_status ON bookings (resource_key, status);
