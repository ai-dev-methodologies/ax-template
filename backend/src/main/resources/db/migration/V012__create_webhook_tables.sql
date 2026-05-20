-- V012__create_webhook_tables.sql
-- Webhook domain schema (R19). Reference migration for production Postgres.
--
-- NOTE: Flyway is not on the runtime classpath in this template — Hibernate
-- ddl-auto=create-drop manages the H2 reference workload. This file documents
-- the migration that a Flyway-enabled production deployment would execute.
-- The DDL is intentionally aligned with what JPA derives from
--   com.ax.template.authblueprint.webhook.WebhookEndpoint
--   com.ax.template.authblueprint.webhook.WebhookDelivery
-- so future Flyway adoption is a drop-in.
--
-- Trace:
--   WEBHOOK-EMIT-001       — webhook_endpoints INSERT with active=true + UUID + 256-bit signing_secret
--   WEBHOOK-EMIT-002       — webhook_deliveries INSERT (one row per matching active endpoint) BEFORE outbound POST
--   WEBHOOK-RETRY-001/002  — attempt_count + next_attempt_at + stable PK (delivery_id header)
--   WEBHOOK-DEAD-LETTER-001 — status=FAILED_PERMANENT retained for 30 days
--   WEBHOOK-DEAD-LETTER-002 — admin replay creates a NEW row (fresh PK)
--   WEBHOOK-CIRCUIT-001    — rolling 50-row window query on webhook_deliveries.endpoint_id

CREATE TABLE IF NOT EXISTS webhook_endpoints (
    id              UUID         PRIMARY KEY,
    url             VARCHAR(1024) NOT NULL,
    active          BOOLEAN      NOT NULL,
    signing_secret  VARCHAR(128) NOT NULL,
    event_filter    VARCHAR(255),
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS ix_webhook_endpoints_url
    ON webhook_endpoints(url);

CREATE INDEX IF NOT EXISTS ix_webhook_endpoints_active
    ON webhook_endpoints(active);

CREATE TABLE IF NOT EXISTS webhook_deliveries (
    id                 UUID         PRIMARY KEY,
    endpoint_id        UUID         NOT NULL,
    event_type         VARCHAR(128) NOT NULL,
    body               TEXT         NOT NULL,
    status             VARCHAR(32)  NOT NULL,
    attempt_count      INT          NOT NULL DEFAULT 0,
    next_attempt_at    TIMESTAMP,
    last_response_code INT,
    last_attempt_at    TIMESTAMP,
    last_error         VARCHAR(1024),
    created_at         TIMESTAMP    NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS ix_webhook_deliveries_endpoint
    ON webhook_deliveries(endpoint_id);

CREATE INDEX IF NOT EXISTS ix_webhook_deliveries_status
    ON webhook_deliveries(status);

CREATE INDEX IF NOT EXISTS ix_webhook_deliveries_next_attempt_at
    ON webhook_deliveries(next_attempt_at);

-- Retention (advisory):
--   DELETE FROM webhook_deliveries
--       WHERE status IN ('SUCCEEDED', 'FAILED_PERMANENT')
--         AND created_at < NOW() - INTERVAL '30 days';
--   See blueprints/webhook-manifest.yaml#retention.webhook_delivery_days=30.
