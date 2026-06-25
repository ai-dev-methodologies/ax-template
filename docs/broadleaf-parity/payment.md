# Broadleaf-absorption parity — payment (split-tender coverage)

- vertical: payment
- broadleaf_source: core/.../payment/domain/OrderPayment.java:78; checkout/service/workflow/ValidateAndConfirmPaymentActivity.java:257; payment/service/type/OrderPaymentStatus.java:61
- spec_items: PAYMENT-SPLIT-001
- rule: practices/rules/payment-split-tender-coverage-sums-to-total-and-capture-bounded-by-auth.md
- behavioral_test: backend/src/test/java/com/ax/template/authblueprint/payment/PaymentSplitTenderTest.java
- adversarial_review: REVISE→fixed (CRITICAL spec-impl drift: spec claimed coverage vs server-frozen ORDER-TOTAL-SNAPSHOT-001 but impl trusted client-supplied total → reconciled to orchestrator-supplied, payment isolated from order; + currency filter, BigDecimal wording, dropped dead jwt, +2 tests)

## Verification-goal parity (Broadleaf test intent → our coverage)

| Broadleaf test scenario (intent) | our behavioral assertion |
|---|---|
| an order under-covered by its tenders cannot be confirmed | singleTenderUnderfunds → 422 PAYMENT_TENDERS_UNDERFUNDED, shortfall 4000 |
| multiple tenders summing to the total cover the order | twoTendersCoverFull → 200, covered == 10000 |
| an AUTHORIZED/CAPTURED tender counts toward coverage | capturedTenderCountsTowardCoverage → 200, covered == 10000 |
| a tender in a different currency does NOT count | differentCurrencyTenderExcluded → 422 (USD excluded from KRW total) |
| a VOIDED tender is excluded from coverage | voidedTenderExcluded → 422, shortfall 4000 |

> PAYMENT-SPLIT-002 (Σcaptured ≤ authorized auth-side dual) is specced applicable:false /
> impl-deferred — the single-row capture model needs a capture ledger. The invariant is
> absorbed and visible without a hollow always-passing test.
