-- V202605181203__create_billing_tables.sql
--
-- Creates tables for the billing domain (SP30):
--   billing_plans, subscriptions, billing_invoices, billing_events
--
-- All tables extend BaseEntity columns:
--   id, created_at, updated_at, version, deleted_at, created_by, last_modified_by
-- Soft-delete is handled by @SQLDelete (UPDATE SET deleted_at) + @Where(deleted_at IS NULL).
--
-- Once applied this file MUST NOT be edited — Flyway checksum validation will reject
-- any modification. New changes go in a new V{N+1}__... file.
--
-- Rules: soft-delete-only-on-base-entity.md (PRACTICES-PERS-005)
--        subscription-state-machine-explicit.md
--        no-billing-cross-import-from-payment.md

-- ─── billing_plans ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS billing_plans (
    -- BaseEntity-inherited columns
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    created_at          TIMESTAMP   NOT NULL,
    updated_at          TIMESTAMP   NOT NULL,
    version             BIGINT      NOT NULL DEFAULT 0,
    deleted_at          TIMESTAMP   NULL,
    created_by          VARCHAR(255) NULL,
    last_modified_by    VARCHAR(255) NULL,

    -- Plan-specific columns
    name                VARCHAR(255)    NOT NULL,
    description         TEXT            NULL,
    amount              BIGINT          NOT NULL,     -- integer minor units (KRW: won, USD: cents)
    currency            VARCHAR(3)      NOT NULL,     -- ISO 4217
    interval_days       INTEGER         NOT NULL,     -- 30=monthly, 365=annual
    trial_days          INTEGER         NOT NULL DEFAULT 0,
    active              BOOLEAN         NOT NULL DEFAULT TRUE
);

-- Plan features (ElementCollection)
CREATE TABLE IF NOT EXISTS billing_plan_features (
    plan_id     UUID        NOT NULL REFERENCES billing_plans(id),
    feature     TEXT        NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_billing_plans_active
    ON billing_plans (active) WHERE deleted_at IS NULL;

-- ─── subscriptions ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS subscriptions (
    -- BaseEntity-inherited columns
    id                      UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    created_at              TIMESTAMP   NOT NULL,
    updated_at              TIMESTAMP   NOT NULL,
    version                 BIGINT      NOT NULL DEFAULT 0,
    deleted_at              TIMESTAMP   NULL,
    created_by              VARCHAR(255) NULL,
    last_modified_by        VARCHAR(255) NULL,

    -- Subscription-specific columns
    user_id                 UUID            NOT NULL,
    plan_id                 UUID            NOT NULL REFERENCES billing_plans(id),
    status                  VARCHAR(16)     NOT NULL,   -- SubscriptionStatus enum: TRIAL, ACTIVE, PAST_DUE, CANCELLED
    currency                VARCHAR(3)      NOT NULL,   -- ISO 4217; frozen at creation
    current_period_start    DATE            NOT NULL,
    current_period_end      DATE            NOT NULL,
    trial_end               DATE            NULL,
    canceled_at             TIMESTAMP       NULL,
    provider_subscription_id VARCHAR(255)   NULL
);

-- Matches @Index on Subscription entity
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_status
    ON subscriptions (user_id, status) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_subscriptions_period_end
    ON subscriptions (current_period_end) WHERE deleted_at IS NULL;

-- ─── billing_invoices ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS billing_invoices (
    -- BaseEntity-inherited columns
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    created_at          TIMESTAMP   NOT NULL,
    updated_at          TIMESTAMP   NOT NULL,
    version             BIGINT      NOT NULL DEFAULT 0,
    deleted_at          TIMESTAMP   NULL,
    created_by          VARCHAR(255) NULL,
    last_modified_by    VARCHAR(255) NULL,

    -- Invoice-specific columns
    user_id             UUID            NOT NULL,
    subscription_id     UUID            NOT NULL REFERENCES subscriptions(id),
    amount_due          BIGINT          NOT NULL,   -- integer minor units
    amount_paid         BIGINT          NOT NULL DEFAULT 0,
    currency            VARCHAR(3)      NOT NULL,
    status              VARCHAR(16)     NOT NULL,   -- DRAFT, OPEN, PAID, VOID
    issued_at           TIMESTAMP       NULL,
    paid_at             TIMESTAMP       NULL,
    period_start        DATE            NOT NULL,
    period_end          DATE            NOT NULL,
    provider_invoice_id VARCHAR(255)    NULL
);

-- Matches @Index on Invoice entity
CREATE INDEX IF NOT EXISTS idx_billing_invoices_sub_status
    ON billing_invoices (subscription_id, status) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_billing_invoices_user
    ON billing_invoices (user_id) WHERE deleted_at IS NULL;

-- ─── billing_events ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS billing_events (
    -- BaseEntity-inherited columns
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    created_at          TIMESTAMP   NOT NULL,
    updated_at          TIMESTAMP   NOT NULL,
    version             BIGINT      NOT NULL DEFAULT 0,
    deleted_at          TIMESTAMP   NULL,
    created_by          VARCHAR(255) NULL,
    last_modified_by    VARCHAR(255) NULL,

    -- BillingEvent-specific columns
    subscription_id     UUID            NOT NULL,
    event_type          VARCHAR(32)     NOT NULL,   -- BillingEventType enum
    idempotency_key     VARCHAR(255)    NOT NULL,   -- UNIQUE per event
    provider_event_id   VARCHAR(255)    NULL,
    metadata            TEXT            NULL        -- JSON; never raw PAN
);

-- Matches @Index on BillingEvent entity
CREATE INDEX IF NOT EXISTS idx_billing_events_sub
    ON billing_events (subscription_id) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_billing_events_idempotency
    ON billing_events (idempotency_key);

CREATE INDEX IF NOT EXISTS idx_billing_events_occurred
    ON billing_events (created_at) WHERE deleted_at IS NULL;
