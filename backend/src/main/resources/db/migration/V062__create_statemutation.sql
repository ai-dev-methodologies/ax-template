-- state-conditional-mutability reference workload — realizes specs/state-conditional-mutability-l0.yaml
-- (P1-49: which FIELDS of a GovernedForm are mutable is a function of its CURRENT STATE — a DECLARED
-- per-(state,field) authority table, monotonically tightened DRAFT⊇SUBMITTED⊇APPROVED⊇LOCKED, with
-- widening only through a RECORDED governed re-open, and re-checked under the form's row lock (CWE-367)).

CREATE TABLE governed_forms (
    id                UUID          NOT NULL PRIMARY KEY,
    owner             VARCHAR(200)  NOT NULL,
    title             VARCHAR(500),
    body              VARCHAR(4000),
    reviewer_note     VARCHAR(2000),
    state             VARCHAR(20)   NOT NULL,        -- DRAFT | SUBMITTED | APPROVED | LOCKED
    last_edited_field VARCHAR(20),                   -- recorded basis of the last edit (TITLE|BODY|REVIEWER_NOTE)
    last_edited_at    TIMESTAMP,
    locked_at         TIMESTAMP,
    version           BIGINT        NOT NULL DEFAULT 0,
    created_at        TIMESTAMP     NOT NULL,
    -- STATEMUTATION-MONOTONE-001 — a LOCKED form carries its terminal lock instant; a recorded edit
    -- basis is consistent (an edited-at implies an edited-field).
    CONSTRAINT chk_governed_form CHECK (
        (state <> 'LOCKED' OR locked_at IS NOT NULL)
        AND (last_edited_at IS NULL OR last_edited_field IS NOT NULL)
    )
);

-- STATEMUTATION-MONOTONE-001 — one immutable governed transition per (form, seq); the uq gives every
-- form a gap-free ordered trail an auditor replays. kind = FORWARD (tightening advance) | REOPEN
-- (recorded widening, reason mandatory). A REOPEN row is what makes a widening auditable.
CREATE TABLE form_transitions (
    id          UUID          NOT NULL PRIMARY KEY,
    form_id     UUID          NOT NULL REFERENCES governed_forms(id),
    seq         BIGINT        NOT NULL,
    from_state  VARCHAR(20)   NOT NULL,
    to_state    VARCHAR(20)   NOT NULL,
    kind        VARCHAR(20)   NOT NULL,              -- FORWARD | REOPEN
    reason      VARCHAR(1000),                       -- mandatory for a REOPEN
    actor       VARCHAR(200)  NOT NULL,
    occurred_at TIMESTAMP     NOT NULL
);

CREATE UNIQUE INDEX uq_form_transition_seq ON form_transitions (form_id, seq);
