---
title: An offer/discount's applicability must be decided by a single deterministic, fail-closed evaluator that reads only the offer's DECLARED criteria — a BOGO qualifier→target minimum-quantity gate AND a customer-xref/segment eligibility gate — so that unknown or missing criteria DENY BY DEFAULT (not-applied) and an ineligible offer can never reach the discount-application path; applicability (WHO/WHICH-ITEMS) is decided here, never the discount amount
impact: HIGH
impactDescription: "An eligibility predicate that fails OPEN silently applies a discount no merchant authorized (margin loss / fraud): a missing target criterion, an empty allow-list read as 'everyone', or a below-threshold qualifier treated as 'close enough' each leak the offer to an order it should never touch. Deciding applicability ad-hoc in the controller (or trusting a client-asserted eligibility flag) means the same cart resolves differently per call and an attacker forges eligibility by editing the request. Conflating applicability with the discount MATH (proration/clamp/stacking) couples two concerns that must compose, not merge — the math engine must receive only the offers that are genuinely applicable."
tags:
  - e-commerce
  - promotion
  - authorization
  - business-logic
  - fail-closed
spec_ref: "specs/offer-eligibility-l0.yaml#OFFER-FAIL-CLOSED-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/offereligibility/OfferEligibilityService.java + backend/src/main/java/com/ax/template/authblueprint/offereligibility/EligibilityOffer.java"
  pattern: "A single OfferEligibilityService.decide(offer, context) is the sole evaluator: pure (no I/O, no wall-clock, no mutation), it denies by default — a missing qualifier/target/eligibility criterion or an unresolvable customer returns a fail-closed NOT-APPLIED reason BEFORE any positive check; the customer gate is satisfied only by the customer-xref allow-list OR a matched segment; the qualifier gate sums the quantities of qualifier-matching lines and requires >= minQualifierQty plus a target line; the only applied outcome carries reason ELIGIBLE; criteria are @Column(updatable=false) on the EligibilityOffer @AggregateRoot with no public setter and an @Check(min_qualifier_qty >= 1 AND discount_basis_points >= 0); the decision computes no amount (math is promotion-l0's)"
upstream:
  - "https://cwe.mitre.org/data/definitions/636.html"
  - "https://cwe.mitre.org/data/definitions/840.html"
  - "https://cwe.mitre.org/data/definitions/285.html"
evidence:
  - source_type: external
    citation: "CWE-636: Not Failing Securely ('Failing Open') — MITRE Common Weakness Enumeration"
    url: "https://cwe.mitre.org/data/definitions/636.html"
    quote: "When the product encounters an error condition or failure, its design requires it to fall back to a state that is less secure than other options that are available, such as selecting the weakest encryption algorithm or using the most permissive access control restrictions."
    quoted_at: "2026-06-28"
  - source_type: external
    citation: "CWE-840: Business Logic Errors — MITRE Common Weakness Enumeration"
    url: "https://cwe.mitre.org/data/definitions/840.html"
    quote: "Weaknesses in this category identify some of the underlying problems that commonly allow attackers to manipulate the business logic of an application. Errors in business logic can be devastating to an entire application."
    quoted_at: "2026-06-28"
  - source_type: external
    citation: "CWE-285: Improper Authorization — MITRE Common Weakness Enumeration"
    url: "https://cwe.mitre.org/data/definitions/285.html"
    quote: "The product does not perform or incorrectly performs an authorization check when an actor attempts to access a resource or perform an action."
    quoted_at: "2026-06-28"
---

## Rule

A conditional promotion (coupon / loyalty / BOGO / cart-rule) has two separable concerns:

1. **Applicability — WHO and WHICH ITEMS the offer applies to.** This rule.
2. **Discount math — HOW MUCH.** Proration, clamping, stacking, max-uses. Owned by `promotion-l0`, which takes the *already-applicable* offers as INPUT.

Applicability must be decided by a **single deterministic, fail-closed evaluator** that reads only the offer's **declared criteria** and the order/customer context. Two independent gates:

1. **Qualifier→target minimum-quantity (BOGO).** A target line is applicable only when the sum of the quantities of lines matching the qualifier criteria is `>= minQualifierQty` *and* a target line is present. Below the threshold the target is simply **NOT applied** — this is a recorded business decision (HTTP 200), not a validation error.
2. **Customer/segment eligibility.** The offer is gated to an explicit customer-xref **allow-list** OR a matched customer **segment**. A customer in neither receives the offer as **NOT-APPLIED**.

