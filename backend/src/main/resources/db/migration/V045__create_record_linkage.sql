-- record-linkage reference workload — realizes specs/record-linkage-l0.yaml
-- (P1-33~34: Fellegi-Sunter banded verdicts with the score / per-field breakdown / thresholds
-- RECORDED on the proposal; the REVIEW band decides only by explicit human confirm/reject;
-- a merge records per-field survivorship and TOMBSTONES the loser — never deletes it).

CREATE TABLE linkage_records (
    id             UUID         NOT NULL PRIMARY KEY,
    full_name      VARCHAR(200) NOT NULL,
    birth_date     DATE,
    identifier     VARCHAR(100),
    status         VARCHAR(20)  NOT NULL,          -- ACTIVE | MERGED (tombstone; no delete path)
    merged_into_id UUID,                            -- forward pointer to the survivor
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMP    NOT NULL,
    -- LINK-RESOLVE-001 — a tombstone always points forward.
    CONSTRAINT chk_linkage_tombstone CHECK (status <> 'MERGED' OR merged_into_id IS NOT NULL)
);

CREATE TABLE match_proposals (
    id              UUID          NOT NULL PRIMARY KEY,
    low_record_id   UUID          NOT NULL REFERENCES linkage_records(id),
    high_record_id  UUID          NOT NULL REFERENCES linkage_records(id),
    score           NUMERIC(5,4)  NOT NULL,
    breakdown_json  VARCHAR(1000) NOT NULL,         -- per-field contribution trail (LINK-BAND-001)
    lower_threshold NUMERIC(5,4)  NOT NULL,
    upper_threshold NUMERIC(5,4)  NOT NULL,
    band            VARCHAR(20)   NOT NULL,         -- AUTO_MATCH | REVIEW | NO_MATCH
    status          VARCHAR(20)   NOT NULL,         -- PROPOSED | CONFIRMED | REJECTED
    decided_by      VARCHAR(200),
    decided_at      TIMESTAMP,
    version         BIGINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMP     NOT NULL,
    -- LINK-BAND/REVIEW-001 — explained verdicts; a decided proposal records who/when.
    CONSTRAINT chk_match_proposal CHECK (
        score >= 0 AND score <= 1 AND lower_threshold < upper_threshold
        AND (status = 'PROPOSED' OR (decided_by IS NOT NULL AND decided_at IS NOT NULL))
    )
);

CREATE INDEX ix_match_proposal_records ON match_proposals (low_record_id, high_record_id);

-- LINK-SURVIVOR-001 — one immutable decision per (proposal, field).
CREATE TABLE survivorship_decisions (
    id               UUID         NOT NULL PRIMARY KEY,
    proposal_id      UUID         NOT NULL REFERENCES match_proposals(id),
    field_name       VARCHAR(50)  NOT NULL,
    winning_value    VARCHAR(500),
    source_record_id UUID,
    rule_applied     VARCHAR(100) NOT NULL,
    decided_at       TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX uq_survivorship_field ON survivorship_decisions (proposal_id, field_name);
