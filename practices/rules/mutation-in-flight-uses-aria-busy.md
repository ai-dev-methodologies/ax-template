---
title: In-flight mutations MUST use aria-busy + aria-disabled, not native `disabled`
impact: MEDIUM
impactDescription: "Native `disabled` removes the element from the tab order mid-flight — keyboard users lose focus context and screen readers miss the busy announcement"
tags:
  - a11y
  - aria
  - keyboard-nav
  - mutation
spec_ref: "specs/favorites-bookmarks-l0.yaml#FAV-CRUD-001"
verification:
  source: "templates/L4/favorites-bookmarks/app/favorite-toggle.tsx"
  pattern: "aria-busy + aria-disabled set during isPending; onClick guards with `if (busy) return`; native `disabled` attribute NOT used for in-flight state"
upstream:
  - "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
  - "https://www.w3.org/TR/wai-aria-1.2/#aria-busy"
evidence:
  - source_type: external
    citation: "WAI-ARIA 1.2 — aria-busy property"
    url: "https://www.w3.org/TR/wai-aria-1.2/#aria-busy"
    quote: "Indicates an element is being modified and that assistive technologies MAY want to wait until the modifications are complete before exposing them to the user."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "WCAG 2.2 — Success Criterion 4.1.3 Status Messages (Level AA)"
    url: "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
    quote: "Status messages can be programmatically determined through role or properties such that they can be presented to the user by assistive technologies without receiving focus."
    quoted_at: "2026-05-25"
---

## In-flight mutations MUST use aria-busy + aria-disabled, not native `disabled`

**Impact: MEDIUM — native `disabled` is the wrong tool for transient busy state**

The HTML `disabled` attribute is for elements that are *currently not interactive*. A button that is in the middle of dispatching a mutation is conceptually busy, not disabled — the user wants it back as soon as the network round-trip completes, focus should stay on it, and assistive tech should announce "busy, please wait" rather than silently removing the element from interaction.

Native `disabled` does three problematic things during the in-flight window:
1. Removes the element from the tab order, so a keyboard user pressing Tab after the click finds focus suddenly elsewhere when the page re-renders with `disabled=true`.
2. Suppresses click + focus events entirely, so a screen reader has no way to announce status.
3. Gets re-enabled on the next render with no signal about why, so a sighted user who clicked once and saw nothing happen has no model for "should I click again or wait?"

The ARIA replacement is `aria-busy` + `aria-disabled`. Both are properties, not interactivity blockers — the element stays in the tab order, focus is preserved, and the screen reader announces the busy state via the page's aria-live mechanism. To prevent double-fire on rapid clicks, guard inside the click handler: `if (busy) return`.

**Incorrect — native `disabled` mid-mutation:**

```tsx
<button
  type="button"
  disabled={toggle.isPending}         // ❌ removed from tab order, no busy announcement
  onClick={() => toggle.mutate(...)}
>
  Save
</button>
```

**Correct — aria-busy + aria-disabled + click guard:**

```tsx
const busy = isLoading || toggle.isPending

<button
  type="button"
  aria-busy={toggle.isPending || undefined}
  aria-disabled={busy || undefined}
  className="… aria-busy:opacity-60 aria-disabled:opacity-50"
  onClick={() => {
    if (busy) return                 // ✅ double-click guard, focus preserved
    toggle.mutate(...)
  }}
>
  Save
</button>
```

Use `undefined` (not `false`) for the aria props when the state is not active — `aria-busy="false"` is technically valid but tooling-noisy. The `aria-busy:` and `aria-disabled:` Tailwind variants pair cleanly for the visual cue without depending on the native `disabled` style.

This rule pairs with **error-message-not-in-native-title-attribute** — together they keep the button's a11y surface clean during failure modes too.

Reference: [WAI-ARIA 1.2 — aria-busy](https://www.w3.org/TR/wai-aria-1.2/#aria-busy)

Reference: [WCAG 2.2 SC 4.1.3 — Status Messages](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html)
