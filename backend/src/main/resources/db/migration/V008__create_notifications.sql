-- V008__create_notifications.sql
-- Notification domain schema (R15). Reference migration for production Postgres
-- deployments.
--
-- NOTE: Flyway is not on the runtime classpath in this template — Hibernate
-- ddl-auto=create-drop manages the H2 reference workload. This file documents
-- the migration that a Flyway-enabled production deployment would execute.
-- The DDL is intentionally identical to what JPA derives from
-- com.ax.template.authblueprint.notification.{Notification,NotificationPreferences}
-- so future Flyway adoption is a drop-in.
--
-- Trace:
--   NOTIF-SEND-001     — initial status defaults to UNREAD
--   NOTIF-LIST-001     — index on (recipient_user_id, created_at) for sorted lookups
--   NOTIF-LIST-002     — index on (recipient_user_id, status, deleted) for the UNREAD count + filter
--   NOTIF-AUTHZ-002    — strict owner lookup uses (id, recipient_user_id) — covered by primary key + index
--   NOTIF-DISMISS-001  — soft-delete (deleted=true) excludes rows from list/get
--   NOTIF-PREF-001/002 — preferences keyed by user_id (no surrogate key)

CREATE TABLE IF NOT EXISTS notifications (
    id                 UUID         PRIMARY KEY,
    recipient_user_id  VARCHAR(255) NOT NULL,
    type               VARCHAR(64)  NOT NULL,
    title              VARCHAR(255) NOT NULL,
    body               VARCHAR(2000),
    link               VARCHAR(1024),
    status             VARCHAR(16)  NOT NULL,
    deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_notifications_recipient_created
    ON notifications(recipient_user_id, created_at);
CREATE INDEX IF NOT EXISTS ix_notifications_recipient_status
    ON notifications(recipient_user_id, status, deleted);

CREATE TABLE IF NOT EXISTS notification_preferences (
    user_id         VARCHAR(255) PRIMARY KEY,
    in_app_enabled  BOOLEAN      NOT NULL DEFAULT TRUE,
    email_enabled   BOOLEAN      NOT NULL DEFAULT TRUE
);
