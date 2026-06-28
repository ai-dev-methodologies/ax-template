---
title: Order-level tax application must (1) SKIP every declared-exempt scope — a tax-exempt customer or a tax-exempt line contributes ZERO to the non-exempt taxable base, so a fully-exempt order has total tax 0 — and (2) recompute IDEMPOTENTLY by find-existing → update-or-create-or-remove so that re-pricing converges to exactly ONE combined tax record per order whose amount == round(taxableBase × injectedRate), never duplicated and never stranded; the tax is DERIVED each time from the declared input and the injected rate, never a client-asserted amount
impact: HIGH
impactDescription: "A tax engine that re-prices without reconciling to a single record duplicates the tax row on every recompute (the order total double-counts tax) or strands a now-exempt order's old tax row (the customer is charged tax they are exempt from) — a silent business-logic correctness defect. Summing a declared-exempt line, or taxing an exempt customer, over-charges tax no jurisdiction authorized. Trusting a client-asserted tax amount lets an attacker forge the tax by editing the request. Floating-point currency or independent rounding of base and rate conjures or destroys money on every order. These are exactly the repeated-application and business-logic weaknesses anchored below — and they are portable: they hold for any order-level tax engine, independent of the (out-of-scope) jurisdiction rate table."
tags:
  - e-commerce
  - tax
  - idempotency
  - business-logic
  - money
spec_ref: "specs/tax-application-l0.yaml#TAX-IDEMPOTENT-RECOMPUTE-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/taxapplication/TaxApplicationService.java + backend/src/main/java/com/ax/template/authblueprint/taxapplication/TaxAssessment.java"
  pattern: "A single TaxApplicationService.recompute(orderId, rateBasisPoints) is the sole mutator of the derived tax record: it computes the non-exempt taxableBase via a pure taxableBase(order) that returns 0 for an exempt customer and skips every exempt line (EXEMPT-SKIP), computes tax = round(taxableBase × rate / 10000) half-up in integer minor units via a pure computeTax(base, rate) that yields 0 for a zero base, then reconciles to exactly one row by find-existing → update-or-create-or-remove: a zero base removes any existing row (never strands it), a non-zero base updates the existing row in place (identity preserved) or creates the first one; the TaxAssessment @AggregateRoot carries UNIQUE(order_id) so a second tax row per order is unrepresentable, @Version, @Check(tax_amount_minor >= 0 AND taxable_base_minor >= 0 AND rate_basis_points >= 0), immutable @Column(updatable=false) id/order_id and no public setter; the amount is never read from the request"
upstream:
  - "https://www.rfc-editor.org/rfc/rfc9110#section-9.2.2"
  - "https://cwe.mitre.org/data/definitions/840.html"
evidence:
  - source_type: external
    citation: "RFC 9110: HTTP Semantics, §9.2.2 Idempotent Methods — IETF"
    url: "https://www.rfc-editor.org/rfc/rfc9110#section-9.2.2"
    quote: "A request method is considered \"idempotent\" if the intended effect on the server of multiple identical requests with that method is the same as the effect for a single such request."
    quoted_at: "2026-06-28"
  - source_type: external
    citation: "CWE-840: Business Logic Errors — MITRE Common Weakness Enumeration"
    url: "https://cwe.mitre.org/data/definitions/840.html"
    quote: "Weaknesses in this category identify some of the underlying problems that commonly allow attackers to manipulate the business logic of an application. Errors in business logic can be devastating to an entire application."
    quoted_at: "2026-06-28"
---

## Rule

Order-level tax **application** (does the order owe tax, and how much, given an injected rate) has two separable, portable correctness invariants. The jurisdiction **rate** is supplied as input — a rate table / nexus rules and any external tax provider are deliberately **out of scope**, so the invariants hold for any tax engine.

