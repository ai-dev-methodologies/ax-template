# WCAG 2.2 — Techniques for Accessibility

**Source:** https://www.w3.org/WAI/WCAG22/Techniques/  
**Fetched:** 2026-05-18  
**Version:** WCAG 2.2 (W3C Recommendation 05 October 2023)  
**Purpose:** Accessibility techniques referenced by SP34 a11y primitives (skip-link, announce-live)

---

## SC 2.4.1 Bypass Blocks (Level A)

**Understanding:** https://www.w3.org/WAI/WCAG22/Understanding/bypass-blocks.html

> A mechanism is available to bypass blocks of content that are repeated on multiple Web pages.

### G1 — Skip Navigation Link

Add a link at the beginning of a block of repeated content that goes to the end of the block:

```html
<!-- First focusable element on the page -->
<a href="#main-content" class="skip-link">Skip to main content</a>

<nav>...</nav>

<main id="main-content">
  <!-- page content begins here -->
</main>
```

The skip link MUST be:
1. The **first** focusable element on every page.
2. Visible when focused (keyboard users must see it).
3. Linked to an `id` on the `<main>` element.

> "The link text should describe the purpose of the link and indicate where it goes."

**WebAIM reference:** https://webaim.org/techniques/skipnav/

---

## SC 4.1.3 Status Messages (Level AA)

**Understanding:** https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html

> In content implemented using markup languages, status messages can be programmatically
> determined through role or properties so they can be presented by assistive technologies
> without receiving focus.

### ARIA live regions

Use `aria-live` to announce status changes without moving focus:

```html
<!-- Polite: announced after current speech finishes -->
<div role="status" aria-live="polite" aria-atomic="true">
  Settings saved successfully
</div>

<!-- Assertive: announced immediately, interrupting current speech -->
<div role="alert" aria-live="assertive" aria-atomic="true">
  Error: Failed to save settings
</div>
```

Key rules:
- Live regions MUST be **present in the DOM on page load** — not dynamically created.
  Screen readers observe the live region from the start; dynamically created regions
  may not be announced.
- Use `polite` for non-urgent status (saves, confirmations, progress).
- Use `assertive` only for urgent errors that require immediate attention.
- `aria-atomic="true"`: announce the entire region content, not just changed parts.
- Do NOT move focus to the live region — it must be announced without focus.

---

## SC 1.4.3 Contrast (Minimum) (Level AA)

**Understanding:** https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html

> The visual presentation of text and images of text has a contrast ratio of at least 4.5:1.

The ImpersonationBanner uses amber-400 background (#FBBF24) with amber-950 text (#451A03).
Contrast ratio: **10.4:1** — exceeds both AA (4.5:1) and AAA (7:1) thresholds.

---

## SC 2.4.7 Focus Visible (Level AA)

**Understanding:** https://www.w3.org/WAI/WCAG22/Understanding/focus-visible.html

> Any keyboard operable user interface has a mode of operation where the keyboard focus indicator is visible.

The skip-link uses `focus:translate-y-0` to become visible on focus. The SkipLink component
satisfies SC 2.4.7 because:
1. The link is focusable by default (it is an `<a>` element with an `href`).
2. The focus indicator is visible via Tailwind's `focus:` utilities.
3. The outline (`focus-visible:ring-2`) satisfies SC 2.4.11 (Focus Appearance, Level AA new in 2.2).

---

## Key sections referenced by ax-template templates

| Section | Referenced by |
|---|---|
| SC 2.4.1 Bypass Blocks — G1 skip link | `templates/L2/blocks/skip-link.tsx` |
| SC 4.1.3 Status Messages — aria-live | `templates/L2/blocks/announce-live.tsx`, `templates/L2/blocks/impersonation-banner.tsx`, `templates/L2/blocks/maintenance-notice.tsx` |
| SC 1.4.3 Contrast | `templates/L2/blocks/impersonation-banner.tsx` amber palette |
| SC 2.4.7 Focus Visible | `templates/L2/blocks/skip-link.tsx`, `templates/L2/blocks/keyboard-shortcut-help.tsx` |

---

*Snapshot captured 2026-05-18. Verify against W3C for normative language.*
