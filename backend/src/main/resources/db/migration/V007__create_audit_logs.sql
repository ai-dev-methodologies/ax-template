-- V007__create_audit_logs.sql
-- Audit log domain schema (R14). Reference migration for production Postgres deployments.
--
-- NOTE: Flyway is not on the runtime classpath in this template — Hibernate
-- ddl-auto=create-drop manages the H2 reference workload. This file documents
-- the migration that a Flyway-enabled production deployment would execute.
-- The DDL is intentionally identical to what JPA derives from
-- com.ax.template.authblueprint.auditlog.AuditLog so future Flyway adoption is
-- a drop-in.
--
-- AUDIT-RECORD-002: audit_logs is append-only. All columns are NOT NULL where
-- the manifest declares them mandatory; @Column(updatable=false) on the entity
-- enforces immutability at the application layer. Production Postgres should
-- additionally install a row-level trigger that raises on UPDATE/DELETE, and
-- revoke UPDATE/DELETE privileges from the application role.

CREATE TABLE IF NOT EXISTS audit_logs (
    id              UUID PRIMARY KEY,
    actor_user_id   VARCHAR(255),
    actor_ip        VARCHAR(64),
    action          VARCHAR(64)  NOT NULL,
    resource_type   VARCHAR(128) NOT NULL,
    resource_id     VARCHAR(255) NOT NULL,
    outcome         VARCHAR(16)  NOT NULL,
    timestamp       TIMESTAMP    NOT NULL,
    correlation_id  VARCHAR(128),
    user_agent      VARCHAR(512),
    metadata_json   CLOB
);

CREATE INDEX IF NOT EXISTS ix_audit_logs_timestamp     ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS ix_audit_logs_actor         ON audit_logs(actor_user_id);
CREATE INDEX IF NOT EXISTS ix_audit_logs_resource      ON audit_logs(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS ix_audit_logs_action        ON audit_logs(action);

-- Export jobs (AUDIT-EXPORT-001): asynchronous job tracking.
CREATE TABLE IF NOT EXISTS audit_export_jobs (
    id            UUID PRIMARY KEY,
    requested_by  VARCHAR(255) NOT NULL,
    format        VARCHAR(8)   NOT NULL,
    status        VARCHAR(16)  NOT NULL,
    filter_json   CLOB,
    download_url  VARCHAR(1024),
    error_message VARCHAR(1024),
    record_count  BIGINT,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP
);
