-- V020__create_session_records.sql
-- Session-management domain schema (R33).
--
-- Trace:
--   SESS-LIFECYCLE-001 — UNIQUE(user_id, jti) backs idempotent register
--   SESS-LIFECYCLE-003 — status flips ACTIVE → REVOKED; no hard delete (audit trail)
--   SESS-REVOKE-003   — ix_session_records_jti supports SPI fast lookup

CREATE TABLE IF NOT EXISTS session_records (
    id                   UUID         PRIMARY KEY,
    user_id              VARCHAR(255) NOT NULL,
    jti                  VARCHAR(128) NOT NULL,
    device_label         VARCHAR(64),
    ip_address           VARCHAR(64),
    user_agent           VARCHAR(512),
    status               VARCHAR(16)  NOT NULL,
    created_at           TIMESTAMP    NOT NULL,
    expires_at           TIMESTAMP    NOT NULL,
    last_seen_at         TIMESTAMP,
    revoked_at           TIMESTAMP,
    revoked_by_user_id   VARCHAR(255),
    CONSTRAINT uq_session_records_user_jti UNIQUE (user_id, jti)
);

CREATE INDEX IF NOT EXISTS ix_session_records_user_created
    ON session_records(user_id, created_at);
CREATE INDEX IF NOT EXISTS ix_session_records_jti
    ON session_records(jti);
CREATE INDEX IF NOT EXISTS ix_session_records_status
    ON session_records(status);
