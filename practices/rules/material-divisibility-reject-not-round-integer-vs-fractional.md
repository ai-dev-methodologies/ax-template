---
title: A per-material divisibility constraint must REJECT — never silently round — a quantity its policy forbids — an INTEGER_ONLY material rejects any quantity with a non-zero fractional part (422 NON_INTEGRAL_QUANTITY, naming the material) and a FRACTIONAL material rejects a quantity whose decimal scale exceeds its recorded maximum (422 EXCESS_PRECISION); integrality and scale are tested with EXACT BigDecimal.stripTrailingZeros (so 5 == 5.0 == 5.00 are integral, format-independent), the policy is a recorded versioned per-material property, and every check is recorded with the policy version in force — the deliberate opposite of the round-UP-to-a-lot-multiple quantizer, which CHANGES the number
impact: HIGH
impactDescription: "A divisibility gate that SILENTLY ROUNDS a fractional quantity to a whole unit for an integer-only material (a discrete item, a motor, a license) corrupts the requirement without an audit trail — it ships 3 motors when 2.5 was ordered, or 0 when 0.4 was, and the caller never learns their input was unrepresentable; a gate that tests integrality with a Double parse or a string-length heuristic misclassifies (0.1 + 0.2 != 0.3 in binary floating point — CWE-682) so 5.00 looks fractional or 5.5 looks whole; a gate that drops excess fractional digits silently truncates 1.2345 kg to 1.234 (CWE-1339), under- or over-charging. The catalog modeled the round-UP-to-a-lot-multiple quantizer but had NO primitive for the prior, orthogonal question every item master answers first — MAY this material carry a fractional quantity at all, and to what precision — which is a REJECT gate, not a quantizer"
tags:
  - validation
  - calculation
  - audit
  - governance
spec_ref: "specs/material-divisibility-constraint-l0.yaml#DIV-INTEGRAL-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/divisibility/DivisibilityArithmetic.java + backend/src/main/java/com/ax/template/authblueprint/divisibility/DivisibilityService.java + backend/src/main/java/com/ax/template/authblueprint/divisibility/MaterialDivisibilityPolicy.java"
  pattern: "DivisibilityArithmetic.isIntegral computes q.stripTrailingZeros().scale() <= 0 and effectiveScale computes Math.max(0, q.stripTrailingZeros().scale()) — exact BigDecimal, never a Double parse; DivisibilityService.check reads the CURRENT MaterialDivisibilityPolicy, computes a CheckVerdict (INTEGER_ONLY → ACCEPTED if integral else NON_INTEGRAL; FRACTIONAL → ACCEPTED if effectiveScale <= maxScale else EXCESS_PRECISION), records an immutable DivisibilityCheck carrying the verdict + the policy version in force + the submitted quantity VERBATIM, and on a rejection throws DivisibilityException.nonIntegral / .excessPrecision (422, naming the material) — NEVER rounding the quantity; the @Transactional carries noRollbackFor = DivisibilityException so the recorded rejection survives the 422; declare appends the next per-material policy version under a PESSIMISTIC_WRITE lock (uq(material_ref, policy_version)); columns are max_scale / submitted_quantity (never value / limit / order); NO delete path exists"
upstream:
  - "https://cwe.mitre.org/data/definitions/682.html"
  - "https://cwe.mitre.org/data/definitions/1339.html"
  - "https://en.wikipedia.org/wiki/Continuous_or_discrete_variable"
