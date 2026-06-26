# Broadleaf-absorption parity — promotion

- vertical: promotion
- broadleaf_source: core/.../offer/service/discount/OrderOfferComparator.java; offer/domain/OfferImpl.java; offer/domain/OfferAuditImpl.java
- spec_items: PROMO-CONSERVE-001
- rule: practices/rules/promotion-offer-engine-conserves-determinism-atomic-cap.md
- behavioral_test: backend/src/test/java/com/ax/template/authblueprint/commercepromotion/CommercePromotionComplianceTest.java
- violation_proof: backend/src/test/java/com/ax/template/authblueprint/commercepromotion/CommercePromotionViolationProofTest.java
- adversarial_review: REVISE→fixed (2 MAJOR: ORDER-path clamp did not track cumulative remaining[] → line could go negative/money-loss; stackable gate unimplemented with false Javadoc → threaded remaining[] + resolveOffers dedup + behavioral tests)

## Verification-goal parity (Broadleaf test intent → our coverage)

| Broadleaf test scenario (intent) | our behavioral assertion |
|---|---|
| order-level discount prorated to items conserves to the cent (Σ shares == discount) | proration test: Σ item adjustments == order discount exactly (largest-remainder) |
| offers apply in a deterministic total order (priority, then potential savings) | deterministic-order test: same cart → identical result; not collection order |
| max-uses cap is atomic (no over-redemption under concurrency) | UNIQUE(offer_id, order_ref) + pessimistic lock; concurrent redeem → exactly one |
| a discount never drives a line below zero | clamp test: adjustment ≤ line price, never negative |
| stackable ≠ combinable (non-combinable blocks further offers) | stackable/combinable gate test: non-combinable offer blocks co-application |
