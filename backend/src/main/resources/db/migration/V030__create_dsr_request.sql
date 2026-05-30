-- V030__create_dsr_request.sql
-- Data-subject-rights request tracking schema (IMW6 / specs/data-subject-rights-l0.yaml).
--
-- Trace:
--   DSR-SLA-001 — {request_id (id), type, status, received_at, due_at, closed_at}
--                 tracked per request; due_at = received_at + 30 days, extendable
--                 by <= 60 further days (extension_days + extension_reason).
--   DSR-ACCESS-001 — ix_dsr_requests_subject_type_status backs the in-flight check.
--   The status column is mutated only via DsrRequestStateMachine; immutable identity
--   columns (subject_id, type, received_at) mirror @Column(updatable=false).
--
-- ddl-auto=create-drop regenerates the H2 test schema from the @Entity, so this
-- migration is NOT auto-exercised by the suite — it must match DsrRequest exactly.

CREATE TABLE IF NOT EXISTS dsr_requests (
    id               UUID         PRIMARY KEY,
    subject_id       VARCHAR(255) NOT NULL,
    type             VARCHAR(16)  NOT NULL,
    status           VARCHAR(16)  NOT NULL,
    received_at      TIMESTAMP    NOT NULL,
    due_at           TIMESTAMP    NOT NULL,
    closed_at        TIMESTAMP,
    extension_days   INTEGER      NOT NULL,
    extension_reason VARCHAR(512),
    sla_breached     BOOLEAN      NOT NULL,
    -- DSR-ERASURE-001 idempotency: completion manifest serialized once on erasure
    -- close; a re-request returns it verbatim (never re-runs provider erase()).
    erasure_manifest_json TEXT,
    version          BIGINT
);

CREATE INDEX IF NOT EXISTS ix_dsr_requests_subject
    ON dsr_requests(subject_id);
CREATE INDEX IF NOT EXISTS ix_dsr_requests_subject_type_status
    ON dsr_requests(subject_id, type, status);
CREATE INDEX IF NOT EXISTS ix_dsr_requests_status_due
    ON dsr_requests(status, due_at);
