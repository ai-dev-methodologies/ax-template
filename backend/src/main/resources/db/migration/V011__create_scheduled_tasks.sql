-- V011__create_scheduled_tasks.sql
-- Scheduled-task domain schema (R18). Reference migration for production Postgres.
--
-- NOTE: Flyway is not on the runtime classpath in this template — Hibernate
-- ddl-auto=create-drop manages the H2 reference workload. This file documents
-- the migration that a Flyway-enabled production deployment would execute.
-- The DDL is intentionally aligned with what JPA derives from
--   com.ax.template.authblueprint.scheduledtask.ScheduledTask
--   com.ax.template.authblueprint.scheduledtask.JobHistory
--   com.ax.template.authblueprint.scheduledtask.TaskLock
-- so future Flyway adoption is a drop-in.
--
-- Trace:
--   SCHED-REGISTER-001 — scheduled_tasks INSERT with status=REGISTERED + UUID
--   SCHED-LOCK-001     — task_locks single-row-per-name pessimistic acquire
--   SCHED-LOCK-002     — task_locks.locked_at + ttl < now ⇒ stale, takeover allowed
--   SCHED-EXECUTE-001  — job_history append-only audit row per cycle
--   SCHED-IDEMPOTENT-001 — @Version (version column) blocks duplicate takeovers

CREATE TABLE IF NOT EXISTS scheduled_tasks (
    id              UUID         PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    cron_expression VARCHAR(128) NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    handler_bean    VARCHAR(255),
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    last_run_at     TIMESTAMP,
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS ix_scheduled_tasks_name
    ON scheduled_tasks(name);

CREATE INDEX IF NOT EXISTS ix_scheduled_tasks_status
    ON scheduled_tasks(status);

CREATE TABLE IF NOT EXISTS job_history (
    id            UUID         PRIMARY KEY,
    task_id       UUID         NOT NULL,
    task_name     VARCHAR(128) NOT NULL,
    started_at    TIMESTAMP    NOT NULL,
    finished_at   TIMESTAMP,
    outcome       VARCHAR(16)  NOT NULL,
    error_message VARCHAR(2000),
    host_instance VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS ix_job_history_task_name
    ON job_history(task_name);

CREATE INDEX IF NOT EXISTS ix_job_history_started_at
    ON job_history(started_at);

CREATE TABLE IF NOT EXISTS task_locks (
    task_name   VARCHAR(128) PRIMARY KEY,
    lock_holder VARCHAR(255) NOT NULL,
    locked_at   TIMESTAMP    NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0
);

-- Production opt-in (Postgres advisory lock):
--   The DB-row pattern above is portable across H2 + Postgres + MySQL.
--   For lower-latency deployments, swap LockingPolicy to a Redis Redlock
--   or Postgres pg_advisory_lock implementation — no schema migration required;
--   task_locks then becomes optional.
--
-- Retention (advisory):
--   DELETE FROM job_history WHERE started_at < NOW() - INTERVAL '90 days';
--   See blueprints/scheduled-task-manifest.yaml#retention.job_history_days=90.
