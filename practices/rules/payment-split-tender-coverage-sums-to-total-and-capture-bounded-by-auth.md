---
title: An order paid by multiple tenders MUST conserve coverage — the sum of the active, successfully-authorized payment amounts sharing one order id MUST cover the order's frozen total before the order is confirmable (an under-covered order is rejected, never shipped unpaid) — and each capture MUST be bounded by its authorization (the cumulative captured amount never exceeds the authorized amount, the auth-side dual of the refund cap)
impact: HIGH
impactDescription: "An order may be paid by more than one tender (two cards, a gift card plus a card). If the platform confirms an order without checking that the tenders actually SUM to the order total, an order ships having collected less than it is owed — silent revenue loss that surfaces only at settlement reconciliation; the failure mode is exactly an off-by-one tender (the customer added one card covering part of the basket and checkout completed before the second tender landed). Conversely, if a capture is not bounded by its authorization, the platform captures more money than the cardholder authorized — an over-charge the card networks will charge back. Split-tender coverage (Σ tenders ≥ order total) and capture-bound (Σ captured ≤ authorized) are the two money-conservation invariants a single-row payment FSM does not express: coverage is a sum-UP-to-a-target across N sibling payment rows (distinct from a refund ≤ ceiling on one row, from a net-zero posting, and from a single-bucket draw); capture-bound is the auth-side mirror of the already-enforced refund cap."
tags:
  - e-commerce
  - payment
  - money
  - conservation
  - settlement
spec_ref: "specs/payment-l0.yaml#PAYMENT-SPLIT-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/payment/PaymentService.java + backend/src/main/java/com/ax/template/authblueprint/payment/PaymentRepository.java"
  pattern: "An order may carry multiple Payment rows sharing one order_id (split tender). A confirm-coverage operation sums the active, successfully-authorized tender amounts (states AUTHORIZED, CAPTURED) for that order_id IN ONE CURRENCY and rejects the confirm (422 PAYMENT_TENDERS_UNDERFUNDED, residual shortfall in the problem body) when the sum is below the order's frozen total — which is SUPPLIED BY THE TRUSTED ORCHESTRATOR (the payment domain is isolated from the order aggregate and does not resolve the total itself, mirroring Broadleaf's ValidateAndConfirmPaymentActivity reading order.getTotal() from the checkout context). The sum is exact decimal (BigDecimal at the currency scale, no floating point). Capture is bounded by authorization: the cumulative captured amount for an authorization never exceeds the authorized amount (the auth-side dual of the refund cap Σrefunds ≤ captured in PAYMENT-REFUND-002), an over-capture rejected 422 PAYMENT_CAPTURE_EXCEEDS_AUTH."
upstream:
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/payment/domain/OrderPayment.java"
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/checkout/service/workflow/ValidateAndConfirmPaymentActivity.java"
  - "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/payment/service/type/OrderPaymentStatus.java"
  - "https://en.wikipedia.org/wiki/Split_payment"
evidence:
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) OrderPayment.getAmount Javadoc — the split-tender coverage invariant absorbed: an Order has many OrderPayments and their amounts sum to the order total (1:N tender coverage)"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/payment/domain/OrderPayment.java"
    quote: "The summation of all of the {@link OrderPayment}s for a particular"
    quoted_at: "2026-06-25"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) ValidateAndConfirmPaymentActivity — the mechanical coverage gate absorbed: checkout rejects the order when the accumulated payment sum is less than the order total"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/checkout/service/workflow/ValidateAndConfirmPaymentActivity.java"
    quote: "if (paymentSum.lessThan(order.getTotal())) {"
    quoted_at: "2026-06-25"
  - source_type: external
    citation: "Broadleaf Commerce (develop-7.0.x) OrderPaymentStatus.FULLY_CAPTURED — the capture-bound (auth-side) invariant absorbed: a payment is fully captured when the partial CAPTURE amounts equal the original (authorized) order-payment transaction; cumulative captures cannot exceed it"
    url: "https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/payment/service/type/OrderPaymentStatus.java"
    quote: "transaction amounts equal the original order payment transaction,"
    quoted_at: "2026-06-25"
  - source_type: external
    citation: "Wikipedia — Split payment (the generic definition of split tender: one full payment divided across multiple payment methods, the shape the coverage invariant conserves)"
    url: "https://en.wikipedia.org/wiki/Split_payment"
    quote: "Split payment (also split payment transaction, or split tender) is the financial term for the act of splitting (dividing) a single full payment into two or more simultaneous transactions made by different payment methods."
    quoted_at: "2026-06-25"
---

## Rule

An order can be paid by **more than one tender** — two cards, a gift card plus a card, a points balance plus a card. Per Broadleaf, an `Order` has many `OrderPayment`s and *the summation of all of the OrderPayments for a particular Order should equal Order#getTotal()*; per the generic definition, split tender *is the financial term for the act of splitting a single full payment into two or more simultaneous transactions made by different payment methods*. Two money-conservation invariants follow that a single-row payment FSM (one `Payment`, scalar `state`, `AUTHORIZED→CAPTURED` full-amount) does not express:

