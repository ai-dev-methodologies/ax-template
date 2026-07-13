---
title: suppressHydrationWarning on the smallest element with intentional server/client text mismatch
impact: LOW-MEDIUM
impactDescription: "Silences noise for known unavoidable text differences (timestamps, locale formatting, randomized ids). Must not mask structural mismatches — those are real bugs."
tags: [rendering, hydration, ssr, nextjs]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-006"
verification:
  type: review
  status: manual
  notes: "Reviewer rejects suppressHydrationWarning if (a) applied to a parent element wrapping more than text, (b) used to mask a structural mismatch, (c) used when deterministic SSR or client-only render is a viable alternative."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy: { status: verified, last_verified: "2026-05-16" }
  freshness: { status: current, last_verified: "2026-05-16", next_review_by: "2026-08-14" }
  completeness:
    status: complete
    amendments:
      - "Apply only to the smallest element holding the mismatched text"
      - "Listed acceptable cases (timestamps, locale, randomized IDs)"
      - "Forbade masking structural mismatches"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-hydration-suppress-warning"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-hydration-suppress-warning.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-hydration-suppress-warning"
    quote: "In SSR frameworks (e.g., Next.js), some values are intentionally different on server vs client (random IDs, dates, locale/timezone formatting)."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - rendering-hydration-no-flicker
---

## suppressHydrationWarning on the smallest element with intentional server/client text mismatch

**Impact: LOW-MEDIUM — Narrow escape hatch for known-unavoidable text differences.**

### Acceptable cases

- Timestamps formatted with the user's local time zone
- Locale-dependent number/currency formatting
- Randomized IDs generated independently on server/client
- Theme class derived from a prehydration script (see `rendering-hydration-no-flicker`)

### Correct — smallest element

```tsx
function Timestamp({ iso }: { iso: string }) {
  return (
    <span suppressHydrationWarning>
      {new Date(iso).toLocaleString()}
    </span>
  )
}
```

The suppression is scoped to the `<span>` containing the dynamic text. Surrounding structure is still checked.

### Incorrect — too broad

```tsx
// BAD: silences hydration warnings for the entire panel.
// A real structural mismatch deeper in <Panel> would now go undetected.
function Panel({ iso }: { iso: string }) {
  return (
    <section suppressHydrationWarning>
      {/* ...lots of structure... */}
      <span>{new Date(iso).toLocaleString()}</span>
    </section>
  )
}
```

### Forbidden — masking structural mismatches

```tsx
// BAD: the issue is conditional rendering shape diverges between server and client.
// suppressHydrationWarning hides the warning but the resulting DOM is wrong.
<div suppressHydrationWarning>
  {isClient ? <Drawer /> : <SidebarPlaceholder />}
</div>
```

Fix the divergence: either server-render the same thing and progressively enhance, or render the variant only on the client (`useEffect` + state).

### Prefer alternatives where possible

- **Deterministic SSR.** Pass the timestamp value in already-formatted form from the server.
- **Client-only render.** `useEffect` + state ensures the value renders only after hydration. Comes with the flicker risk addressed by `rendering-hydration-no-flicker`.

`suppressHydrationWarning` should be the third choice, used only when neither alternative fits.

Sources:
- [Vercel: rendering-hydration-suppress-warning](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-hydration-suppress-warning.md)
