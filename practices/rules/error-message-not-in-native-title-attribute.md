---
title: Mutation error messages MUST NOT render in the native `title` tooltip
impact: MEDIUM
impactDescription: "Native title tooltips appear in screenshots, screencasts, and over-the-shoulder views — server prose surfaced there can leak incidental PII or internal product names"
tags:
  - error-handling
  - a11y
  - pii-side-channel
  - aria-live
spec_ref: "specs/favorites-bookmarks-l0.yaml#FAV-CRUD-001"
verification:
  type: review
  source: "templates/L4/favorites-bookmarks/app/favorite-toggle.tsx"
  pattern: "title={ariaLabel} only; error.message rendered in a separate role='alert' aria-live span next to the button"
upstream:
  - "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "WCAG 2.2 — Success Criterion 4.1.3 Status Messages (Level AA)"
    url: "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
    quote: "In content implemented using markup languages, status messages can be programmatically determined through role or properties such that they can be presented to the user by assistive technologies without receiving focus."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "OWASP ASVS V8.3 — Sensitive Private Data"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quote: "Verify that sensitive information is not transmitted via URL parameters or hidden form fields and is sanitized or removed when no longer required."
    quoted_at: "2026-05-25"
---

## Mutation error messages MUST NOT render in the native `title` tooltip

**Impact: MEDIUM — native tooltips are a quiet side-channel for incidental information leaks**

The native `title` attribute on a button or link renders on hover, persists in the DOM, appears in screenshots and screencasts, and shows over-the-shoulder during screenshare. It is also non-dismissable — once it appears the user has no way to clear it short of moving the mouse. When the value is a server-emitted error message, that message reaches every surface that re-captures the page.

Server error messages often carry information the catalog's PII deny-list cannot fully scrub: internal product names (`Subscription tier "enterprise"`), billing URLs, role / tenant / subscription identifiers, partial stack-trace excerpts, queue identifiers, vendor product codes. The catalog's `parse-error.ts` deny-list catches the most-dangerous PII shapes (email, IP, JWT, PEM headers, internal hostnames, Korean RRN + mobile) but cannot enumerate every operator's product vocabulary. The right answer is to keep error prose out of the `title` slot entirely.

The replacement surface is an inline `role="alert"` span (an ARIA live region). Sighted users see the error next to the action that produced it; screen-reader users hear it announced via aria-live without taking focus; the native tooltip retains a stable, public-safe value (the button's `aria-label`).

**Incorrect — mutation error falls back into `title`, leaks via every screenshot and screenshare:**

```tsx
<button
  type="button"
  aria-label={ariaLabel}
  title={
    toggle.error
      ? toggle.error.message              // ❌ server prose lands in the native tooltip
      : ariaLabel
  }
  onClick={() => toggle.mutate()}
>
  ★
</button>
```

**Correct — title carries the aria-label only; errors render in a separate role='alert' span:**

```tsx
<>
  <button
    type="button"
    aria-label={ariaLabel}
    title={ariaLabel}                     // ✅ Public-safe stable value
    aria-busy={toggle.isPending || undefined}
    aria-disabled={busy || undefined}
    onClick={() => {
      if (busy) return
      toggle.mutate(...)
    }}
  >
    ★
  </button>
  {(toggle.error || error) && (
    <span role="alert" className="ml-1 text-xs text-red-700">
      {(toggle.error ?? (error as Error)).message}
    </span>
  )}
</>
```

Two follow-on patterns travel with this rule:
- Use `aria-busy` + `aria-disabled` instead of native `disabled` while a mutation is in flight (separate rule). Native `disabled` removes the element from the tab order mid-flight; `aria-busy` preserves focus and lets the screen reader announce the busy state.
- Allow the user to dismiss a sticky error banner. TanStack Query mutation errors do NOT auto-clear when the next `mutate()` succeeds — they require an explicit `mutation.reset()`. Pair the alert with a Dismiss button that calls `.reset()`.

Reference: [WCAG 2.2 SC 4.1.3 — Status Messages](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html)

Reference: [OWASP ASVS V8 — Sensitive Private Data](https://owasp.org/www-project-application-security-verification-standard/)
