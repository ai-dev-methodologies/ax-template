---
title: A net requirement quantized to a procurement constraint must round UP deterministically to the supplier lot multiple at or above the MOQ (orderQuantity = max(MOQ, ceil(required / multiple) * multiple)), and because this is NON-CONSERVING by design — the placed order exceeds the requirement — the surplus overage = orderQuantity − required MUST be computed exactly and RECORDED (never hidden), the full basis persisted so it is reconstructible, and MOQ / multiple held positive — the deliberate opposite of the catalog's conserving rounded-split
impact: HIGH
impactDescription: "A replenishment/purchase-order quantizer that rounds DOWN, ignores the MOQ, or silently drops the overage places wrong-quantity orders a supplier rejects or that strand surplus stock with no recorded reason; a quantizer that divides by a zero order multiple throws or corrupts; and a non-conserving surplus that is not recorded inflates the order with no audit trail (CWE-682) — the procurement cost of the lot constraint becomes invisible and cannot be reconciled. Conserving primitives (transformation-conservation, rounded-split) are the WRONG model here: the order MUST exceed the requirement and the excess is real"
tags:
  - calculation
  - audit
  - validation
  - billing
  - governance
spec_ref: "specs/order-multiple-quantization-l0.yaml#ORDERQUANT-QUANTIZE-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/orderquantization/Quantizer.java + backend/src/main/java/com/ax/template/authblueprint/orderquantization/OrderQuantizationService.java + backend/src/main/java/com/ax/template/authblueprint/orderquantization/OrderQuantization.java"
  pattern: "Quantizer.quantize computes orderQuantity = max(moq, ceilDiv(required, multiple) * multiple) in exact long arithmetic (round the requirement UP to the next whole order multiple, then floor at the MOQ); OrderQuantizationService.quantize validates required >= 0 AND moq >= 1 AND multiple >= 1 (else 422 ORDERQUANT_INVALID_CONSTRAINT, guarding the divide-by-zero), computes the NON-CONSERVING overage = orderQuantity - required, and persists the full basis (required, moq, multiple, orderQuantity, overage) on an immutable OrderQuantization whose @Check binds overage = order_quantity - required_quantity AND overage >= 0 AND moq >= 1 AND order_multiple >= 1 AND required_quantity >= 0; the quantizer is a pure function of its three inputs so the same (required, moq, multiple) is idempotent; columns are order_quantity / order_multiple (never value / order); NO delete path exists"
upstream:
  - "https://www.acquisition.gov/far/52.207-4"
  - "https://www.acquisition.gov/far/7.204"
  - "https://cwe.mitre.org/data/definitions/682.html"
evidence:
  - source_type: external
    citation: "FAR 52.207-4(b), Economic Purchase Quantity—Supplies (Acquisition.gov) — the federal-acquisition lot-sizing authority the MOQ / order-multiple quantization generalizes: buying in the quantity at which a price break occurs"
    url: "https://www.acquisition.gov/far/52.207-4"
    quote: "An economic purchase quantity is that quantity at which a significant price break occurs."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "FAR 7.204, Responsibilities of the requirements and supply personnel for economic order quantities (Acquisition.gov) — the inventory-manager discipline the recorded quantization basis supports"
    url: "https://www.acquisition.gov/far/7.204"
    quote: "The economic purchase quantity data so obtained are intended to assist inventory managers in establishing and evaluating economic order quantities for supplies under their cognizance."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "CWE-682: Incorrect Calculation — MITRE (the non-conserving ceiling arithmetic must be exact and the overage reconstructible, the divisor guarded before the calculation)"
    url: "https://cwe.mitre.org/data/definitions/682.html"
    quote: "The product performs a calculation that generates incorrect or unintended results that are later used in security-critical decisions or resource management."
    quoted_at: "2026-06-23"
---

## A procurement quantization rounds UP to a positive lot multiple at or above the MOQ — and because it is NON-conserving, the surplus is recorded, not hidden

**Impact: HIGH — a quantizer that rounds down, ignores the MOQ, divides by a zero multiple, or drops the overage places wrong-quantity orders and erases the procurement cost of the lot constraint (CWE-682).**

Order-multiple / MOQ quantization is the arithmetic every replenishment and purchase-order system runs against a net requirement: a supplier imposes a **minimum order quantity** (you cannot order below it) and an **order multiple** (a lot / pack / case size you cannot subdivide), so a required net quantity must be quantized UP to the smallest *placeable* order. The federal-acquisition discipline names the same idea — *"An economic purchase quantity is that quantity at which a significant price break occurs"* — and *"The economic purchase quantity data … are intended to assist inventory managers in establishing and evaluating economic order quantities."* The catalog already modeled **conserving** primitives — transformation-conservation (input == output + classified loss) and the conserving rounded-split (the parts sum back to the whole) — but had no primitive for their **non-conserving sibling**:

