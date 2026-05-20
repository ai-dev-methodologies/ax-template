-- R21 billing domain (specs/billing-l0.yaml, blueprints/billing-manifest.yaml)
--
-- BILLING-AUTHZ-001/002/003 — subscription/plan/admin endpoints persisted
-- BILLING-IDEMP-001         — billing_events idempotent on (provider_event_id) unique constraint
-- BILLING-IDEMP-002         — webhook replay tolerance enforced at controller (300s); DB stores received_at
-- BILLING-STATE-001/002     — subscription.status mutated only via SubscriptionStateMachine; lifecycle transitions audited via billing_events row
-- BILLING-CUR-001           — monetary amount stored as BIGINT (integer minor units); float inputs rejected at JSON deser layer
-- BILLING-BOUNDARY-001      — billing package owns these tables; payment package owns payments + refunds. No FK between domains.

CREATE TABLE plans (
    id              VARCHAR(36)  PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    amount          BIGINT       NOT NULL,
    currency        VARCHAR(3)   NOT NULL,
    billing_cycle   VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    deleted_at      TIMESTAMP
);

CREATE INDEX idx_plans_currency ON plans(currency);

CREATE TABLE subscriptions (
    id                    VARCHAR(36)  PRIMARY KEY,
    user_id               VARCHAR(36)  NOT NULL,
    plan_id               VARCHAR(36)  NOT NULL,
    status                VARCHAR(16)  NOT NULL,
    provider              VARCHAR(32)  NOT NULL,
    amount                BIGINT       NOT NULL,
    currency              VARCHAR(3)   NOT NULL,
    started_at            TIMESTAMP    NOT NULL,
    current_period_end    TIMESTAMP,
    created_at            TIMESTAMP    NOT NULL,
    updated_at            TIMESTAMP    NOT NULL,
    deleted_at            TIMESTAMP
);

CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_status  ON subscriptions(status);

CREATE TABLE billing_events (
    id                    VARCHAR(36)  PRIMARY KEY,
    provider_event_id     VARCHAR(120) NOT NULL,
    subscription_id       VARCHAR(36),
    event_type            VARCHAR(40)  NOT NULL,
    provider              VARCHAR(32)  NOT NULL,
    payload               VARCHAR(4000),
    received_at           TIMESTAMP    NOT NULL,
    CONSTRAINT uk_billing_events_provider_event_id UNIQUE (provider_event_id)
);

CREATE INDEX idx_billing_events_sub_id ON billing_events(subscription_id);
CREATE INDEX idx_billing_events_type   ON billing_events(event_type);
