-- reserve-settle-balance reference workload — realizes specs/reserve-settle-balance-l0.yaml
-- (IDW13 telecom dogfood flagship: two-phase reserve→settle pooled-balance lifecycle, RFC 4006).
-- A pooled balance is drawn in two phases: an over-reserve-safe RESERVE places a hold, a SETTLE
-- commits the actual (≤ hold) and returns the unused remainder. available = funded − committed − reserved.

CREATE TABLE reservable_balances (
    id               UUID          NOT NULL PRIMARY KEY,
    scope_key        VARCHAR(200)  NOT NULL,
    funded_amount    NUMERIC(19,4) NOT NULL,
    committed_amount NUMERIC(19,4) NOT NULL DEFAULT 0,   -- "committed"/"reserved" risk reserved-word DDL
    reserved_amount  NUMERIC(19,4) NOT NULL DEFAULT 0,
    version          BIGINT        NOT NULL DEFAULT 0,
    created_at       TIMESTAMP     NOT NULL,
    -- RSV-RESERVE-001 / RSV-CONSERVE-001 — solvency: never over-reserve/over-spend, never negative.
    CONSTRAINT chk_reservable_solvency CHECK (
        committed_amount >= 0 AND reserved_amount >= 0
        AND committed_amount + reserved_amount <= funded_amount)
);

CREATE UNIQUE INDEX uq_reservable_scope ON reservable_balances (scope_key);

CREATE TABLE reservations (
    id             UUID          NOT NULL PRIMARY KEY,
    balance_id     UUID          NOT NULL,
    amount         NUMERIC(19,4) NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    settled_amount NUMERIC(19,4),
    expires_at     TIMESTAMP     NOT NULL,
    created_at     TIMESTAMP     NOT NULL,
    version        BIGINT        NOT NULL DEFAULT 0,
    -- RSV-SETTLE-001 — the settled actual can never exceed the held amount (load-bearing overspend guard).
    CONSTRAINT chk_reservation_amount  CHECK (amount > 0),
    CONSTRAINT chk_reservation_settled CHECK (settled_amount IS NULL
        OR (settled_amount >= 0 AND settled_amount <= amount))
);

CREATE INDEX ix_reservation_balance ON reservations (balance_id, status);
-- RSV-SWEEP-001 — due-hold lookup for the timeout sweep (status + expiry).
CREATE INDEX ix_reservation_due ON reservations (status, expires_at);