```text
quantize(required, moq, multiple):  orderQuantity = max(moq, ceil(required / multiple) * multiple)
                                     — round the requirement UP to the next whole order multiple,
                                       then floor at the MOQ
overage:                            orderQuantity - required  (>= 0)  — the REAL surplus the lot
                                     constraint forces; recorded, NEVER hidden
constraint:                         moq >= 1 AND multiple >= 1 AND required >= 0 (422 + @Check) —
                                     a zero multiple would be a divide-by-zero
basis:                              (required, moq, multiple, orderQuantity, overage) persisted,
                                     immutable, reconstructible
```

**1. The quantizer rounds UP deterministically (ORDERQUANT-QUANTIZE-001).** `orderQuantity = max(moq, ceil(required / multiple) * multiple)` is the smallest quantity that is at least `required`, at least `moq`, and an exact integer multiple of `multiple`. It is a pure function of three inputs — no clock, no sequence — so it is idempotent (ORDERQUANT-IDEMPOTENT-001).

**2. It does NOT conserve — and that is the point (ORDERQUANT-OVERAGE-001).** `orderQuantity >= required` by construction; the surplus `overage = orderQuantity - required` is the procurement cost of a lot constraint that cannot be subdivided. This is the *opposite* of a conserving rounded-split (whose parts sum back to the whole): the rounded-split creates nothing, this deliberately creates surplus. The surplus is RECORDED on the row and a `@Check` binds `overage = order_quantity - required_quantity` so it can never be faked or dropped.

**3. The basis is recorded and the constraints are positive (ORDERQUANT-BASIS/CONSTRAINT-001).** All five inputs/outputs are persisted on an immutable row so the quantization is reconstructible; `moq >= 1 AND order_multiple >= 1 AND required >= 0` is enforced both at the API boundary (422) and as a DB `@Check`, guarding the divide-by-zero before the calculation runs.

**Incorrect — rounds down, ignores the MOQ, divides by a possibly-zero multiple, drops the surplus:**

```java
public long orderQty(long required, long moq, long multiple) {
    long lots = required / multiple;          // ❌ integer DIVISION rounds DOWN — orders too little
    long qty = lots * multiple;               // ❌ result can be < required and < moq
    return qty;                               // ❌ MOQ ignored; ❌ multiple==0 → ArithmeticException;
}                                             // ❌ overage never computed — the surplus is invisible
```

**Correct — pure ceiling quantizer floored at the MOQ, recorded non-conserving overage, guarded divisor:**

```java
// Quantizer — the pure deterministic function the whole domain turns on
static long quantize(long required, long moq, long multiple) {
    long roundedUp = ceilDiv(required, multiple) * multiple;   // ✅ round UP to the next lot
    return Math.max(moq, roundedUp);                           // ✅ floor at the MOQ
}
private static long ceilDiv(long required, long multiple) {
    return (required + multiple - 1) / multiple;               // ✅ ceiling, multiple already > 0
}

@Transactional
public OrderQuantization quantize(String itemRef, long required, long moq, long multiple) {
    if (required < 0 || moq < 1 || multiple < 1) {             // ✅ guard the bounds + the divisor
        metrics.record("invalid_constraint");
        throw OrderQuantizationException.invalidConstraint(/* … */);   // 422
    }
    long orderQuantity = Quantizer.quantize(required, moq, multiple);  // ✅ deterministic, idempotent
    long overage = orderQuantity - required;                           // ✅ NON-CONSERVING surplus, >= 0
    return records.save(new OrderQuantization(UUID.randomUUID(), itemRef,
        required, moq, multiple, orderQuantity, overage, Instant.now(clock)));  // ✅ full basis recorded
}
```

The `OrderQuantization` row is immutable (every `@Column(updatable=false)`) with a `@Check` that binds `overage = order_quantity - required_quantity AND overage >= 0 AND moq >= 1 AND order_multiple >= 1 AND required_quantity >= 0`, so a record that drops or fakes the surplus, or carries a non-positive constraint, is unrepresentable at the storage layer. The columns are `order_quantity` / `order_multiple` (never `value` / `order`); no delete path exists.

Verification: review-tier — confirm the quantizer rounds UP (`ceilDiv`) and floors at the MOQ (`Math.max`), the service rejects `required < 0 || moq < 1 || multiple < 1` with 422 before any division, the overage is computed as `orderQuantity - required` and recorded, the `@Check` binds the overage identity, and the basis columns are immutable. The behavioural proof a fork-receiver keeps green: `required=23, MOQ=1, multiple=10 → orderQuantity 30, overage 7` and `required=5, MOQ=50, multiple=10 → orderQuantity 50, overage 45`.

Reference: [FAR 52.207-4 — Economic Purchase Quantity—Supplies](https://www.acquisition.gov/far/52.207-4)

Reference: [FAR 7.204 — Economic order quantities](https://www.acquisition.gov/far/7.204)

Reference: [CWE-682: Incorrect Calculation](https://cwe.mitre.org/data/definitions/682.html)
