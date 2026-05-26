-- F4 (R60 dogfood iter1 LOW closure via R84) — add last_failure_at to email_outbox.
--
-- Background: R60 P1 (ops) finding F4 — DLQ rows have no transition timestamp.
-- Operators inferred staleness from retryCount + createdAt, which is
-- ambiguous (the row could have been created an hour ago and failed three
-- times in rapid succession, vs created a week ago and failed slowly).
-- The new column captures the wall-clock moment of the most recent
-- markFailure call. resetForRetry clears it back to NULL.
--
-- Backward compatibility: column is NULLABLE. Existing rows in any pre-R84
-- environment will have NULL until their next markFailure invocation; the
-- application code treats NULL as "no recorded failure" (which is also the
-- correct state for SENT and never-failed PENDING rows).

ALTER TABLE email_outbox
    ADD COLUMN last_failure_at TIMESTAMP NULL;
