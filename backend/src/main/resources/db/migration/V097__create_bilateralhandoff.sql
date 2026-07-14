-- bilateral-handoff reference workload — realizes specs/bilateral-handoff-l0.yaml
-- (P3-6 NEW domain). A handoff is PROPOSED between two named parties and completes
-- ONLY when BOTH have independently confirmed (BHO-FSM-001); either declining voids
-- it terminally (BHO-VOID-001). custody_holder starts at releasor_party and flips to
-- receiver_party exactly once, atomically with COMPLETED (BHO-ATOMIC-001).

CREATE TABLE handoffs (
    id                     UUID         NOT NULL PRIMARY KEY,
    releasor_party         VARCHAR(200) NOT NULL,
    receiver_party         VARCHAR(200) NOT NULL,
    status                 VARCHAR(16)  NOT NULL,          -- PROPOSED | COMPLETED | VOIDED
    custody_holder         VARCHAR(200) NOT NULL,          -- releasor_party until COMPLETED
    releasor_confirmed_at  TIMESTAMP,
    receiver_confirmed_at  TIMESTAMP,
    version                BIGINT       NOT NULL DEFAULT 0,
    created_at             TIMESTAMP    NOT NULL,
    -- BHO-ATOMIC-001 — a COMPLETED row always has BOTH confirmations recorded; custody is always
    -- one of the two named parties (never a third value, never left unset).
    CONSTRAINT chk_handoff CHECK (
        (status <> 'COMPLETED' OR (releasor_confirmed_at IS NOT NULL AND receiver_confirmed_at IS NOT NULL))
        AND (custody_holder = releasor_party OR custody_holder = receiver_party)
    )
);
