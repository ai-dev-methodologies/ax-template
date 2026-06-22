---
title: POST endpoints with non-idempotent side effects must require an Idempotency-Key header
impact: HIGH
impactDescription: "Network retries on POST without dedupe cause duplicate side effects (double charge, duplicate send)"
tags:
  - api
  - http
  - idempotency
  - retry-safety
spec_ref: "specs/payment-l0.yaml#PAYMENT-IDEMP-001"
verification:
  gradle_task: testPayment
  tag: PAYMENT-IDEMP-001
upstream:
  - "https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/"
  - "https://docs.stripe.com/api/idempotent_requests"
evidence:
  - upstream_id: rfc-7807
    section: "Problem Details for HTTP APIs — error envelope for the missing-key 400"
    quote: "Problem Details"
  - source_type: external
    citation: "IETF draft — The Idempotency-Key HTTP Header Field (draft-ietf-httpapi-idempotency-key-header)"
    url: "https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/"
  - source_type: external
    citation: "Stripe API Reference — Idempotent requests"
    url: "https://docs.stripe.com/api/idempotent_requests"
---

## POST endpoints with non-idempotent side effects must require an Idempotency-Key header

**Impact: HIGH — Network retries on POST without dedupe cause duplicate side effects (double charge, duplicate send)**

`POST` is the HTTP verb defined as non-idempotent: the IETF semantics allow each call to create a fresh resource or trigger a fresh side effect. Any production network — mobile, browser fetch with auto-retry, load balancer retries, service-mesh retries — will retry a request that timed out, returned 502, or lost its socket. Without a dedupe protocol the second attempt double-charges a card, double-sends an email, or double-creates a job. The de-facto fix, standardised by an IETF draft (`draft-ietf-httpapi-idempotency-key-header`) and implemented by Stripe, Adyen, Plaid, GitHub, and Square, is the `Idempotency-Key` request header: the client supplies a unique key per logical operation; the server caches the response keyed by `(principal, key)` for a TTL window (commonly 24 hours) and on a duplicate-key arrival returns the *cached* original response without re-executing the side effect.

This rule applies to any POST endpoint whose execution has a non-idempotent side effect: payment authorize/capture/refund/void, notification dispatch (email / SMS / push), order placement, file upload finalize, webhook delivery. The rule does **not** apply to read-only POST endpoints (rare but valid) or to `PUT`/`DELETE` endpoints whose semantics are already idempotent by HTTP definition.

**Incorrect — POST without idempotency key, retry replays the side effect:**

```java
@PostMapping("/api/payments")
public PaymentResponse create(@Valid @RequestBody CreatePaymentRequest req) {
    // network retry → second invocation → second charge
    return paymentService.charge(req);
}
```

**Correct — manual null-check + dedupe store:**

```java
@PostMapping("/api/payments")
public ResponseEntity<PaymentResponse> create(
        @Valid @RequestBody CreatePaymentRequest req,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
        throw new PaymentValidationException("Idempotency-Key header is required");
    }
    UUID userId = UUID.fromString(jwt.getSubject());
    UUID paymentId = idempotencyStore.findOrCreate(userId, idempotencyKey,
            () -> paymentService.charge(req).getId());  // executes once per (userId, key)
    return ResponseEntity.ok(paymentService.getPayment(paymentId, userId));
}
// Missing header → manual guard throws a domain exception mapped to 400 with an
// RFC 7807 ProblemDetail by the global @ExceptionHandler.
```

The store layer (Caffeine, Redis, or a database table) MUST be atomic — `putIfAbsent` semantics or `SELECT ... FOR UPDATE` — so that concurrent duplicate requests with the same key collapse to one execution and the losers either wait for the result or receive the same cached payload. A non-atomic implementation re-creates the double-charge bug under racing retries.

Verification: `./gradlew testPayment --tests "*Idempotency*"` exercises (a) missing-header → 400 RFC 7807, (b) duplicate-key within TTL → cached response, no second charge, (c) 5-thread concurrent same-key race → exactly one charge created. `Idempotency-Key` is the canonical header name; alternative names (`X-Idempotency-Key`, `Request-Id`) should be avoided for interop with PSP and platform tooling that assume the IETF draft name.

Reference: [IETF draft — The Idempotency-Key HTTP Header Field](https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/)

Reference: [Stripe API — Idempotent requests](https://docs.stripe.com/api/idempotent_requests)