evidence:
  - source_type: external
    citation: "CWE-682: Incorrect Calculation — MITRE (the integrality/scale test must be EXACT BigDecimal arithmetic, not a lossy Double parse or a round that silently changes the quantity)"
    url: "https://cwe.mitre.org/data/definitions/682.html"
    quote: "The product performs a calculation that generates incorrect or unintended results that are later used in security-critical decisions or resource management."
    quoted_at: "2026-06-24"
  - source_type: external
    citation: "CWE-1339: Insufficient Precision or Accuracy of a Real Number — MITRE (a quantity carrying more fractional precision than the material's unit of measure admits is the over-precise real number the EXCESS_PRECISION gate refuses)"
    url: "https://cwe.mitre.org/data/definitions/1339.html"
    quote: "The product processes a real number with an implementation in which the number's representation does not preserve required accuracy and precision in its fractional part, causing an incorrect result."
    quoted_at: "2026-06-24"
  - source_type: external
    citation: "Continuous or discrete variable — Wikipedia (the discrete-vs-continuous distinction the INTEGER_ONLY / FRACTIONAL policy generalizes: a discrete quantity takes only countable integer values; a continuous quantity takes any value between two values)"
    url: "https://en.wikipedia.org/wiki/Continuous_or_discrete_variable"
    quote: "Common examples are variables that must be integers, non-negative integers, positive integers, or only the integers 0 and 1."
    quoted_at: "2026-06-24"
---

## A material's divisibility constraint REJECTS an unrepresentable quantity — it never silently rounds it

**Impact: HIGH — a gate that silently rounds a fractional quantity to a whole unit, or tests integrality with float arithmetic, corrupts the requirement without an audit trail (CWE-682); a gate that truncates excess fractional digits silently changes the amount (CWE-1339).**

Every item master answers a question BEFORE it quantizes: **may this material be transacted in a fractional amount at all, and if so to what precision?** A discrete item — a motor, a license, a unit of stock — is **INTEGER_ONLY**: it takes only whole-unit quantities (the discrete-variable case: *"Common examples are variables that must be integers, non-negative integers, positive integers, or only the integers 0 and 1."*). Bulk liquid, powder, or sheet stock is **FRACTIONAL**: any positive amount, bounded by a maximum decimal scale (the continuous-variable case). This is a **REJECT gate**, the deliberate opposite of the catalog's round-UP-to-a-lot-multiple quantizer (order-multiple-quantization):

```text
INTEGER_ONLY material, quantity 2.5  ->  422 NON_INTEGRAL_QUANTITY (naming the material)   — NOT rounded to 3
FRACTIONAL material maxScale=3, qty 1.2345  ->  422 EXCESS_PRECISION                        — NOT truncated to 1.234
integrality test:  stripTrailingZeros().scale() <= 0   — 5, 5.0, 5.00 integral; 5.5 not    — EXACT, format-independent
policy:            recorded, versioned per material (a re-declaration appends a version)
check:             recorded immutably with the verdict + the policy version in force
```

**1. INTEGER_ONLY rejects a fractional quantity — it never rounds (DIV-INTEGRAL-001).** Rounding 2.5 to 3 would silently corrupt the requirement; rounding a requirement up to a *placeable lot* is the quantizer's job, on a quantity that is already representable. This gate refuses the unrepresentable quantity and names the material so the caller can fix the input.

**2. FRACTIONAL enforces a maximum decimal scale — it never truncates (DIV-PRECISION-001).** A quantity above the material's recorded `maxScale` is 422 EXCESS_PRECISION; a quantity at or under it (including a whole quantity, scale 0) is accepted. The over-precise digits are not silently dropped (CWE-1339).

**3. The test is EXACT and format-independent (DIV-DETERMINISM-001).** Integrality is `stripTrailingZeros().scale() <= 0` and the effective scale is `Math.max(0, stripTrailingZeros().scale())` — exact `BigDecimal`, so `5`, `5.0` and `5.00` are all integral and `1.250` has effective scale `2`. A `Double` parse would be the incorrect calculation CWE-682 warns against.

**4. The policy is recorded and versioned; every check is recorded (DIV-POLICY/RECORD-001).** A re-declaration appends a new per-material version (the prior is retained); each check records the verdict + the policy version in force + the submitted quantity verbatim, so any acceptance or rejection is reconstructible.

**Incorrect — silently rounds the fractional quantity, and tests integrality with a lossy double:**

