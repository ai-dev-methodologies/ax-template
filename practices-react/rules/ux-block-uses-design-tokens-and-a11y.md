---
title: "Codified UX blocks must use semantic design tokens (no hardcoded hex/palette), semantic HTML with role/aria for stateful UI, and typed string-literal variant props"
rule_id: ux-block-uses-design-tokens-and-a11y
impact: HIGH
impactDescription: "A third-party UI block imported verbatim (e.g. a 21st.dev component) ships hardcoded hex (text-[#EAA65D]), non-semantic markup (an <h1> used as a status pill), an inline all-variant demo with no props, and no a11y — so it cannot be themed (no dark/brand), breaks screen-reader semantics, and is not reusable; codifying it into an ax block normalizes all four."
tags:
  - ux
  - design-tokens
  - accessibility
  - a11y
  - theming
  - codification
  - l2-blocks
applicable_to:
  - react
  - nextjs
  - vite
provenance_class: internal_design
protects_template_id: templates/L2/blocks/status-badge.tsx
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-UX-001"
verification:
  type: review
  notes: |
    Review-tier (a planned ax/no-raw-color ESLint rule is the static enforcer). A reviewer confirms a
    codified templates/L2/blocks/* component: (1) references colors ONLY via design tokens — grep for hex
    literals `#[0-9a-fA-F]{3,6}` and Tailwind raw-palette arbitrary values `[#...]` returns zero in the
    component; (2) stateful/announcing UI carries the correct role + aria (a status pill is
    `role="status"` with an aria-label, never an `<h1>`; the decorative icon is `aria-hidden`);
    (3) variants are a typed string-literal union (ax forbids enum), surfaced as a single parameterized
    component, not an inline demo of every variant.
evidence:
  - source_type: external
    anchors: generic_principle_only
    citation: "WCAG 2.2 SC 1.4.1 Use of Color: color is not used as the only visual means of conveying information; pairing each status with an icon plus a text label (not color alone) satisfies it."
    url: "https://www.w3.org/WAI/WCAG22/Understanding/use-of-color.html"
    quoted_at: "2026-06-03"
  - source_type: external
    anchors: generic_principle_only
    citation: "WAI-ARIA 1.2 status role: a type of live region whose content is advisory information for the user that is not important enough to justify an alert; assistive technologies announce its changes."
    url: "https://www.w3.org/TR/wai-aria-1.2/#status"
    quoted_at: "2026-06-03"
  - source_type: external
    anchors: generic_principle_only
    citation: "MDN CSS custom properties (--*): define design tokens once and reference them with var(); a single token swap re-themes every consumer (light/dark/brand) without touching component code."
    url: "https://developer.mozilla.org/en-US/docs/Web/CSS/Using_CSS_custom_properties"
    quoted_at: "2026-06-03"
decided_at: "2026-06-03"
---

## Codified UX blocks must use design tokens, semantic+a11y markup, and typed variant props

**Impact: HIGH — a third-party component dropped in verbatim can't be themed, breaks screen readers, and isn't reusable.** When a 21st.dev (or any community) component is codified into `templates/L2/blocks/`, the import is normalized to four ax invariants so it behaves like first-party ax code. This rule governs `templates/L2/blocks/status-badge.tsx`, the worked example of the transform.

**Incorrect — the raw import (status pill):**

```tsx
// VIOLATION: hardcoded hex; <h1> as a badge; inline demo of every variant; no props; no a11y
<div className="w-40 h-[35px] flex items-center justify-center bg-orange-50 rounded-xl">
  <h1 className="flex items-center text-[#EAA65D] font-semibold">
    <TriangleAlert className="w-4 h-4 mr-2" strokeWidth={3} />
    Pending
  </h1>
</div>
```

**Correct — codified ax block (one typed, tokenized, accessible component):**

```tsx
export type StatusKind = 'pending' | 'failed' | 'success' | 'in_progress' | 'in_review' | 'expired' | 'submitted'
// status -> { label, Icon, token }; token is the ONLY color reference.
export function StatusBadge({ status, label }: StatusBadgeProps) {
  const spec = STATUS[status]
  return (
    <span role="status" aria-label={label ?? spec.label} data-status={status}
      style={{ color: `var(--ax-status-${spec.token}-fg)`, background: `var(--ax-status-${spec.token}-bg)` }}>
      <spec.Icon aria-hidden="true" strokeWidth={3} />
      {label ?? spec.label}
    </span>
  )
}
```

The four normalizations every codification applies:

1. **Design tokens, not hardcoded color** — `var(--ax-status-<token>-fg/bg)` instead of `text-[#EAA65D]` / `bg-orange-50`. One theme swap re-skins every consumer (light/dark/brand).
2. **Semantic HTML + a11y** — a status pill is `role="status"` with an `aria-label`, never an `<h1>`; the decorative icon is `aria-hidden`. State is conveyed by icon **and** text, not color alone (WCAG SC 1.4.1).
3. **Typed string-literal variants** — a `StatusKind` union (ax forbids `enum`), surfaced as one parameterized `<StatusBadge status=…/>` instead of an inline demo listing every variant.
4. **Reusable props contract** — typed `StatusBadgeProps`, with an optional `label` override that is still announced to assistive tech.

This is the unit transform behind the design-decision tooling in `practices/scripts/design-decision/` (which scores a crawled component catalog and emits, per pick, exactly which of these four normalizations a codification must apply).

Reference: [WCAG 2.2 SC 1.4.1 Use of Color](https://www.w3.org/WAI/WCAG22/Understanding/use-of-color.html) · [WAI-ARIA status role](https://www.w3.org/TR/wai-aria-1.2/#status) · [MDN CSS custom properties](https://developer.mozilla.org/en-US/docs/Web/CSS/Using_CSS_custom_properties)
