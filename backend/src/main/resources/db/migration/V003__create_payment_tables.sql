-- V003__create_payment_tables.sql
-- Payment blueprint schema. Reference migration for production Postgres deployments.
--
-- NOTE: Flyway is not on the runtime classpath in this template — Hibernate
-- ddl-auto=create-drop manages the H2 reference workload. This file documents
-- the migration that a Flyway-enabled production deployment would execute.
-- The DDL is intentionally identical to what JPA derives from the @Entity
-- annotations in com.ax.template.authblueprint.payment.* so a future Flyway
-- adoption is a drop-in.
--
-- PAYMENT-RECON-001: payment_events is append-only. The trigger at the bottom
-- enforces this at the DB layer in production; H2 deployments install an
-- equivalent Java trigger via PaymentConfig.installImmutabilityGuard().

CREATE TABLE IF NOT EXISTS payments (
    id                        UUID PRIMARY KEY,
    order_id                  VARCHAR(255) NOT NULL,
    user_id                   UUID NOT NULL,
    amount                    NUMERIC(19, 8) NOT NULL,
    captured_amount           NUMERIC(19, 8),
    balance                   NUMERIC(19, 8),
    currency                  CHAR(3) NOT NULL,
    state                     VARCHAR(32) NOT NULL,
    payment_method_token      VARCHAR(512),
    idempotency_key           VARCHAR(255),
    decline_reason            VARCHAR(128),
    captured_at               TIMESTAMP,
    created_at                TIMESTAMP NOT NULL,
    updated_at                TIMESTAMP,
    version                   BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_payments_user_id ON payments(user_id);
CREATE INDEX IF NOT EXISTS ix_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS ix_payments_idempotency_key ON payments(idempotency_key);

CREATE TABLE IF NOT EXISTS refunds (
    id              UUID PRIMARY KEY,
    payment_id      UUID NOT NULL,
    amount          NUMERIC(19, 8) NOT NULL,
    currency        CHAR(3) NOT NULL,
    reason          VARCHAR(500),
    state           VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(255),
    created_at      TIMESTAMP NOT NULL,
    version         BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_refunds_payment_id ON refunds(payment_id);
CREATE INDEX IF NOT EXISTS ix_refunds_idempotency_key ON refunds(idempotency_key);

-- payment_events is append-only. Production Postgres uses a trigger function
-- raise_immutable() that always raises EXCEPTION 'payment_events is append-only'.
CREATE TABLE IF NOT EXISTS payment_events (
    event_id       UUID PRIMARY KEY,
    payment_id     UUID NOT NULL,
    type           VARCHAR(64) NOT NULL,
    occurred_at    TIMESTAMP NOT NULL,
    payload_hash   CHAR(64) NOT NULL,
    prev_hash      CHAR(64),
    payload        TEXT NOT NULL,
    amount_numeric NUMERIC(19, 8)
);

CREATE INDEX IF NOT EXISTS ix_payment_events_payment_id ON payment_events(payment_id);
CREATE INDEX IF NOT EXISTS ix_payment_events_occurred_at ON payment_events(occurred_at);

-- Production Postgres equivalent (commented; uncomment when adopting Flyway):
-- CREATE OR REPLACE FUNCTION raise_immutable() RETURNS TRIGGER AS $$
-- BEGIN
--     RAISE EXCEPTION 'payment_events is append-only; UPDATE/DELETE is blocked (PAYMENT-RECON-001)';
-- END;
-- $$ LANGUAGE plpgsql;
--
-- CREATE TRIGGER payment_events_no_update_stmt
--     BEFORE UPDATE OR DELETE ON payment_events
--     FOR EACH STATEMENT EXECUTE FUNCTION raise_immutable();
