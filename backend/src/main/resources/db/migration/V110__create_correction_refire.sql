-- correction-refire reference workload — realizes specs/correction-refire-l0.yaml (backlog wave
-- 2026-07-14, P3-35): an append-only versioned publish chain per subject (CRF-SUPERSEDE-001,
-- uq(subject_ref, version) backstop) where a correction over a version whose ack was CLOSED
-- re-opens the loop via a brand-new PENDING ack row (CRF-REFIRE-002), content-hash idempotency
-- prevents ack-spam on a no-op re-publish (CRF-IDEMPOTENT-003), and NO column anywhere stores a
-- "current version" pointer — current is always MAX(version) derived on read (CRF-CHAIN-004).

CREATE TABLE corrected_records (
    id               UUID          NOT NULL PRIMARY KEY,
    subject_ref      VARCHAR(200)  NOT NULL,
    version          INTEGER       NOT NULL,
    content          VARCHAR(4000) NOT NULL,
    content_hash     VARCHAR(64)   NOT NULL,
    corrects_version INTEGER,
    published_at     TIMESTAMP     NOT NULL
);

CREATE UNIQUE INDEX uq_corrected_record_subject_version ON corrected_records (subject_ref, version);

-- one PENDING/CLOSED ack row per published version — CRF-CHAIN-004 independence.
CREATE TABLE correction_ack_records (
    id         UUID        NOT NULL PRIMARY KEY,
    record_id  UUID        NOT NULL REFERENCES corrected_records(id),
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    closed_at  TIMESTAMP
);

CREATE UNIQUE INDEX uq_ack_record_id ON correction_ack_records (record_id);
