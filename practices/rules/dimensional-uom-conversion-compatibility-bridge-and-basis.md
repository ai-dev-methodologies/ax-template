---
title: A cross-dimension unit conversion must enforce a DIMENSIONAL-COMPATIBILITY precondition (same-dimension ⇒ pure ratio; cross-dimension ⇒ a recorded versioned bridging material property, else 422 INCOMPATIBLE_DIMENSIONS — never a silent wrong number), record its full reconstructible basis (from-quantity/unit, to-unit, dimension verdict, factor, material version, result), and be deterministic BigDecimal arithmetic at a recorded scale
impact: HIGH
impactDescription: "Converting between units of DIFFERENT physical dimensions (volume L ↔ mass kg, count ↔ mass) is NOT a pure unit ratio — it requires a material property (density / unit-weight). A converter that applies a bare ratio across dimensions returns a wrong number (CWE-682 Incorrect Calculation): a recipe written in litres consumed against stock kept in kg silently mis-decrements inventory, a BOM explodes the wrong mass, a goods-in count is weighed wrong. The catalog already rounds quantities WITHIN one dimension (order-multiple-quantization) but had no primitive for the cross-dimension bridge; without the precondition the system cannot even tell an admissible conversion from a nonsensical one, and without a recorded versioned basis an already-computed conversion cannot be reconciled to the density that produced it"
tags:
  - validation
  - audit
  - governance
  - billing
  - state-machine
spec_ref: "specs/dimensional-uom-conversion-l0.yaml#UOMCONV-COMPAT-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/uomconversion/UomConversionService.java + backend/src/main/java/com/ax/template/authblueprint/uomconversion/Unit.java + backend/src/main/java/com/ax/template/authblueprint/uomconversion/Material.java + backend/src/main/java/com/ax/template/authblueprint/uomconversion/Conversion.java"
  pattern: "A conversion first classifies the from/to units' Dimension; a SAME_DIMENSION conversion applies the pure in-dimension ratio; a CROSS_DIMENSION conversion is admissible ONLY with a recorded bridging Material property for the (from-dim → to-dim) pair, else 422 INCOMPATIBLE_DIMENSIONS naming both dimensions (an unknown unit → 422 UNKNOWN_UNIT); the bridging factor is the material's recorded BigDecimal density/unit-weight (mass = volume × density), VERSIONED append-only so a correction is a new immutable version and an old conversion keeps citing its version; every conversion persists an immutable basis (from-quantity, from-unit, to-unit, from/to-dimension, mode, factor, material version, result scale, result) and is deterministic + idempotent (BigDecimal HALF_UP at a recorded scale; an identical re-request returns the recorded conversion verbatim); NO delete path exists"
upstream:
  - "https://www.nist.gov/pml/special-publication-811/nist-guide-si-chapter-7-rules-and-style-conventions-expressing-values"
  - "https://cwe.mitre.org/data/definitions/682.html"
evidence:
  - source_type: external
    citation: "NIST Special Publication 811, Guide for the Use of the International System of Units (SI), §7.1 — the value of a quantity is a number times a unit, so a conversion across dimensions cannot be a bare number scaling; it must pass through a property relating the two dimensions"
    url: "https://www.nist.gov/pml/special-publication-811/nist-guide-si-chapter-7-rules-and-style-conventions-expressing-values"
    quote: "The value of a quantity is its magnitude expressed as the product of a number and a unit, and the number multiplying the unit is the numerical value of the quantity expressed in that unit."
    quoted_at: "2026-06-24"
  - source_type: external
    citation: "NIST Special Publication 811, Guide for the Use of the International System of Units (SI), §7.14 — the dimension of a quantity is a structured exponent over the base quantities, so two units share a dimension iff their dimensional exponents match; a pure unit ratio is meaningful ONLY within one dimension"
    url: "https://www.nist.gov/pml/special-publication-811/nist-guide-si-chapter-7-rules-and-style-conventions-expressing-values"
    quote: "The dimension of Q is defined to be dim Q = L_α_M_β_T_γ_I_δ_θ_ε_N_ζ_J_η_."
    quoted_at: "2026-06-24"
  - source_type: external
    citation: "CWE-682: Incorrect Calculation — MITRE (a cross-dimension conversion done as a bare unit ratio is a wrong-result calculation)"
    url: "https://cwe.mitre.org/data/definitions/682.html"
    quote: "The product performs a calculation that generates incorrect or unintended results that are later used in security-critical decisions or resource management."
    quoted_at: "2026-06-24"
---

## Converting between dimensions is a material-property bridge, not a unit ratio — a same-dimension ratio applied across dimensions is a wrong number

**Impact: HIGH — a cross-dimension conversion done as a bare ratio (volume→mass without a density) returns a wrong number (CWE-682); a recipe in litres consumed against stock in kg silently mis-decrements inventory; an already-recorded conversion with no versioned basis cannot be reconciled to the density that produced it.**

Per NIST SP 811 §7.1, *"the value of a quantity is its magnitude expressed as the product of a number and a unit"*, and §7.14 defines *"the dimension of Q … dim Q = L_α_M_β_T_γ_I_δ_θ_ε_N_ζ_J_η_"* — a structured exponent over the base quantities. Two units are inter-convertible by a **pure ratio only when they share a dimension** (kg→g, L→mL). Converting between **different** dimensions (volume L ↔ mass kg, count ↔ mass) is *not* a unit ratio at all: it requires a **material property** (a density, mass per unit volume; or a unit-weight, mass per unit count) that relates the two dimensions — `mass = volume × density`.

