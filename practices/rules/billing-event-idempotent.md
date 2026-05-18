---
title: "All BillingEvent writes must carry a unique idempotencyKey; duplicate provider events must be rejected without creating a second row"
rule_id: billing-event-idempotent
impact: CRITICAL
impactDescription: "Duplicate webhook delivery (common from Stripe/Toss under network instability) creates duplicate BillingEvents, double-transitions subscription state, and emits double counter increments"
tags:
  - billing
  - idempotency
  - webhook
  - event-sourcing
provenance_class: internal_design
protects_template_id: templates/backend/billing/BillingEvent.java
failing_fixture_path: practices/evals/fixtures/billing-event-idempotent/fail_no_idempotency_key/
spec_ref: "specs/billing-l0.yaml#BILLING-IDEMP-001"
verification:
  type: review
  notes: |
    Check: every BillingEvent.createInternal() or BillingEvent.fromWebhook() call
    supplies a non-null, non-empty idempotencyKey.
    DB: billing_events.idempotency_key has UNIQUE constraint.
    WebhookBillingReceiver catches duplicate-key exceptions and returns 200 (not 5xx).
evidence:
  - source_type: upstream_id
    upstream_id: stripe-billing-2026-05
    section: "Idempotency"
    quote: "Stripe stores results for at least 24 hours. Retrying the same key within the window returns the original response without creating a duplicate resource."
  - source_type: upstream_id
    upstream_id: toss-billing-2026-05
    section: "멱등성"
    quote: "Idempotency-Key 헤더를 사용하면 네트워크 오류로 인한 재시도 시 중복 결제를 방지할 수 있습니다."
  - source_type: external
    citation: "IETF draft — The Idempotency-Key HTTP Header Field (exactly-once semantics)"
    url: "https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## BillingEvent writes must carry a unique idempotencyKey

**Impact: CRITICAL — Duplicate webhook delivery (normal under provider SLA) creates duplicate BillingEvents that cause double subscription state transitions, double counter increments, and incorrect invoice generation.**

Both Stripe and Toss Payments guarantee **at-least-once** delivery of webhook events. Under network instability, the same event may arrive 2–3 times within a few seconds. Without idempotency protection, each delivery creates a new BillingEvent row, triggering a second state transition (e.g., ACTIVE → PAST_DUE twice) and emitting billing observability counters twice.

### Enforcement

1. **DB UNIQUE constraint** on `billing_events.idempotency_key` (see `BillingEvent.java`).
2. **Factory constructors** `BillingEvent.createInternal()` and `BillingEvent.fromWebhook()` require non-null `idempotencyKey`.
3. **WebhookBillingReceiver** catches `DataIntegrityViolationException` from duplicate-key inserts and returns HTTP 200 without re-processing.
4. **Observability**: `billing.event.idempotency_hit_count` counter increments on every detected duplicate.

**Incorrect — BillingEvent without idempotencyKey:**

```java
// VIOLATION: no idempotencyKey → duplicate webhook creates second row → double state transition
BillingEvent event = new BillingEvent();
event.setSubscriptionId(sub.getId());
event.setEventType(PAYMENT_SUCCEEDED);
// idempotencyKey not set → null constraint violation or silent duplicate
billingEventRepository.save(event);
```

**Correct — BillingEvent with idempotencyKey from provider event ID:**

```java
// CORRECT: fromWebhook() sets idempotencyKey from provider event ID
BillingEvent event = BillingEvent.fromWebhook(
    sub.getId(),
    BillingEventType.PAYMENT_SUCCEEDED,
    providerWebhookEvent.getId(),   // idempotencyKey = stripe evt_xxx or toss payment_xxx
    providerWebhookEvent.getId(),
    providerWebhookEvent.getTimestamp(),
    metadataJson
);
billingEventRepository.save(event);
// Duplicate webhook with same providerEventId → DataIntegrityViolationException → return 200
```

Reference: https://stripe.com/docs/api/idempotent_requests

## Failing fixture

See: `practices/evals/fixtures/billing-event-idempotent/fail_no_idempotency_key/` — BillingEvent created via a constructor that leaves `idempotencyKey` null. ArchUnit or static analysis flags the missing field.

See: `practices/evals/fixtures/billing-event-idempotent/pass_idempotency_key_set/` — correct usage.
