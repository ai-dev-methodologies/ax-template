-- deadline-obligation reference workload — realizes specs/deadline-obligation-l0.yaml
-- (P1-10~13: a governed deadline is DERIVED from recorded axes (grounding), the EARLIEST of
-- multiple axes governs, ordered escalation rungs fire exactly once as appended additive
-- events, and the ONLY terminal is an explicit who/when acknowledgment — never auto-expire).

CREATE TABLE deadline_obligations (
    id                 UUID         NOT NULL PRIMARY KEY,
    obligation_key     VARCHAR(200) NOT NULL,
    status             VARCHAR(20)  NOT NULL,         -- OPEN | ACKNOWLEDGED (no EXPIRED — by design)
    effective_deadline TIMESTAMP    NOT NULL,         -- always min(axis candidates), never raw
    window_start       TIMESTAMP    NOT NULL,         -- earliest axis anchor (ladder window start)
    ack_by             VARCHAR(200),
    ack_at             TIMESTAMP,
    version            BIGINT       NOT NULL DEFAULT 0,
    created_at         TIMESTAMP    NOT NULL,
    -- OBL-ACK-001 — a closed loop records who closed it, and when.
    CONSTRAINT chk_obligation_ack CHECK (
        status <> 'ACKNOWLEDGED' OR (ack_by IS NOT NULL AND ack_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_obligation_key ON deadline_obligations (obligation_key);
CREATE INDEX ix_obligation_status ON deadline_obligations (status, created_at);

CREATE TABLE obligation_axes (
    id                 UUID          NOT NULL PRIMARY KEY,
    obligation_id      UUID          NOT NULL REFERENCES deadline_obligations(id),
    kind               VARCHAR(20)   NOT NULL,        -- CALENDAR | USAGE
    anchor_at          TIMESTAMP     NOT NULL,
    interval_days      INT,
    limit_units        NUMERIC(19,4),
    used_units         NUMERIC(19,4),
    units_per_day      NUMERIC(19,4),
    candidate_deadline TIMESTAMP     NOT NULL,
    -- OBL-AXIS-001 — a USAGE axis must be derivable (positive budget + declared rate).
    CONSTRAINT chk_obligation_axis CHECK (
        (kind <> 'USAGE') OR (limit_units > 0 AND units_per_day > 0 AND used_units >= 0)
    )
);

CREATE INDEX ix_obligation_axis ON obligation_axes (obligation_id, kind);

-- OBL-GROUND-001 — every (re)derivation appended; an auditor re-derives every deadline.
CREATE TABLE obligation_derivations (
    id                 UUID         NOT NULL PRIMARY KEY,
    obligation_id      UUID         NOT NULL REFERENCES deadline_obligations(id),
    axis_id            UUID         NOT NULL REFERENCES obligation_axes(id),
    candidate_deadline TIMESTAMP    NOT NULL,
    formula            VARCHAR(500) NOT NULL,
    derived_at         TIMESTAMP    NOT NULL
);

CREATE INDEX ix_obligation_derivation ON obligation_derivations (obligation_id, derived_at);

-- OBL-LADDER-001 — each rung fires exactly once: the UNIQUE pair is the DB backstop.
CREATE TABLE obligation_escalations (
    id                 UUID        NOT NULL PRIMARY KEY,
    obligation_id      UUID        NOT NULL REFERENCES deadline_obligations(id),
    rung               VARCHAR(20) NOT NULL,           -- APPROACH | IMMINENT | BREACH
    fired_at           TIMESTAMP   NOT NULL,
    deadline_at_firing TIMESTAMP   NOT NULL
);

CREATE UNIQUE INDEX uq_obligation_rung ON obligation_escalations (obligation_id, rung);