1. **Coverage — Σ tenders ≥ order total (the flagship).** An order may carry N ≥ 1 payment rows sharing one `order_id`. Before the order is confirmable, the sum of the **active, successfully-authorized** tender amounts (states `AUTHORIZED`, `CAPTURED`) for that `order_id` MUST cover the order's frozen total. That total is **supplied by the trusted orchestrator** — the order-confirm / checkout caller that owns `ORDER-TOTAL-SNAPSHOT-001` in its workflow context. The payment domain is deliberately **isolated** from the order aggregate (`payment.order_id` is a free-form reference, no FK to the order), so the coverage gate does not resolve the total itself; this mirrors Broadleaf's `ValidateAndConfirmPaymentActivity`, which reads `order.getTotal()` from the checkout workflow context and is the trusted caller. An under-covered confirm is rejected — **422 `PAYMENT_TENDERS_UNDERFUNDED`**, with the residual shortfall disclosed — so an order is never confirmed having collected less than it is owed. This is a sum-**UP**-to-a-target across sibling rows: distinct from the refund cap `Σrefunds ≤ captured` (a ≤ ceiling on ONE row), from a balanced posting (signed legs netting to **zero**), and from an accumulator draw (a single moving watermark). Broadleaf enforces it mechanically: `if (paymentSum.lessThan(order.getTotal())) { throw … }`.

2. **Capture bounded by authorization — Σ captured ≤ authorized (the auth-side dual).** The cumulative captured amount against an authorization MUST never exceed the authorized amount; a payment is fully captured when its (possibly partial) capture amounts equal the original authorized transaction. An over-capture is rejected **422 `PAYMENT_CAPTURE_EXCEEDS_AUTH`**. This is the exact mirror of the already-enforced refund cap (`PAYMENT-REFUND-002`: `Σrefunds ≤ captured`) on the authorization→capture edge instead of the capture→refund edge. (Auth-side multi/partial capture requires a capture ledger the current single-row model does not yet carry — `PAYMENT-SPLIT-002` is specced `applicable: false`, impl-deferred; this rule states the invariant so it is absorbed and visible.)

Money is exact decimal (`BigDecimal` at the currency's scale, never floating point), summed over a single currency and compared against the frozen total exactly (no penny invented).

**Correct — coverage checked against the orchestrator-supplied frozen total before confirm; under-coverage rejected:**

```java
// backend/.../payment/PaymentService.java — split tender: N rows share order_id; Σ active authorized ≥ total.
// orderTotal is supplied by the trusted orchestrator (it owns ORDER-TOTAL-SNAPSHOT-001); payment is isolated from the order.
public CoverageResult confirmCoverage(String orderId, String currency, BigDecimal orderTotal) {
    BigDecimal covered = payments.sumActiveAuthorizedByOrderId(orderId, currency);  // Σ over sibling tenders, one currency
    if (covered.compareTo(orderTotal) < 0) {                          // sum-UP-to-target coverage gate (exact decimal)
        throw new TendersUnderfundedException(orderId, orderTotal, covered);  // 422, residual shortfall disclosed
    }
    return new CoverageResult(orderId, orderTotal, covered);
}
// capture is bounded by its authorization (auth-side dual of the refund cap — PAYMENT-SPLIT-002, impl-deferred)
if (alreadyCaptured.add(requested).compareTo(authorizedAmount) > 0) {
    throw PaymentException.captureExceedsAuth();                      // 422 — never capture beyond what was authorized
}
```

**Incorrect — confirm without coverage; capture ignores the authorization:**

```java
// confirm the order without summing the tenders → an order with one 6000 tender on a 10000 total ships 4000 short
order.confirm();                                  // WRONG: no Σ tenders ≥ total gate (silent revenue loss at settlement)
payment.setCapturedAmount(requested);             // WRONG: captures whatever is asked, may exceed the authorization
```

Confirming without the coverage gate ships orders that collected less than the total; capturing without the auth bound charges beyond what the cardholder authorized (a chargeback). Summing the sibling tenders against the frozen total before confirm, and bounding cumulative capture by the authorization, make both defects unrepresentable.

Reference: [Broadleaf OrderPayment.getAmount (Σ OrderPayments == Order total)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/payment/domain/OrderPayment.java)

Reference: [Broadleaf ValidateAndConfirmPaymentActivity (paymentSum < total → reject)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/checkout/service/workflow/ValidateAndConfirmPaymentActivity.java)

Reference: [Broadleaf OrderPaymentStatus.FULLY_CAPTURED (Σ partial captures == authorized)](https://github.com/BroadleafCommerce/BroadleafCommerce/blob/develop-7.0.x/core/broadleaf-framework/src/main/java/org/broadleafcommerce/core/payment/service/type/OrderPaymentStatus.java)

Reference: [Wikipedia — Split payment (split tender definition)](https://en.wikipedia.org/wiki/Split_payment)
