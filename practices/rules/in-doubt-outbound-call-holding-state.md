---
title: A non-idempotent outbound call whose response is lost MUST enter a holding state — never silently retry, never assume failure
impact: HIGH
impactDescription: "Silently retrying a lost-response charge / label purchase / SMS double-applies the side effect; assuming failure loses a side effect that actually succeeded. Both corrupt the system of record."
tags:
  - distributed-systems
  - idempotency
  - reconciliation
  - retry-safety
  - in-doubt
spec_ref: "specs/in-doubt-outbound-call-l0.yaml#INDOUBT-HOLD-001"
verification:
  type: review
  source: "specs/in-doubt-outbound-call-l0.yaml"
  pattern: "On a lost / timed-out / reset outbound response, the local record transitions to a terminal holding state (UNKNOWN / IN_DOUBT); resolution is by reconcile-by-query, not by an unconditional retry; the reconcile path is double-effect-free."
upstream:
  - "https://en.wikipedia.org/wiki/Two_Generals%27_Problem"
  - "https://aws.amazon.com/builders-library/making-retries-safe-with-idempotent-APIs/"
evidence:
  - source_type: external
    citation: "Two Generals' Problem — Wikipedia (Akkoyunlu, Ekanadham & Huber 1975; proven unsolvable)"
    url: "https://en.wikipedia.org/wiki/Two_Generals%27_Problem"
    quote: "The Two Generals' Problem was the first computer communication problem to be proven to be unsolvable."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "AWS Builders' Library — Making retries safe with idempotent APIs"
    url: "https://aws.amazon.com/builders-library/making-retries-safe-with-idempotent-APIs/"
    quote: "To overcome this dilemma the provisioning process has to perform a reconciliation to determine whether this workload is running or not."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Stripe — Error handling / low-level network errors"
    url: "https://docs.stripe.com/error-low-level"
    quote: "When intermittent problems occur, clients are usually left in a state where they don't know whether or not the server received the request."
    quoted_at: "2026-06-01"
---

## A non-idempotent outbound call whose response is lost MUST enter a holding state — never silently retry, never assume failure

**Impact: HIGH — a lost outbound response is the most dangerous state in a side-effecting system, because both naive recoveries are wrong**

Every system that makes an outbound call with a real-world side effect — charging a card, purchasing a carrier shipping label, sending an SMS or email, booking a shipment, submitting an external job — eventually hits the same indeterminacy: the request leaves, and then the response (or its ACK) is lost to a timeout, a connection reset, or a dropped packet. You cannot know whether the side effect happened. The Two Generals' Problem proves this is not an engineering gap you can close — it is unsolvable over an unreliable channel, and exactly-once delivery is therefore impossible. The two intuitive recoveries are both corrupting:

- **Silently retry** the call → if the first attempt *did* apply, you have now double-charged / double-shipped / double-sent. A non-idempotent call MUST NOT be blindly retried.
- **Assume failure** and mark the record FAILED → if the first attempt *did* apply, you have lost the side effect in your system of record, and the user / accounting / carrier sees a charge or shipment you believe never happened.

The only correct move is to park the local record in a dedicated terminal **holding state** (`UNKNOWN` / `IN_DOUBT` / terminal-`PENDING`) that means exactly "I do not know yet", and resolve it later by **querying the provider's authoritative status** (reconcile-by-query) — or, if and only if the call carried a provider-honored idempotency key, by a key-stable retry that the provider collapses to at-most-once. This generalizes the payment domain's UNKNOWN-state handling (`specs/payment-l0.yaml` PAYMENT-PROVIDER-001 timeout, PAYMENT-PROVIDER-005 network-reset) to any non-idempotent outbound call.

**Incorrect — a lost response is silently retried (double-effect) or assumed-failed (lost effect):**

```java
public ShipmentLabel buyLabel(BookingRequest req) {
    try {
        return carrier.purchaseLabel(req);          // non-idempotent: charges + mints a label
    } catch (TimeoutException | ConnectException e) {
        // ❌ silent retry: the first attempt may have already minted + charged a label
        return carrier.purchaseLabel(req);          // → two labels, two charges
        // (or, equally wrong elsewhere:)
        // record.markFailed();  return null;        // → a label that DOES exist is lost from our books
    }
}
```

**Correct — park in a holding state; resolve by reconcile-by-query; never re-issue the side effect on the recovery path:**

```java
public LabelResult buyLabel(BookingRequest req) {
    String providerKey = idempotencyKeyFor(req);    // generated + persisted BEFORE the first attempt
    record.setProviderKey(providerKey);
    try {
        ShipmentLabel label = carrier.purchaseLabel(req, providerKey);
        record.markSuccess(label);
        return LabelResult.applied(label);
    } catch (TimeoutException | ConnectException e) {
        record.setState(IN_DOUBT);                  // ✅ terminal holding state, not FAILED, not SUCCESS
        return LabelResult.inDoubt(record.getId()); // caller sees "pending", not a fabricated outcome
    }
}

@Scheduled(fixedDelayString = "${in-doubt.reconcile-interval-seconds:60}000")
void reconcile() {
    for (Record r : records.findInDoubtOlderThan(grace)) {
        // STATUS QUERY ONLY — never carrier.purchaseLabel(...) again on this path
        CarrierStatus s = carrier.queryStatus(r.getProviderKey());
        switch (s) {
            case APPLIED      -> r.markSuccess(s.label());   // catch up to reality; do NOT re-buy
            case NOT_APPLIED  -> r.markFailed();             // now safe to retry as a NEW operation
            case INDETERMINATE-> r.escalateIfOlderThan(maxAgeHours);  // age-out → human review
        }
    }
}
```

The reconcile path is **double-effect-free by construction**: it queries status, it never re-issues the original side-effecting call, and on a confirmed-applied status it makes the *local state* catch up to reality rather than re-executing reality. Retrying is permissible only as a deliberate, declared alternative to querying, and only when the call carried a stable provider idempotency key reused across every attempt — at-least-once delivery plus an idempotent operation is what recovers effectively-once. A fresh key on retry, or a retry without provider idempotency support, reintroduces the double-effect this rule exists to prevent.

Verification: review that every non-idempotent outbound call (a) maps lost/timeout/reset to a terminal holding state distinct from SUCCESS and FAILED, (b) is resolved by a scheduled reconcile-by-query keyed on the provider transaction id / idempotency key, and (c) has a negative test asserting the side-effect count increments by at most the single original intent when the reconciler runs twice or concurrently (`specs/in-doubt-outbound-call-l0.yaml#INDOUBT-NO-DOUBLE-EFFECT-001`).

Reference: [Two Generals' Problem](https://en.wikipedia.org/wiki/Two_Generals%27_Problem)

Reference: [AWS Builders' Library — Making retries safe with idempotent APIs](https://aws.amazon.com/builders-library/making-retries-safe-with-idempotent-APIs/)

Reference: [Stripe — low-level network error handling](https://docs.stripe.com/error-low-level)
