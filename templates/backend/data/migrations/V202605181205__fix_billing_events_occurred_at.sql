-- V202605181205__fix_billing_events_occurred_at.sql
--
-- Adds the BillingEvent.occurredAt column missed in V202605181203.
-- Also corrects idx_billing_events_occurred to index occurred_at instead of created_at.
--
-- BillingEvent entity: @Column(name = "occurred_at", nullable = false)
--                      @Index(name = "idx_billing_events_occurred", columnList = "occurred_at")

ALTER TABLE billing_events ADD COLUMN occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE billing_events ALTER COLUMN occurred_at DROP DEFAULT;
DROP INDEX IF EXISTS idx_billing_events_occurred;
CREATE INDEX idx_billing_events_occurred ON billing_events (occurred_at);
