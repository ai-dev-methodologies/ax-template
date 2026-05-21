-- V016__create_export_jobs.sql
-- Report-export domain schema (R29).
--
-- NOTE: Flyway is not on the runtime classpath in this template — Hibernate
-- ddl-auto=create-drop manages the H2 reference workload. This file documents
-- the migration that a Flyway-enabled production deployment would execute.
-- The DDL is intentionally aligned with what JPA derives from
--   com.ax.template.authblueprint.reportexport.ExportJob
-- so future Flyway adoption is a drop-in.
--
-- Trace:
--   EXPORT-AUTHZ-002    — every lookup filters on (id, owner_user_id)
--   EXPORT-LIFECYCLE-001 — rows are inserted with status='PENDING'
--   EXPORT-LIFECYCLE-002 — status mirrored to API consumers
--   EXPORT-LIFECYCLE-004 — status mutated only via ExportJobStateMachine

CREATE TABLE IF NOT EXISTS export_jobs (
    id              UUID         PRIMARY KEY,
    owner_user_id   VARCHAR(255) NOT NULL,
    format          VARCHAR(16)  NOT NULL,
    name            VARCHAR(128),
    status          VARCHAR(16)  NOT NULL,
    row_count       BIGINT,
    size_bytes      BIGINT,
    error_message   VARCHAR(1024),
    query_json      VARCHAR(4096),
    created_at      TIMESTAMP    NOT NULL,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    payload         BYTEA
);

CREATE INDEX IF NOT EXISTS ix_export_jobs_owner_status
    ON export_jobs(owner_user_id, status);

CREATE INDEX IF NOT EXISTS ix_export_jobs_status_created
    ON export_jobs(status, created_at);