1. **EXEMPT-SKIP.** The non-exempt taxable base excludes every **declared-exempt** scope: a tax-exempt customer makes the order's taxable base `0`; each tax-exempt line contributes `0`; only non-exempt, positive line bases are summed. A fully-exempt order has total tax `0`. Exemption is a **declared** property of the customer/line — never inferred from amounts, never a client-asserted tax figure.
2. **IDEMPOTENT-RECOMPUTE.** Re-pricing reconciles the order's tax to a **single derived record** by `find-existing → update-or-create-or-remove`: if the order is taxable, exactly one row exists carrying `amount == round(non-exempt taxableBase × injectedRate)`; if it is not taxable, no row exists. A second row per order is **unrepresentable** (`UNIQUE(order_id)`), and a now-exempt order's prior row is **removed, not stranded**. Repeated application has the same effect on persisted state as one application.

Money is integer **minor units**; `tax = round(base × rate / 10000)` half-up. The amount is **derived** from the declared input + injected rate each time — never read from the request.

**Correct — one pure exempt-skip base, one half-up tax, one find-existing→update-or-create-or-remove convergence:**

```java
// backend/.../taxapplication/TaxApplicationService.java
static long taxableBase(TaxableOrder order) {          // EXEMPT-SKIP
    if (order.isCustomerExempt()) return 0L;           // exempt customer ⇒ 0
    return order.getLines().stream()
        .filter(l -> !l.isExempt())                    // exempt line contributes 0
        .mapToLong(TaxLine::getTaxableBaseMinor).filter(b -> b > 0).sum();
}
static long computeTax(long base, long rateBasisPoints) {
    if (base <= 0 || rateBasisPoints <= 0) return 0L;  // exempt/zero path ⇒ 0
    return Math.floorDiv(Math.multiplyExact(base, rateBasisPoints) + 5000L, 10000L); // round half-up, minor units
}

@Transactional
public TaxResult recompute(UUID orderId, long rateBasisPoints) {           // IDEMPOTENT-RECOMPUTE
    TaxableOrder order = orders.findById(orderId).orElseThrow(/* 404 */);
    long base = taxableBase(order);
    long tax = computeTax(base, rateBasisPoints);
    Optional<TaxAssessment> existing = assessments.findByOrderIdForUpdate(orderId);
    if (base == 0) {                                   // now exempt / nothing taxable
        existing.ifPresent(assessments::delete);       // remove the prior row — never strand it
        return TaxResult.none(orderId);
    }
    TaxAssessment row = existing
        .map(a -> { a.recompute(tax, base, rateBasisPoints, now()); return a; }) // UPDATE in place (id preserved)
        .orElseGet(() -> TaxAssessment.create(UUID.randomUUID(), orderId, tax, base, rateBasisPoints, now())); // CREATE
    return TaxResult.of(assessments.save(row));        // exactly one row; UNIQUE(order_id) forbids a second
}
```

**Incorrect — appends a new row each recompute, taxes exempt scope, trusts a client amount:**

```java
// WRONG: every recompute INSERTs another row — the order total double-counts tax on re-price
TaxAssessment row = new TaxAssessment(UUID.randomUUID(), orderId, req.clientTaxAmount()); // trusts request (forgeable)
assessments.save(row);                                  // no find-existing; duplicates; never removes a stranded row
// WRONG: taxes the whole base, ignoring declared exemptions
long base = order.getLines().stream().mapToLong(TaxLine::getTaxableBaseMinor).sum(); // exempt customer/line still taxed
double tax = base * (rate / 10000.0);                   // floating-point currency — conjures/destroys money
```

The incorrect version charges tax the jurisdiction never authorized: it duplicates the tax row on every re-price (non-idempotent — the persisted effect of N recomputes differs from one), leaves a now-exempt order's old row stranded, sums exempt lines, and lets the client forge the amount — exactly the repeated-application and business-logic weaknesses below. The correct version converges to one derived row and skips every exempt scope.

Reference: [RFC 9110 §9.2.2 — Idempotent Methods](https://www.rfc-editor.org/rfc/rfc9110#section-9.2.2)

Reference: [CWE-840: Business Logic Errors](https://cwe.mitre.org/data/definitions/840.html)