```java
public long check(String materialRef, double quantity) {     // ❌ double — 0.1+0.2 != 0.3 (CWE-682)
    if (isIntegerOnly(materialRef)) {
        return Math.round(quantity);                          // ❌ SILENTLY ROUNDS 2.5 -> 3 — corrupts the requirement,
    }                                                          //    no rejection, no audit, caller never learns
    return (long) quantity;                                   // ❌ truncates the fraction away — drops precision
}
```

**Correct — exact BigDecimal integrality/scale test, REJECT (422) not round, recorded verbatim:**

```java
// DivisibilityArithmetic — the exact, format-independent test the domain turns on
static boolean isIntegral(BigDecimal q) {
    return q.stripTrailingZeros().scale() <= 0;               // ✅ 5, 5.0, 5.00 integral; 5.5 not
}
static int effectiveScale(BigDecimal q) {
    return Math.max(0, q.stripTrailingZeros().scale());       // ✅ 1.250 -> 2, 5 -> 0
}

// DivisibilityService — REJECT-not-round, record the verdict + the policy version in force
@Transactional(noRollbackFor = DivisibilityException.class)   // ✅ the recorded rejection survives the 422
public DivisibilityCheck check(String materialRef, BigDecimal quantity) {
    MaterialDivisibilityPolicy policy = policies.findCurrent(materialRef)
        .orElseThrow(DivisibilityException::notFound);
    CheckVerdict verdict = switch (policy.getPolicyKind()) {
        case INTEGER_ONLY -> DivisibilityArithmetic.isIntegral(quantity)
            ? CheckVerdict.ACCEPTED : CheckVerdict.NON_INTEGRAL;
        case FRACTIONAL -> DivisibilityArithmetic.effectiveScale(quantity) <= policy.getMaxScale()
            ? CheckVerdict.ACCEPTED : CheckVerdict.EXCESS_PRECISION;
    };
    DivisibilityCheck recorded = members.persist(new DivisibilityCheck(UUID.randomUUID(),
        materialRef, quantity, verdict, policy.getPolicyVersion(), Instant.now(clock)));   // ✅ verbatim
    return switch (verdict) {
        case NON_INTEGRAL -> throw DivisibilityException.nonIntegral(materialRef, quantity.toPlainString());
        case EXCESS_PRECISION -> throw DivisibilityException.excessPrecision(
            materialRef, quantity.toPlainString(), policy.getMaxScale());
        case ACCEPTED -> recorded;                            // ✅ the quantity is NEVER changed
    };
}
```

The `MaterialDivisibilityPolicy` row is immutable (every `@Column(updatable=false)`) with a `@Check` that binds `policy_version >= 1 AND max_scale >= 0 AND (policy_kind = 'FRACTIONAL' OR max_scale = 0)`, and a re-declaration appends the next version under a `PESSIMISTIC_WRITE` lock (`uq(material_ref, policy_version)`). The columns are `max_scale` / `submitted_quantity` (never `value` / `limit` / `order`); no delete path exists.

Verification: review-tier — confirm the integrality test is `stripTrailingZeros().scale() <= 0` (not a `Double` parse or string length), the service throws 422 (naming the material) rather than rounding, the submitted quantity is recorded verbatim and the verdict carries the policy version in force, and the `@Transactional` carries `noRollbackFor` so a recorded rejection is not rolled back. The behavioural proof a fork-receiver keeps green: INTEGER_ONLY material with `2.5 → 422 NON_INTEGRAL_QUANTITY` (response does NOT echo a rounded 3) and `3.00 → ACCEPTED`; FRACTIONAL maxScale=3 with `1.2345 → 422 EXCESS_PRECISION` and `1.250 → ACCEPTED`.

Reference: [CWE-682: Incorrect Calculation](https://cwe.mitre.org/data/definitions/682.html)

Reference: [CWE-1339: Insufficient Precision or Accuracy of a Real Number](https://cwe.mitre.org/data/definitions/1339.html)

Reference: [Continuous or discrete variable — Wikipedia](https://en.wikipedia.org/wiki/Continuous_or_discrete_variable)
