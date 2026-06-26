# Broadleaf-absorption parity — order (cart→order spine)

- vertical: order
- broadleaf_source: core/.../order/domain/OrderItem.java; order/service/type/OrderStatus.java; order/domain/Order.java
- spec_items: ORDER-SNAPSHOT-001
- rule: practices/rules/order-cart-spine-price-snapshot-immutable-after-submit-merge-and-fulfillment-conserves.md
- behavioral_test: backend/src/test/java/com/ax/template/authblueprint/commerceorder/CommerceOrderComplianceTest.java
- violation_proof: backend/src/test/java/com/ax/template/authblueprint/commerceorder/CommerceOrderViolationProofTest.java
- adversarial_review: REVISE→fixed (3 HIGH fulfillment bugs: no post-submit status guard, non-idempotent append, phantom orderItemId accepted → status guard + clear-before-add + unknown-id rejection + multiplyExact); separately DDD-006 cross-aggregate object pointer → FGItem converted to UUID id-ref (caught by R25 testPractices)

## Verification-goal parity (Broadleaf test intent → our coverage)

| Broadleaf test scenario (intent) | our behavioral assertion |
|---|---|
| an order line freezes the unit price at add-time (not re-read from live catalog) | snapshot test: line price unchanged after a catalog price edit |
| a SUBMITTED order rejects add/update/remove | immutable test: mutating a SUBMITTED order → 409 |
| adding the same SKU merges quantity (no duplicate line) | merge test: add qty 2 then qty 3 → one line, quantity 5 |
| fulfillment partition conserves (Σ group-item qty == line qty) | fulfillment test: 5 units split 2+3 ok; 2+2 → 422; phantom id → 422 |
| fulfillment is post-submit + idempotent | assign on IN_PROCESS → 409; re-assign replaces (no double) |
