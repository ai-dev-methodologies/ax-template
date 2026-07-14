-- provisional-attestation reference workload — realizes specs/provisional-attestation-l0.yaml
-- (backlog wave 2026-07-14, P3-34): a 2-state PROVISIONAL -> ATTESTED co-signature record. The
-- DB @Check (attested_by <> authored_by) is the PATT-DISTINCT-002 backstop against even a
-- direct-SQL self-attestation; attested_content_hash is the PATT-FREEZE-003 tamper-evidence pin.

CREATE TABLE provisional_records (
    id                     UUID          NOT NULL PRIMARY KEY,
    authored_by            VARCHAR(200)  NOT NULL,
    content                VARCHAR(4000) NOT NULL,
    status                 VARCHAR(20)   NOT NULL,
    attested_by            VARCHAR(200),
    attested_at            TIMESTAMP,
    attested_content_hash  VARCHAR(64),
    created_at             TIMESTAMP     NOT NULL,
    CONSTRAINT chk_provisional_distinct_attestor CHECK (
        attested_by IS NULL OR attested_by <> authored_by
    )
);