The catalog already rounds quantities **within** one dimension (`order-multiple-quantization` ceils a quantity up to a MOQ/lot multiple — mass→mass, count→count, no dimension change). It had **no** primitive for the **cross-dimension** conversion a BOM/recipe/packing system runs constantly. This rule is that primitive, and it is DISTINCT from the same-dimension quantizer: the quantizer never changes dimension and needs no material; this rule's whole job is the dimensional-compatibility precondition and the recorded bridge.

```text
classify:   dim(fromUnit), dim(toUnit) from a fixed Dimension on each Unit
same-dim:   apply the pure in-dimension ratio (kg→g = ×1000) — no material needed
cross-dim:  admissible ONLY with a recorded bridging Material property for
            (fromDim → toDim); factor = material density/unit-weight (VERSIONED);
            mass = volume-in-base × density; else → 422 INCOMPATIBLE_DIMENSIONS
record:     every conversion persists its basis (from-qty/unit, to-unit, dims,
            mode, factor, material version, scale, result) — reconstructible
arithmetic: BigDecimal, recorded scale, HALF_UP — deterministic + idempotent
```

**Incorrect — a bare ratio applied regardless of dimension (CWE-682), an unversioned density, no recorded basis:**

```java
public BigDecimal convert(BigDecimal qty, String fromUnit, String toUnit) {
    double from = RATIO.get(fromUnit);          // ❌ no dimension check — L and kg both "have a ratio"
    double to   = RATIO.get(toUnit);            // ❌ double arithmetic — not deterministic
    return BigDecimal.valueOf(qty.doubleValue() * from / to);  // ❌ 2 L → 2 kg, a wrong number
    // ❌ no material property, no version, no recorded basis — unreconcilable
}
```

**Correct — the dimensional-compatibility precondition, a versioned material bridge, a recorded reconstructible basis:**

```java
@Transactional
public Conversion convert(UUID materialId, BigDecimal fromQuantity, String fromUnitCode,
                          String toUnitCode, String actor) {
    Unit from = Unit.byCode(fromUnitCode).orElseThrow(() -> UomConversionException.unknownUnit(fromUnitCode));
    Unit to = Unit.byCode(toUnitCode).orElseThrow(() -> UomConversionException.unknownUnit(toUnitCode));
    Dimension fromDim = from.dimension();
    Dimension toDim = to.dimension();

    if (fromDim == toDim) {                                   // ✅ same dimension ⇒ pure in-dimension ratio
        return sameDimension(fromQuantity, from, to, actor);
    }
    // ✅ cross dimension ⇒ a recorded bridging material property is REQUIRED
    Material material = materials.findByIdForUpdate(materialId)
        .orElseThrow(() -> UomConversionException.incompatibleDimensions(fromDim, toDim));
    MaterialProperty bridge = currentBridge(material, fromDim, toDim)
        .orElseThrow(() -> UomConversionException.incompatibleDimensions(fromDim, toDim)); // ✅ 422, never a wrong number
    return crossDimension(material, bridge, fromQuantity, from, to, actor);   // mass = volume × density
}
```

```java
private Conversion crossDimension(Material material, MaterialProperty bridge, BigDecimal fromQuantity,
                                  Unit from, Unit to, String actor) {
    BigDecimal fromInBase = fromQuantity.multiply(from.toBaseFactor());      // to the from-dimension base unit
    BigDecimal toBase = fromInBase.multiply(bridge.getFactor());             // ✅ bridge: e.g. volume×density = mass
    BigDecimal result = toBase.divide(to.toBaseFactor(), RESULT_SCALE, RoundingMode.HALF_UP); // ✅ deterministic
    Conversion c = new Conversion(UUID.randomUUID(), material.getId(), fromQuantity, from, to,
        Conversion.Mode.CROSS_DIMENSION, bridge.getFactor(), bridge.getVersion(), RESULT_SCALE, result,
        idempotencyBasis(material.getId(), fromQuantity, from, to, bridge.getVersion()),
        Instant.now(clock), actor);
    return idempotentRecord(c);                                              // ✅ identical re-request → recorded verbatim
}
```

The dimensional-compatibility precondition makes an inadmissible conversion a deterministic `422 INCOMPATIBLE_DIMENSIONS` (naming both dimensions) instead of a silently wrong number; the bridging factor is the material's recorded BigDecimal density/unit-weight, **versioned append-only** so a correction is a new immutable version and an already-recorded conversion keeps citing the version it used. Every conversion persists its full basis (`from-quantity`, `from-unit`, `to-unit`, both dimensions, mode, factor, material version, result scale, result) so the result is re-derivable, and the arithmetic is `BigDecimal` at a recorded scale with `HALF_UP` so it is deterministic and idempotent. `Conversion` rows are `@AggregateMember` of `Material` — root-JPQL reads, `common/MemberWriter` writes; no delete path exists.

Verification: review-tier — confirm the conversion classifies the from/to `Dimension` before any arithmetic, a same-dimension conversion applies the pure ratio with no material, a cross-dimension conversion REQUIRES a recorded bridge (else 422 naming both dimensions), the bridge factor is a recorded versioned BigDecimal (append-only, prior versions preserved), every conversion records its reconstructible basis, and the arithmetic is BigDecimal HALF_UP at a recorded scale (no double). The behavioural proof a fork-receiver keeps green: a cross-dimension request with no material is 422 INCOMPATIBLE_DIMENSIONS, with a material it bridges via mass = volume × density, and an identical re-request returns the recorded conversion verbatim.

Reference: [NIST SP 811 §7 — Rules and Style Conventions for Expressing Values of Quantities](https://www.nist.gov/pml/special-publication-811/nist-guide-si-chapter-7-rules-and-style-conventions-expressing-values)

Reference: [CWE-682: Incorrect Calculation](https://cwe.mitre.org/data/definitions/682.html)
