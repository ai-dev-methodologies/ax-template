---
title: "Payment, notification, and email-outbox POST mutations must enforce a required Idempotency-Key request header, deduplicated via IdempotencyKeyStore"
rule_id: idempotency-key-on-mutations
impact: CRITICAL
impactDescription: "A network retry on a POST mutation without idempotency protection creates duplicate charges, duplicate notifications, or duplicate emails"
tags:
  - idempotency
  - payment
  - notification
  - email-outbox
  - retry-safety
provenance_class: internal_design
protects_template_id: backend/src/main/java/com/ax/template/authblueprint/payment/PaymentController.java
failing_fixture_path: practices/evals/fixtures/idempotency-key-on-mutations/fail_no_annotation/
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-VAL-001"
verification:
  type: review
  notes: "All @PostMapping handlers in payment, notification, and email-outbox controllers must declare @RequestHeader(value=\"Idempotency-Key\", required=false), reject a null/blank key with 400, and dedup the key via the shared common/IdempotencyKeyStore — the pattern PaymentController uses (PAYMENT-IDEMP-001)."
evidence:
  - source_type: external
    anchors: generic_principle_only
    citation: "IETF draft-ietf-httpapi-idempotency-key-header — The Idempotency-Key HTTP Header Field (deduplicated retry semantics)"
    url: "https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/"
    quoted_at: "2026-05-18"
  - source_type: external
    anchors: generic_principle_only
    citation: "Stripe API Reference — Idempotent requests: all POST requests accept an Idempotency-Key header to guarantee exactly-once delivery"
    url: "https://docs.stripe.com/api/idempotent_requests"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## Payment, notification, and email-outbox POST mutations must enforce a required `Idempotency-Key` header, deduplicated via `IdempotencyKeyStore`

**Impact: CRITICAL — Network retries without idempotency guards cause duplicate charges, duplicate SMS/push notifications, and duplicate email sends that are indistinguishable from the first request.**

The `api-idempotency-key-required.md` rule defines the general pattern (Idempotency-Key header + 400 on missing). This rule strengthens the enforcement for the three **high-risk mutation domains** — payment, notification, and email-outbox — by requiring a non-null `Idempotency-Key` header on every such handler (400 if missing) and routing the key through the shared `IdempotencyKeyStore`, so a retried POST returns the first response instead of repeating the side effect.

The store semantics (IdempotencyKeyStore):
1. The handler reads the `Idempotency-Key` header from the request.
2. On first call: processes normally, records `(key → serialised result)` in the store (Redis/DB).
3. On retry with the same key: returns the recorded response, skipping the side effect.
4. Missing/blank key: 400 `application/problem+json` with `type=urn:ax:idempotency:key-missing`.

**Incorrect — POST mutation handlers with no Idempotency-Key header check:**

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    // VIOLATION: a retried POST will create a second charge
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.ok(paymentService.charge(request));
    }

    // VIOLATION: notification send also missing
    @PostMapping("/notify")
    public ResponseEntity<Void> sendNotification(@RequestBody NotifyRequest req) {
        notificationService.send(req);
        return ResponseEntity.accepted().build();
    }
}
```

**Correct — required `Idempotency-Key` header + `IdempotencyKeyStore` dedup on every side-effecting POST:**

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    // CORRECT: the Idempotency-Key header is REQUIRED — a null/blank key is rejected with 400,
    // then the key is passed to the service, which dedups via the shared IdempotencyKeyStore
    // (a retry with the same key returns the cached result instead of charging again).
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreatePaymentRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank())
            throw new PaymentValidationException("Idempotency-Key header is required");
        return ResponseEntity.ok(paymentService.charge(idempotencyKey, request));
    }
}
```

## Why this matters

The payment, notification, and email-outbox templates are the three domains where retried mutations have user-visible, financially and operationally significant consequences:
- **Payment**: double-charge is a regulatory incident and a customer refund obligation.
- **Notification**: duplicate push/SMS sends degrade user trust.
- **Email-outbox**: duplicate transactional emails (password reset, OTP) may violate ESP rate limits and confuse users.

Unlike read endpoints or idempotent writes (PUT/PATCH), POST mutations in these domains have no natural key to deduplicate on — the `Idempotency-Key` header is the caller-supplied deduplication token.

The mechanism is the shared `common/IdempotencyKeyStore` (backend/src/main/java/com/ax/template/authblueprint/common/IdempotencyKeyStore.java): each side-effecting POST takes the `Idempotency-Key` header, rejects null/blank with 400, and records/looks up the key in the store so a retry returns the cached result. Enforcement across the three domains is review-tier (there is no `@RequireIdempotencyKey` annotation or `IdempotencyFilter` in this template — use the explicit header + store pattern shown above).

## Failing fixture

See: `practices/evals/fixtures/idempotency-key-on-mutations/fail_no_annotation/PaymentController.java` — two `@PostMapping` handlers in the payment controller with no `Idempotency-Key` header check. A network retry creates a second charge and a second notification.

Reference: [IETF draft — The Idempotency-Key HTTP Header Field](https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/)

Reference: [Stripe API Reference — Idempotent requests](https://docs.stripe.com/api/idempotent_requests)
