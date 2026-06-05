---
title: "Billing UI status badges must pair semantic color with a text label, and pricing tables must use accessible headers (scope=col) + ARIA-labeled CTAs"
rule_id: billing-frontend-status-color-and-table-a11y
impact: MEDIUM
impactDescription: "A subscription status conveyed by color alone (green=ACTIVE, yellow=PAST_DUE) is invisible to color-blind and screen-reader users; a pricing table without scope='col' headers or ARIA-labeled tier CTAs is unnavigable by assistive tech — a screen-reader user cannot tell which 'Subscribe' button belongs to which plan. The billing surface drives purchase decisions; its status and comparison UI must be accessible."
tags:
  - billing
  - frontend
  - accessibility
  - a11y
  - status-badge
  - tables
applicable_to:
  - react
  - nextjs
spec_ref: "specs/billing-frontend-l0.yaml#BILLING-FE-002"
verification:
  type: review
  notes: |
    Reviewer confirms the billing UI against specs/billing-frontend-l0.yaml: subscription status badges map
    the correct semantic color (TRIAL→blue, ACTIVE→green, PAST_DUE→yellow, CANCELLED→gray) AND carry a
    text label — never color alone (BILLING-FE-002, WCAG 1.4.1). PricingTable and PlanComparison use
    accessible table headers (scope='col') and ARIA labels on plan-tier CTAs so each 'Subscribe' button is
    associated with its plan (BILLING-FE-003, WCAG 1.3.1). (Currency display is BILLING-FE-001, governed by
    currency-amount-no-raw-jsx-render; the billing↔payment module boundary is BILLING-FE-004.)
evidence:
  - source_type: external
    citation: "WCAG 2.2 Success Criterion 1.4.1 Use of Color (Level A) — a status badge must not rely on color alone; pair color with a text label (BILLING-FE-002)"
    url: "https://www.w3.org/WAI/WCAG22/Understanding/use-of-color.html"
    quote: "Color is not used as the only visual means of conveying information, indicating an action, prompting a response, or distinguishing a visual element."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "WCAG 2.2 Success Criterion 1.3.1 Info and Relationships (Level A) — pricing table headers (scope=col) + ARIA-labeled CTAs make structure programmatically determinable (BILLING-FE-003)"
    url: "https://www.w3.org/WAI/WCAG22/Understanding/info-and-relationships.html"
    quote: "Information, structure, and relationships conveyed through presentation can be programmatically determined or are available in text."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## Billing UI status badges must pair color with a text label, and pricing tables must use accessible headers + ARIA-labeled CTAs

**Impact: MEDIUM — The billing surface drives money decisions, so its accessibility is not optional. A subscription status shown only by color — green for ACTIVE, yellow for PAST_DUE — is invisible to a color-blind user and silent to a screen reader; WCAG 1.4.1 is explicit that *color is not used as the only visual means of conveying information, indicating an action, prompting a response, or distinguishing a visual element*. A pricing comparison table without `scope='col'` headers and ARIA-labeled tier CTAs is unnavigable by assistive technology — WCAG 1.3.1 requires that *information, structure, and relationships conveyed through presentation can be programmatically determined or are available in text*, so a screen-reader user can tell which 'Subscribe' button belongs to which plan and what each cell means.**

There are two load-bearing requirements here (BILLING-FE-001 currency → `currency-amount-no-raw-jsx-render`; BILLING-FE-004 module boundary → its own boundary rule).

**Status badge color + label (BILLING-FE-002).** Subscription status badges map the correct semantic color — TRIAL→blue, ACTIVE→green, PAST_DUE→yellow, CANCELLED→gray — AND carry a text label, so the status is conveyed by more than color alone (WCAG 1.4.1).

**Accessible pricing tables (BILLING-FE-003).** PricingTable and PlanComparison use accessible table headers (`scope='col'`) and ARIA labels on plan-tier CTAs, so the table's structure and each CTA's plan association are programmatically determinable (WCAG 1.3.1).

**Incorrect — color-only status; pricing table with no header scope and an ambiguous CTA:**

```tsx
<span className={statusColor(status)} />                  {/* VIOLATION: color alone, no label (BILLING-FE-002) */}
<table><tr><th>Free</th><th>Pro</th></tr>               {/* VIOLATION: no scope='col' (BILLING-FE-003) */}
  <button>Subscribe</button></table>                     {/* VIOLATION: CTA not associated with a plan (no ARIA label) */}
```

**Correct — color + text label status; scope=col headers + ARIA-labeled CTAs:**

```tsx
<StatusBadge color={STATUS_COLOR[status]}>{STATUS_LABEL[status]}</StatusBadge>   {/* color + label (BILLING-FE-002) */}
<table>
  <thead><tr><th scope="col">Free</th><th scope="col">Pro</th></tr></thead>      {/* scope=col (BILLING-FE-003) */}
  <tbody>...<button aria-label="Subscribe to Pro plan">Subscribe</button>...</tbody>
</table>
```

Verification: review-tier. Billing a11y is an accessibility property with no compile signal — a color-only badge and an unscoped table render fine and exclude real users. Verify by review against `specs/billing-frontend-l0.yaml`: status badges pair the correct semantic color with a text label; pricing tables use `scope='col'` headers and ARIA-labeled tier CTAs. When a fork-receiver wires an axe/a11y test (status has a non-color label; table headers are scoped; CTAs are labeled), this rule's verification may be upgraded from review to a test-tag binding.

Reference: [WCAG 2.2 — Use of Color (1.4.1)](https://www.w3.org/WAI/WCAG22/Understanding/use-of-color.html)

Reference: [WCAG 2.2 — Info and Relationships (1.3.1)](https://www.w3.org/WAI/WCAG22/Understanding/info-and-relationships.html)
