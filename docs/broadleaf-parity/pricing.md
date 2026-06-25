# Broadleaf-absorption parity — pricing

- vertical: pricing
- broadleaf_source: core/.../pricing/service/workflow/FulfillmentItemPricingActivity.java:~; pricing/service/workflow/TotalActivity.java:~
- spec_items: PRICING-ORDER-001
- rule: practices/rules/pricing-pipeline-orders-discount-before-tax-and-total-conserves.md
- behavioral_test: backend/src/test/java/com/ax/template/authblueprint/commercepricing/CommercePricingComplianceTest.java
- adversarial_review: REVISE→fixed (IDEMPOTENT item reframed to stateless pure recompute; thin-by-design banner added — ~70% of Broadleaf pricing already absorbed by promotion/catalog/banded)

## Verification-goal parity (Broadleaf test intent → our coverage)

| Broadleaf test scenario (intent) | our behavioral assertion |
|---|---|
| tax is charged on the NET (post-discount) base, never the gross | discounted-order test: tax == rate × (item − prorated discount) |
| swapping phase order (tax-on-gross) yields a strictly higher, wrong total | keystone: pipeline tax < gross-base tax; produced total uses net tax |
| order total is the conserving sum of disclosed components | total == subTotal − discount + shipping + tax exactly (no penny lost) |
| penny-conservation under naive rounding | engineered fixture: total reconstructs from disclosed lines exactly |
