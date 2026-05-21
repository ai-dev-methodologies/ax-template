-- V017__create_api_keys.sql
-- API-key domain schema (R30).
--
-- NOTE: Flyway is not on the runtime classpath in this template — Hibernate
-- ddl-auto=create-drop manages the H2 reference workload. This file documents
-- the migration that a Flyway-enabled production deployment would execute.
--
-- Trace:
--   KEY-AUTHZ-002  — every lookup filters on (id, owner_user_id)
--   KEY-STORAGE-001 — hashed_value is the SHA-256 hex digest (64 chars)
--   KEY-STORAGE-003 — hashed_value is NOT NULL and not updated after insert
--   KEY-LIFECYCLE-001/002 — status transitions ACTIVE → REVOKED only
--   KEY-LIFECYCLE-003 — expires_at checked at every authentication; no sweep job

CREATE TABLE IF NOT EXISTS api_keys (
    id              UUID         PRIMARY KEY,
    owner_user_id   VARCHAR(255) NOT NULL,
    name            VARCHAR(128),
    hash_prefix     VARCHAR(16)  NOT NULL,
    hashed_value    VARCHAR(64)  NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    expires_at      TIMESTAMP,
    revoked_at      TIMESTAMP,
    last_used_at    TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_api_keys_owner_status
    ON api_keys(owner_user_id, status);

CREATE INDEX IF NOT EXISTS ix_api_keys_hash_prefix
    ON api_keys(hash_prefix);

CREATE TABLE IF NOT EXISTS api_key_scopes (
    api_key_id      UUID         NOT NULL,
    scope           VARCHAR(32)  NOT NULL,
    PRIMARY KEY (api_key_id, scope),
    CONSTRAINT fk_api_key_scopes_api_key
        FOREIGN KEY (api_key_id) REFERENCES api_keys(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_api_key_scopes_key
    ON api_key_scopes(api_key_id);
