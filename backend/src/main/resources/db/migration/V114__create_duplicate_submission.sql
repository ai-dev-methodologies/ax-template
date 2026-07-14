-- duplicate-submission-key-l0 reference workload — realizes specs/duplicate-submission-key-l0.yaml
-- (BACKLOG P3-19: intake-time duplicate-submission gate. An exact natural same-loss key match
-- against an ACTIVE submission is a 409, backstopped by a UNIQUE(channel_id, active_key)
-- constraint; a near (fuzzy-window) match is accepted but flagged for review; withdrawing or
-- rejecting a submission releases its key by clearing active_key to NULL, which SQL's standard
-- multiple-NULLs-allowed uniqueness semantics excludes from the constraint — no partial/filtered
-- index syntax required, portable across H2/PostgreSQL).

CREATE TABLE duplicate_key_channels (
    id                UUID         NOT NULL PRIMARY KEY,
    scope_label       VARCHAR(200) NOT NULL,
    fuzzy_window_days INT          NOT NULL,
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL,
    CONSTRAINT chk_duplicate_key_channel CHECK (fuzzy_window_days >= 0)
);

-- DUPKEY-NATURAL-001 — active_key mirrors natural_key while ACTIVE; NULL once WITHDRAWN/REJECTED
-- (DUPKEY-WITHDRAWN-003). The UNIQUE constraint below is the authoritative concurrent-race backstop.
CREATE TABLE duplicate_submissions (
    id                    UUID         NOT NULL PRIMARY KEY,
    channel_id            UUID         NOT NULL REFERENCES duplicate_key_channels(id),
    subject_ref           VARCHAR(200) NOT NULL,
    loss_date             DATE         NOT NULL,
    loss_type             VARCHAR(100) NOT NULL,
    natural_key           VARCHAR(320) NOT NULL,          -- immutable — subjectRef|lossDate|lossType
    active_key            VARCHAR(320),                   -- mirrors natural_key while ACTIVE; NULL otherwise
    status                VARCHAR(20)  NOT NULL,           -- ACTIVE | WITHDRAWN | REJECTED
    flagged_for_review    BOOLEAN      NOT NULL,           -- DUPKEY-FUZZY-002
    suspect_submission_id UUID,
    version               BIGINT       NOT NULL DEFAULT 0,
    created_at            TIMESTAMP    NOT NULL,
    CONSTRAINT chk_duplicate_submission_active_key CHECK (
        (status = 'ACTIVE') = (active_key IS NOT NULL)
    ),
    CONSTRAINT uq_duplicate_submission_active_key UNIQUE (channel_id, active_key)
);

CREATE INDEX idx_duplicate_submissions_channel ON duplicate_submissions (channel_id, subject_ref, loss_type, loss_date);
