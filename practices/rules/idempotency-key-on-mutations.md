---
title: "Payment, notification, and email-outbox POST mutations must enforce Idempotency-Key via @RequireIdempotencyKey"
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
protects_template_id: templates/backend/payment/PaymentController.java
failing_fixture_path: practices/evals/fixtures/idempotency-key-on-mutations/fail_no_annotation/
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-VAL-001"
verification:
  type: review
  notes: "All @PostMapping handlers in payment, notification, and email-outbox controllers must carry @RequireIdempotencyKey. The annotation triggers the IdempotencyFilter which caches responses by key."
evidence:
  - source_type: external
    citation: "IETF draft-ietf-httpapi-idempotency-key-header — The Idempotency-Key HTTP Header Field (deduplicated retry semantics)"
    url: "https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "Stripe API Reference — Idempotent requests: all POST requests accept an Idempotency-Key header to guarantee exactly-once delivery"
    url: "https://docs.stripe.com/api/idempotent_requests"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "AWS API Gateway — Idempotency tokens for preventing duplicate requests in stateful operations"
    url: "https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-routes.html"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## Payment, notification, and email-outbox POST mutations must enforce Idempotency-Key via `@RequireIdempotencyKey`

**Impact: CRITICAL — Network retries without idempotency guards cause duplicate charges, duplicate SMS/push notifications, and duplicate email sends that are indistinguishable from the first request.**

The `api-idempotency-key-required.md` rule defines the general pattern (Idempotency-Key header + 400 on missing). This rule strengthens the enforcement for the three **high-risk mutation domains** — payment, notification, and email-outbox — by requiring the `@RequireIdempotencyKey` annotation, which wires the handler to the `IdempotencyFilter` cache at the framework level rather than relying on ad-hoc header checks in each handler.

The annotation semantics:
1. Filter reads `Idempotency-Key` header from the request.
2. On first call: processes normally, stores `(key → serialised ResponseEntity)` in the idempotency store (Redis/DB).
3. On retry with the same key: returns the cached response immediately, skipping handler execution.
4. Missing key: 400 `application/problem+json` with `type=urn:ax:idempotency:key-missing`.

**Incorrect — POST mutation handlers without @RequireIdempotencyKey:**

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

**Correct — @RequireIdempotencyKey on all side-effecting POST handlers:**

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    // CORRECT: IdempotencyFilter intercepts and deduplicates retries
    @RequireIdempotencyKey
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.ok(paymentService.charge(request));
    }

    @RequireIdempotencyKey
    @PostMapping("/notify")
    public ResponseEntity<Void> sendNotification(@RequestBody NotifyRequest req) {
        notificationService.send(req);
        return ResponseEntity.accepted().build();
    }
}
```

## Why this matters

The payment, notification, and email-outbox templates are the three domains where retried mutations have user-visible, financially and operationally significant consequences:
- **Payment**: double-charge is a regulatory incident and a customer refund obligation.
- **Notification**: duplicate push/SMS sends degrade user trust.
- **Email-outbox**: duplicate transactional emails (password reset, OTP) may violate ESP rate limits and confuse users.

Unlike read endpoints or idempotent writes (PUT/PATCH), POST mutations in these domains have no natural key to deduplicate on — the `Idempotency-Key` header is the caller-supplied deduplication token.

The `@RequireIdempotencyKey` annotation is defined in `templates/backend/idempotency/` and is wired to `IdempotencyFilter` via AOP. Its use is checked at code-review time for all three domains.

## Failing fixture

See: `practices/evals/fixtures/idempotency-key-on-mutations/fail_no_annotation/PaymentController.java` — two `@PostMapping` handlers in the payment controller without `@RequireIdempotencyKey`. A network retry creates a second charge and a second notification.

Reference: [IETF draft — The Idempotency-Key HTTP Header Field](https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/)

Reference: [Stripe API Reference — Idempotent requests](https://docs.stripe.com/api/idempotent_requests)
