-- P1-70 refund idempotency — DB backstop for exactly-once refund semantics.
--
-- PAYMENT-IDEMP-004: a retried POST /api/payments/{id}/refund carrying the same
-- Idempotency-Key MUST replay the original refund row (HTTP 200) instead of creating a
-- second refund. RefundService performs the (payment_id, idempotency_key) lookup and
-- short-circuits; this unique index is the DB-level backstop for the case where two
-- concurrent requests both miss that lookup.
--
-- The PRIMARY concurrency mechanism is the optimistic lock (@Version) on the parent
-- Payment row: two concurrent same-key refunds both mutate the payment, so the loser
-- gets an OptimisticLockException → 409 urn:ax:payment:concurrent-modification, and the
-- client's retry then hits the replay path. This index closes the residual window.
--
-- NULL idempotency keys stay multiply-allowed (SQL treats NULLs as distinct in a unique
-- index), preserving any historical row written before the header became mandatory.
CREATE UNIQUE INDEX IF NOT EXISTS ux_refunds_payment_id_idempotency_key
    ON refunds(payment_id, idempotency_key);
