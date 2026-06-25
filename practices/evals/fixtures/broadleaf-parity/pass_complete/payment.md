# Broadleaf-absorption parity — payment (FIXTURE: complete)

- vertical: payment
- broadleaf_source: core/.../payment/domain/OrderPayment.java:78; .../ValidateAndConfirmPaymentActivity.java:257
- spec_items: PAYMENT-SPLIT-001
- rule: REVIEW-TIER (no new rule path required for this fixture)
- behavioral_test: REVIEW-TIER (fixture does not reference a live test path)
- adversarial_review: REVISE→fixed (CRITICAL spec-impl drift reconciled; +currency +2 tests)

## Verification-goal parity (Broadleaf test intent → our coverage)

| Broadleaf test scenario (intent) | our behavioral assertion |
|---|---|
| under-funded order rejected | PaymentSplitTenderTest.singleTenderUnderfunds → 422 shortfall 4000 |
| multiple tenders sum to cover total | twoTendersCoverFull → 200 covered 10000 |
| different-currency tender excluded | differentCurrencyTenderExcluded → 422 (USD not counted) |
