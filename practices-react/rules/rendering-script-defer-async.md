---
title: Mark script tags defer or async (or use next/script with a strategy); type="module" is deferred by default
impact: HIGH
impactDescription: "Render-blocking scripts kill TTFP/TTI. defer for DOM-dependent ordered scripts, async for independent (analytics), next/script with strategy in Next.js. Critical inline boot scripts (theme prehydration) are an exception — they must run before hydration."
tags: [rendering, script, defer, async, performance]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-011"
verification:
  type: review
  status: manual
  notes: "Reviewer flags any <script> without defer/async/type=module/dangerouslySetInnerHTML in the document head. Confirms next/script is used where Next-available. Critical inline boot scripts are allowed when justified."
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
      - "Noted type=\"module\" defers by default"
      - "Carved exception for critical inline boot scripts (theme prehydration)"
      - "Cross-linked to bundle-defer-third-party for SDK loading"
  gap_check:
    status: complete
    note: "Overlaps bundle-defer-third-party for the script-tag-loading case; this rule covers the lower-level defer/async attributes."
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-script-defer-async"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-script-defer-async.md"
    role: seed
  - id: mdn-script-element
    title: "MDN — <script> element"
    url: "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/script"
    role: primitive-semantics
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-script-defer-async"
    quote: "Script tags without `defer` or `async` block HTML parsing while the script downloads and executes."
  - source_type: external
    citation: "MDN — script element: For module scripts, the defer attribute has no effect since they are deferred by default"
    url: "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/script#defer"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - bundle-defer-third-party
  - rendering-hydration-no-flicker
---

## Mark script tags defer or async (or use next/script); type="module" defers by default

**Impact: HIGH — A bare `<script src="…">` blocks HTML parsing.**

### Attribute semantics

| Attribute | Downloads | Executes | Order preserved |
|---|---|---|---|
| (none) | blocks parse | inline as discovered | yes |
| `defer` | parallel | after HTML parse | yes |
| `async` | parallel | as soon as ready | NO |
| `type="module"` | parallel | after HTML parse | yes (deferred by default) |

### Correct — explicit attributes

```tsx
<head>
  {/* Analytics — independent, no order dep */}
  <script src="https://example.com/analytics.js" async />
  {/* Utils — DOM-dependent or order-dependent */}
  <script src="/scripts/utils.js" defer />
  {/* Module — defers by default; the defer attribute is redundant but not wrong */}
  <script type="module" src="/scripts/app.js" />
</head>
```

### Correct — next/script in Next.js

Prefer the framework primitive — it handles strategy + de-duplication + Suspense compatibility:

```tsx
import Script from 'next/script'

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html>
      <body>
        {children}
        <Script src="https://example.com/analytics.js" strategy="afterInteractive" />
        <Script src="/scripts/utils.js" strategy="beforeInteractive" />
      </body>
    </html>
  )
}
```

### Incorrect — bare script blocks rendering

```tsx
<head>
  <script src="https://example.com/analytics.js" />
  <script src="/scripts/utils.js" />
</head>
```

Both block HTML parse until they download and execute.

### Critical inline boot scripts — exception

Some scripts MUST run before hydration to avoid flicker (theme prehydration; see `rendering-hydration-no-flicker`). Those are intentionally synchronous, dangerouslySetInnerHTML, and small. Defer/async would defeat the purpose.

Constraints on this exception:
- Small (< ~1 KB).
- Deterministic, no fetch, no user-controlled interpolation.
- CSP nonce attached.

### Choosing defer vs async

- `defer` when execution order matters, or the script depends on the DOM being parsed.
- `async` when the script is independent (analytics, error tracking, ad pixels) and order is irrelevant.
- When in doubt, `defer` is the safer default — order is preserved.

### Cross-rule scope

`bundle-defer-third-party` covers the higher-level "load this vendor library after hydration" decision (which often resolves to `next/script strategy="afterInteractive"`). This rule covers the raw `<script>` attribute decision when you're directly writing one.

Sources:
- [Vercel: rendering-script-defer-async](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-script-defer-async.md)
- [MDN — <script>](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/script)
