---
title: "Payment UI must realize the payment contract — checkout with method picker + idempotency-key handler + slow-provider warning, idempotent success/failure pages, methods list/detail, refund"
impact: HIGH
rule_id: payment-frontend-checkout-idempotent-states
impactDescription: "A checkout that re-POSTs without a stable idempotency key double-charges on a retry or back-button; one with no slow-provider warning leaves the user staring at a frozen button and re-clicking (more double-charges); a success page that is not idempotent re-runs side effects on refresh; a failure page with no retry strands a recoverable payment. Payment is the surface where a UI bug is a financial bug."
tags:
  - payment
  - frontend
  - idempotency
  - checkout
  - contract-first
  - financial
applicable_to:
  - react
  - nextjs
spec_ref: "specs/payment-frontend-l0.yaml#PAYMENT-FE-001"
verification:
  type: review
  notes: |
    Reviewer confirms the payment UI against specs/payment-frontend-l0.yaml: the checkout page renders a
    PaymentMethodPicker (001) and a PaymentCheckoutForm with amount display + card fields + pay button
    (002), wrapped by an IdempotencyKeyHandler whose key is sent as the Idempotency-Key header and is
    STABLE across retries (003); a SlowProviderWarning appears after 3000ms in-flight (004); the
    'already processed' state is shown when the server returns the idempotent-replay result (011). The
    success page renders a receipt for a completed payment by orderId and is idempotent on refresh (005);
    the failure page renders an error state with a retry for a failed payment by orderId (006). The
    methods list shows payment history with method types (007); methods/new renders the PaymentMethodPicker
    (008); methods/detail renders a payment detail by id via getPayment (009); the refund page submits a
    refund for an orderId via refundPayment (010). Idempotency key is generated once and reused on retry,
    never regenerated per click.
evidence:
  - source_type: external
    citation: "React Docs — Reacting to input with state (declarative UI): checkout renders idle/in-flight/slow/already-processed/success/failure states declaratively (PAYMENT-FE-004/005/006/011)"
    url: "https://react.dev/learn/reacting-to-input-with-state"
    quote: "React provides a declarative way to manipulate the UI. Instead of manipulating individual pieces of the UI directly, you describe the different states that your component can be in, and switch between them in response to the user input."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "RFC 9457 Problem Details — the failure page maps the backend payment problem type to a specific error + retry; the 'already processed' replay is recognized from the response (PAYMENT-FE-006/011)"
    url: "https://www.rfc-editor.org/rfc/rfc9457"
    quote: "This document defines a 'problem detail' to carry machine-readable details of errors in HTTP response content to avoid the need to define new error response formats for HTTP APIs."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## Payment UI must realize the payment contract — idempotent checkout with method picker + slow-provider warning, idempotent success/failure, methods, refund

**Impact: HIGH — On a payment surface a UI bug is a financial bug. The classic double-charge is a frontend failure: the user clicks Pay, the provider is slow, the button looks dead, they click again — and without a STABLE idempotency key the second click is a second charge. The catalog's backend payment idempotency (`PAYMENT-CALLBACK-002`, `idempotency-key-on-mutations`) only protects the user if the UI sends the SAME key on the retry. So the checkout wraps the form in an IdempotencyKeyHandler, shows a SlowProviderWarning after 3s so the user waits instead of re-clicking, and recognizes the 'already processed' replay. React renders these states declaratively — *you describe the different states that your component can be in, and switch between them in response to the user input* — and the failure page maps the backend RFC 9457 problem type to a specific, retryable error.**

There are eleven load-bearing requirements — the items of `specs/payment-frontend-l0.yaml`, all governed by this rule.

**Checkout (PAYMENT-FE-001..004, 011).** A PaymentMethodPicker to choose a method (001); a PaymentCheckoutForm with amount display, card fields, and a pay button (002); wrapped in an IdempotencyKeyHandler whose key is sent as `Idempotency-Key` and is STABLE across retries — generated once, reused, never regenerated per click (003); a SlowProviderWarning shown after 3000ms in-flight (004); an 'already processed' state shown when the server returns the idempotent-replay result (011).

**Result pages (PAYMENT-FE-005..006).** A success page rendering a receipt for a completed payment by `orderId`, idempotent on refresh — a reload re-reads, never re-charges (005); a failure page rendering an error state for a failed payment by `orderId` with a retry action (006).

**Methods + refund (PAYMENT-FE-007..010).** A methods list DataTable showing payment history with method types (007); a methods/new page with the PaymentMethodPicker (008); a methods/detail view by payment id via `getPayment` (009); a refund page submitting a refund for an `orderId` via `refundPayment` (010).

**Incorrect — new idempotency key per click, no slow warning, success page re-charges on refresh:**

```tsx
async function pay() {
  await paymentClient.charge({ ...form, idempotencyKey: crypto.randomUUID() });  // VIOLATION: new key per click → double charge (PAYMENT-FE-003)
}
<button onClick={pay}>Pay</button>                          {/* VIOLATION: no in-flight/slow state (PAYMENT-FE-004) */}
useEffect(() => { charge(order); }, []);                    {/* VIOLATION: success page re-charges on refresh (PAYMENT-FE-005) */}
```

**Correct — stable idempotency key, slow-provider warning, idempotent receipt read:**

```tsx
const idemKey = useIdempotencyKey(orderId);                 // STABLE across retries (PAYMENT-FE-003)
async function pay() { await paymentClient.charge({ ...form }, { 'Idempotency-Key': idemKey }); }
{inFlightMs > 3000 && <SlowProviderWarning />}              // PAYMENT-FE-004
{result?.alreadyProcessed && <AlreadyProcessed />}          // idempotent replay recognized (PAYMENT-FE-011)
// success page: READ the receipt by orderId (getPayment), never charge (PAYMENT-FE-005)
const receipt = await getPaymentByOrder(orderId);
// failure page: map RFC 9457 problem type → specific error + retry (PAYMENT-FE-006)
```

Verification: review-tier. Payment-UI correctness is a financial-safety property with no compile signal — a per-click key and a re-charging success page compile and only double-charge under retry/refresh. Verify by review against `specs/payment-frontend-l0.yaml`: the idempotency key is stable across retries; a slow-provider warning appears after 3s; the success page reads (never charges) and is refresh-idempotent; the failure page maps the problem type and offers retry; methods/refund pages call their documented endpoints. When a fork-receiver wires real tests (double-click sends one key; success refresh does not re-charge), this rule's verification may be upgraded from review to a test-tag binding.

Reference: [React — Reacting to input with state](https://react.dev/learn/reacting-to-input-with-state)

Reference: [RFC 9457 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457)
