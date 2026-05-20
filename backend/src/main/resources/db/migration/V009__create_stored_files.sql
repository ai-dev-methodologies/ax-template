-- V009__create_stored_files.sql
-- File-storage domain schema (R16). Reference migration for production Postgres
-- deployments.
--
-- NOTE: Flyway is not on the runtime classpath in this template — Hibernate
-- ddl-auto=create-drop manages the H2 reference workload. This file documents
-- the migration that a Flyway-enabled production deployment would execute.
-- The DDL is intentionally identical to what JPA derives from
-- com.ax.template.authblueprint.filestorage.StoredFile so future Flyway
-- adoption is a drop-in.
--
-- Trace:
--   FILE-AUTHZ-002 — strict owner lookup uses (id, owner_user_id) — covered by primary key + ix_stored_files_owner_status
--   FILE-UPLOAD-003 — file_name is the SANITIZED display name; storage_key is the opaque server UUID
--   FILE-SCAN-001  — status transitions PENDING → READY | QUARANTINED via ix_stored_files_owner_status
--   FILE-QUOTA-001 — sumByOwnerForCountedStatuses() reads from ix_stored_files_owner_status
--   FILE-SEC-001 / FILE-SEC-002 — storage_key is NEVER selected into the API DTO

CREATE TABLE IF NOT EXISTS stored_files (
    id              UUID         PRIMARY KEY,
    owner_user_id   VARCHAR(255) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    content_type    VARCHAR(255) NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    sha256          VARCHAR(64)  NOT NULL,
    storage_key     VARCHAR(512) NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    uploaded_at     TIMESTAMP    NOT NULL,
    scanned_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_stored_files_owner_status
    ON stored_files(owner_user_id, status, deleted);

CREATE INDEX IF NOT EXISTS ix_stored_files_sha256_owner
    ON stored_files(sha256, owner_user_id);
