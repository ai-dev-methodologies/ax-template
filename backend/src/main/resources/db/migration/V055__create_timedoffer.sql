-- timed-offer-exclusive-assignment reference workload — realizes
-- specs/timed-offer-exclusive-assignment-l0.yaml (P1-39 G6 timed-offer / P1-40 G7
-- exclusive-assignment / P1-41 G8 re-offer ladder). The IDW9 dispatch
-- exclusive-assignment + timed-offer pattern GENERALIZED as a standalone primitive:
-- a timed offer to a candidate for a subject (OPEN until accept/decline/deadline; a
-- @Scheduled sweep expires past-deadline OPEN offers EXACTLY ONCE, recorded SYSTEM/when);
-- at most ONE accepted offer per subject (uq(subject_id) Assignment backstop); an ordered
-- append-only re-offer ladder (each re-offer a NEW row referencing the prior).

CREATE TABLE timed_offers (
    id             UUID         NOT NULL PRIMARY KEY,
    subject_id     VARCHAR(200) NOT NULL,                  -- the thing being assigned (drives exclusivity)
    candidate      VARCHAR(200) NOT NULL,                  -- the candidate this offer is extended to
    status         VARCHAR(16)  NOT NULL,                  -- OPEN | ACCEPTED | DECLINED | EXPIRED
    deadline       TIMESTAMP    NOT NULL,                  -- the recorded timed deadline
    attempt_seq    INTEGER      NOT NULL,                  -- strictly monotonic ladder position (1,2,3,…)
    prior_offer_id UUID,                                   -- the prior offer this one re-offers (null = first)
    decided_by     VARCHAR(200),                           -- candidate on accept/decline, SYSTEM on sweep-expiry
    decided_at     TIMESTAMP,                              -- when the offer left OPEN
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMP    NOT NULL,
    -- TIMEDOFFER-LIFECYCLE/LADDER-001 — attempt position positive; an OPEN offer has no decision
    -- basis, a terminal offer always records who/when (a bare status change is unrepresentable).
    CONSTRAINT chk_timed_offer CHECK (
        attempt_seq >= 1
        AND (status <> 'OPEN' OR decided_at IS NULL)
        AND (status = 'OPEN' OR decided_at IS NOT NULL)
    )
);

-- TIMEDOFFER-EXCLUSIVE/CONCURRENT-001 — one immutable assignment per subject; the uq(subject_id)
-- index makes a second accept for the same subject (even via a different competing offer) a
-- deterministic constraint violation the service maps to 409 (CWE-362 suspenders).
CREATE TABLE timed_offer_assignments (
    id          UUID         NOT NULL PRIMARY KEY,
    subject_id  VARCHAR(200) NOT NULL,
    offer_id    UUID         NOT NULL REFERENCES timed_offers(id),
    candidate   VARCHAR(200) NOT NULL,
    assigned_at TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX uq_timed_offer_subject ON timed_offer_assignments (subject_id);
