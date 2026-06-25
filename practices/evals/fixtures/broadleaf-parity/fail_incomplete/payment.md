# Broadleaf-absorption parity — payment (FIXTURE: incomplete)

- vertical: payment
- broadleaf_source: core/.../payment/domain/OrderPayment.java:78
- spec_items: PAYMENT-SPLIT-001
<!-- FIXTURE (fail): MISSING required fields `rule`, `behavioral_test`,
     `adversarial_review`, AND has ZERO verification-goal parity rows.
     The parity guard MUST BLOCK this incomplete record. -->

## Verification-goal parity (Broadleaf test intent → our coverage)

| Broadleaf test scenario (intent) | our behavioral assertion |
|---|---|
