-- dispatch reference workload — realizes specs/exclusive-assignment-l0.yaml +
-- specs/timed-offer-l0.yaml (IDW9 dogfood signature cluster: exclusive, time-bounded
-- two-sided matching-assignment under fan-out contention). version = JPA @Version
-- (makes the timeout sweep LOSE the race against a live accept).

CREATE TABLE dispatch_providers (
    id                UUID         NOT NULL PRIMARY KEY,
    handle            VARCHAR(120) NOT NULL,
    status            VARCHAR(16)  NOT NULL,
    last_heartbeat_at TIMESTAMP    NOT NULL,
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL,
    CONSTRAINT chk_dispatch_provider_status
        CHECK (status IN ('OFFLINE', 'AVAILABLE', 'ASSIGNED'))
);

CREATE TABLE dispatch_requests (
    id                   UUID         NOT NULL PRIMARY KEY,
    description          VARCHAR(500) NOT NULL,
    status               VARCHAR(16)  NOT NULL,
    assigned_provider_id UUID,
    created_by           VARCHAR(255) NOT NULL,
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           TIMESTAMP    NOT NULL,
    CONSTRAINT chk_dispatch_request_status
        CHECK (status IN ('PENDING', 'OFFERED', 'ASSIGNED', 'FULFILLED', 'UNFULFILLED', 'CANCELLED'))
);

CREATE TABLE dispatch_offers (
    id          UUID        NOT NULL PRIMARY KEY,
    request_id  UUID        NOT NULL,
    provider_id UUID        NOT NULL,
    status      VARCHAR(16) NOT NULL,
    expires_at  TIMESTAMP   NOT NULL,
    ordinal     INT         NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMP   NOT NULL,
    CONSTRAINT chk_dispatch_offer_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED')),
    CONSTRAINT chk_dispatch_offer_ordinal CHECK (ordinal >= 1)
);

-- EXCL-INDEX-003 (exclusive-assignment-l0) — defense-in-depth backstop: a provider holds
-- AT MOST ONE active (ASSIGNED) request. A partial unique index enforces uniqueness only
-- over the active subset, so AVAILABLE/OFFLINE providers (assigned_provider_id NULL) are
-- unconstrained while a double-dispatch is unrepresentable at the DB.
CREATE UNIQUE INDEX uq_dispatch_request_active_provider
    ON dispatch_requests (assigned_provider_id)
    WHERE status = 'ASSIGNED';

-- OFFER-FSM-001 (timed-offer-l0) — a request has AT MOST ONE outstanding PENDING offer.
CREATE UNIQUE INDEX uq_dispatch_offer_pending_per_request
    ON dispatch_offers (request_id)
    WHERE status = 'PENDING';

-- hot paths: due-offer sweep (AVAIL-SWEEP-001) + eligible-provider scan (AVAIL-FRESH-002)
CREATE INDEX ix_dispatch_offers_due ON dispatch_offers (status, expires_at);
CREATE INDEX ix_dispatch_providers_eligible ON dispatch_providers (status, last_heartbeat_at);
