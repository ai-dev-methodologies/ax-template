-- V202605181200__add_soft_delete_columns.sql
--
-- Adds deleted_at TIMESTAMP NULL to all 8 BaseEntity-derived tables.
-- Once applied this file MUST NOT be edited — Flyway checksum validation will reject
-- any modification. New soft-delete changes go in a new V{N+1}__... file.
--
-- Each entity's @SQLDelete sets deleted_at = CURRENT_TIMESTAMP on ORM-triggered deletes.
-- Each partial index (WHERE deleted_at IS NULL) keeps active-row lookups on the fast path.
--
-- Rules: soft-delete-only-on-base-entity.md (PRACTICES-PERS-005)

-- ── notifications ────────────────────────────────────────────────────────────────
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_notifications_not_deleted
    ON notifications (id) WHERE deleted_at IS NULL;

-- ── notification_preferences ──────────────────────────────────────────────────────
ALTER TABLE notification_preferences
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_notification_preferences_not_deleted
    ON notification_preferences (id) WHERE deleted_at IS NULL;

-- ── audit_logs ────────────────────────────────────────────────────────────────────
ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_audit_logs_not_deleted
    ON audit_logs (id) WHERE deleted_at IS NULL;

-- ── stored_files ──────────────────────────────────────────────────────────────────
ALTER TABLE stored_files
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_stored_files_not_deleted
    ON stored_files (id) WHERE deleted_at IS NULL;

-- ── email_outbox ──────────────────────────────────────────────────────────────────
ALTER TABLE email_outbox
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_email_outbox_not_deleted
    ON email_outbox (id) WHERE deleted_at IS NULL;

-- ── email_templates ───────────────────────────────────────────────────────────────
ALTER TABLE email_templates
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_email_templates_not_deleted
    ON email_templates (id) WHERE deleted_at IS NULL;

-- ── scheduled_tasks ───────────────────────────────────────────────────────────────
ALTER TABLE scheduled_tasks
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_scheduled_tasks_not_deleted
    ON scheduled_tasks (id) WHERE deleted_at IS NULL;

-- ── job_history ───────────────────────────────────────────────────────────────────
ALTER TABLE job_history
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_job_history_not_deleted
    ON job_history (id) WHERE deleted_at IS NULL;