The keystone is **fail-closed (deny by default)**: if any criterion is missing/unknown — no qualifier, no target, an empty allow-list with no segment, or an unresolvable customer — the result is NOT-APPLIED with the corresponding reason, *before* any positive check. There is no path by which a mis-declared offer reaches the discount-application path. The evaluator is pure (no I/O, no wall-clock, no mutation), so the same offer + context always yields the same recorded decision + reason. The decision computes **no amount**.

**Correct — one pure, fail-closed evaluator that denies by default and decides only applicability:**

```java
// backend/.../offereligibility/OfferEligibilityService.java
EligibilityDecision decide(EligibilityOffer offer, EvaluationContext ctx) {
    // FAIL-CLOSED: every criterion must be DECLARED, else deny by default (CWE-636).
    if (!isPresent(offer.getQualifierSku()) && !isPresent(offer.getQualifierTag()))
        return EligibilityDecision.notApplied(offer.getId(), MISSING_QUALIFIER_CRITERIA);
    if (!isPresent(offer.getTargetSku()) && !isPresent(offer.getTargetTag()))
        return EligibilityDecision.notApplied(offer.getId(), MISSING_TARGET_CRITERIA);
    if (offer.getEligibleCustomerIds().isEmpty() && !isPresent(offer.getEligibleSegment()))
        return EligibilityDecision.notApplied(offer.getId(), MISSING_ELIGIBILITY_CRITERIA);
    if (ctx == null || ctx.customerId() == null)
        return EligibilityDecision.notApplied(offer.getId(), UNKNOWN_CUSTOMER);

    // Customer/segment gate (CWE-285): allow-list OR matched segment — never a client flag.
    boolean eligible = offer.getEligibleCustomerIds().contains(ctx.customerId())
        || (isPresent(offer.getEligibleSegment()) && ctx.customerSegments().contains(offer.getEligibleSegment()));
    if (!eligible) return EligibilityDecision.notApplied(offer.getId(), CUSTOMER_NOT_ELIGIBLE);

    // BOGO qualifier→target minimum-quantity.
    long qty = ctx.lines().stream().filter(l -> matches(l, offer.getQualifierSku(), offer.getQualifierTag()))
        .mapToInt(Line::quantity).filter(q -> q > 0).sum();
    if (qty < offer.getMinQualifierQty()) return EligibilityDecision.notApplied(offer.getId(), QUALIFIER_MIN_QTY_NOT_MET);
    if (ctx.lines().stream().noneMatch(l -> matches(l, offer.getTargetSku(), offer.getTargetTag())))
        return EligibilityDecision.notApplied(offer.getId(), NO_TARGET_LINE);

    return EligibilityDecision.applied(offer.getId());   // only applied outcome: reason ELIGIBLE
}
```

**Incorrect — fails open and decides eligibility ad-hoc in the controller from a client flag:**

```java
// WRONG: missing criteria silently treated as "applies to everyone / every item" (CWE-636 failing open)
boolean apply = true;
if (offer.getEligibleCustomerIds() != null && !offer.getEligibleCustomerIds().isEmpty())
    apply = offer.getEligibleCustomerIds().contains(req.customerId());   // empty allow-list ⇒ apply stays true
// WRONG: trusts a client-asserted eligibility flag (CWE-285 — forge eligibility by editing the request)
if (req.customerSaysEligible()) apply = true;
// WRONG: no qualifier-quantity check at all — the target is discounted regardless of the cart
if (apply) applyDiscount(offer, order);   // an ineligible offer reached the discount path
```

The incorrect version applies the offer to an order the merchant never authorized: an empty allow-list reads as "everyone", a client flag forges eligibility, and the BOGO threshold is never enforced — exactly the failing-open and improper-authorization weaknesses below. The correct version denies by default and never reaches `applyDiscount` for an ineligible offer.

Reference: [CWE-636: Not Failing Securely ('Failing Open')](https://cwe.mitre.org/data/definitions/636.html)

Reference: [CWE-840: Business Logic Errors](https://cwe.mitre.org/data/definitions/840.html)

Reference: [CWE-285: Improper Authorization](https://cwe.mitre.org/data/definitions/285.html)
