---
title: Inline-script prehydration for deterministic boot values (theme/auth-shell) — never for fetched data or user-controlled values; honor CSP
impact: MEDIUM
impactDescription: "Eliminates flicker for client-only boot values (theme class, color scheme) without breaking SSR. Narrow scope: deterministic, tiny, non-user-controlled values only."
tags: [rendering, ssr, hydration, localStorage, flicker, csp]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-005"
verification:
  type: review
  status: manual
  notes: "Reviewer enforces: (a) script accesses ONLY deterministic boot keys (theme/color-scheme), (b) values are escaped or strictly typed, (c) CSP nonce on the script, (d) no fetch / heavy logic / untrusted data inside, (e) framework primitive used if available."
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
      - "Narrowed scope to deterministic boot values"
      - "Added XSS / CSP nonce requirement"
      - "Forbade fetch and untrusted-data interpolation inside"
      - "Recommended framework primitives where available"
  gap_check: { status: complete }
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: rendering-hydration-no-flicker"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-hydration-no-flicker.md"
    role: seed
evidence:
  - upstream_id: vercel-react-best-practices
    section: "rendering-hydration-no-flicker"
    quote: "Inject a synchronous script that updates the DOM before React hydrates."
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
sibling_rules:
  - rendering-hydration-suppress-warning
  - client-localstorage-schema
---

## Inline-script prehydration for deterministic boot values — never for fetched data or user-controlled values

**Impact: MEDIUM — Narrow tool. Use for theme/color-scheme/known-auth-shell ONLY.**

### Correct — theme class prehydration

```tsx
function ThemeWrapper({ children, nonce }: { children: ReactNode; nonce: string }) {
  return (
    <>
      <div id="theme-wrapper">{children}</div>
      <script
        nonce={nonce}
        dangerouslySetInnerHTML={{
          __html: `
            (function() {
              try {
                var t = localStorage.getItem('theme:v1');
                var el = document.getElementById('theme-wrapper');
                if (el && (t === 'dark' || t === 'light')) el.className = t;
              } catch (e) {}
            })();
          `,
        }}
      />
    </>
  )
}
```

Notes:
- Whitelist allowed values inline (`'dark' || 'light'`) — never inject user-controlled strings.
- CSP nonce attached.
- Script body is small and deterministic.

### Forbidden inside the inline script

- **Untrusted/user-controlled data interpolation.** Any value coming from a cookie/header/storage MUST be parsed and validated against an allowlist before use. Don't string-interpolate.
- **`fetch()` / `XMLHttpRequest` / `import()`.** Boot scripts must be synchronous and deterministic.
- **Heavy logic.** Run as little code as possible.
- **Errors swallowed silently** — the script does need a try/catch but log the error in dev.

### CSP requirement

If your CSP includes `script-src 'self'` without `'unsafe-inline'`, you must attach a per-request nonce. React's `nonce` prop on `<script>` propagates to the rendered tag.

```typescript
// In a Server Component or middleware
const nonce = randomBytes(16).toString('base64')
// Headers().set('Content-Security-Policy', `script-src 'nonce-${nonce}' ...`)
```

### Framework primitives — prefer when available

- Next.js: `next-themes` library handles theme prehydration safely.
- Some UI frameworks ship their own prehydration helpers.
- Use them. Less to audit, less to misconfigure.

### Anti-pattern — flicker

```tsx
function ThemeWrapper({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState('light')
  useEffect(() => {
    const stored = localStorage.getItem('theme')
    if (stored) setTheme(stored)  // visible flash from light → stored
  }, [])
  return <div className={theme}>{children}</div>
}
```

### Anti-pattern — SSR break

```tsx
function ThemeWrapper({ children }: { children: ReactNode }) {
  const theme = localStorage.getItem('theme')   // throws on server
  return <div className={theme || 'light'}>{children}</div>
}
```

Sources:
- [Vercel: rendering-hydration-no-flicker](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/rendering-hydration-no-flicker.md)
