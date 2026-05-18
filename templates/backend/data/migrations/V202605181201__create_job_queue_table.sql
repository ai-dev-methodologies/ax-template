-- V202605181201__create_job_queue_table.sql
--
-- Creates the job_queue table for the JobQueue entity (templates/backend/jobs/JobQueue.java).
-- JobQueue extends BaseEntity, inheriting: id, created_at, updated_at, version, deleted_at.
-- Soft-delete is handled by @SQLDelete (UPDATE SET deleted_at) + @Where(deleted_at IS NULL).
--
-- Once applied this file MUST NOT be edited — Flyway checksum validation will reject
-- any modification. New changes go in a new V{N+1}__... file.
--
-- Rules: soft-delete-only-on-base-entity.md (PRACTICES-PERS-005)
--        webhook-hmac-required / transactional-outbox-pattern (PRACTICES-INTEG-001)

CREATE TABLE IF NOT EXISTS job_queue (
    -- BaseEntity-inherited columns
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0,
    deleted_at  TIMESTAMP   NULL,

    -- JobQueue-specific columns
    job_type        VARCHAR(128)    NOT NULL,
    payload         JSONB           NOT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    attempt_count   INTEGER         NOT NULL DEFAULT 0,
    last_attempted_at TIMESTAMP     NULL,
    error_message   TEXT            NULL
);

-- Partial index: active-row lookups (WHERE deleted_at IS NULL) on the fast path
-- Matches the pattern used in V202605181200__add_soft_delete_columns.sql
CREATE INDEX IF NOT EXISTS idx_job_queue_deleted_at
    ON job_queue (id) WHERE deleted_at IS NULL;

-- Dispatcher query: find PENDING jobs ordered by creation time
CREATE INDEX IF NOT EXISTS idx_job_queue_status_created
    ON job_queue (status, created_at) WHERE deleted_at IS NULL;

-- Worker query: find jobs by type and status (e.g. retry FAILED jobs of a specific type)
CREATE INDEX IF NOT EXISTS idx_job_queue_type_status
    ON job_queue (job_type, status) WHERE deleted_at IS NULL;
