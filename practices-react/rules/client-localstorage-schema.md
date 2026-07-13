---
title: Version localStorage keys, wrap every access in try-catch, store minimal fields
impact: MEDIUM
impactDescription: "Schema evolution without conflicts. Survives Safari/Firefox private mode (which throws on setItem) and quota overflow. Reduces storage size and prevents accidental persistence of tokens/PII."
tags:
  - client
  - localStorage
  - storage
  - versioning
  - data-minimization
applicable_to:
  - react
  - nextjs
  - vite
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-CLIENT-004"
verification:
  type: review
  status: manual
  notes: "Reviewer checks every localStorage access: (a) key has a version suffix, (b) wrapped in try-catch, (c) value is a minimal field set (no tokens, no PII, no full server objects), (d) typeof window guard if the access can run during SSR, (e) migration path from prior versions documented."
provenance:
  pilot: true
  pipeline_version: "2026-05-16"
  pipeline_steps: [phaseA_multi_source, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-16"
    notes: "Pattern matches MDN guidance: localStorage can throw (Safari private mode, quota, disabled storage). Versioning + minimal payload + try-catch is the safe form."
  freshness:
    status: current
    last_verified: "2026-05-16"
    next_review_by: "2026-08-14"
  completeness:
    status: complete
    amendments:
      - "Added SSR guard: typeof window check at module top-level OR inside an effect"
      - "Made try-catch behavior explicit (return null / fall back to default)"
  gap_check:
    status: complete
upstream:
  - id: vercel-react-best-practices
    title: "Vercel agent skill: client-localstorage-schema"
    url: "https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/client-localstorage-schema.md"
    role: seed
  - id: mdn-localstorage
    title: "MDN — Window.localStorage"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage"
    role: primitive-semantics
evidence:
  - upstream_id: vercel-react-best-practices
    section: "client-localstorage-schema"
    quote: "Add version prefix to keys and store only needed fields."
  - source_type: external
    citation: "MDN — Window.localStorage (may throw a SecurityError when storage is disabled or quota is exceeded; not available during SSR — no `window` object)"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage"
codex_consensus:
  reviewer: "codex-cli 0.130.0, model_reasoning_effort=medium"
  reviewed_at: "2026-05-16"
  verdict: SHIP_WITH_AMEND
  agreements:
    - "Solid rule"
    - "Add SSR guard"
    - "Make try-catch fallback explicit"
sibling_rules: []
---

## Version localStorage keys, wrap every access in try-catch, store minimal fields

**Impact: MEDIUM — Schema evolution without conflicts. Survives Safari/Firefox private mode (which throws on `setItem`) and quota overflow. Reduces storage size and prevents accidental persistence of tokens/PII.**

### Three required practices

1. **Version every key.** `userConfig:v2` not `userConfig`. Schema can evolve; old data can be migrated or discarded explicitly.
2. **Wrap every access in try-catch.** `localStorage` throws in Safari private mode (`setItem`), Firefox private mode under some conditions, quota overflow, and when storage is disabled.
3. **Store minimal fields.** Pick the few keys the UI needs. Never persist tokens, PII, or full server objects.

Plus, when access can run during SSR (Next.js): **guard against `window` being undefined.**

### Correct

```tsx
const KEY = 'userConfig:v2'

type Config = { theme: 'light' | 'dark'; language: string }
const DEFAULT: Config = { theme: 'light', language: 'en' }

export function loadConfig(): Config {
  if (typeof window === 'undefined') return DEFAULT
  try {
    const raw = window.localStorage.getItem(KEY)
    return raw ? (JSON.parse(raw) as Config) : DEFAULT
  } catch {
    return DEFAULT
  }
}

export function saveConfig(config: Config): void {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(KEY, JSON.stringify(config))
  } catch {
    // private mode / quota / disabled — silently degrade
  }
}

// Migration from v1 to v2, run once at app boot (see advanced-init-once)
export function migrateConfig(): void {
  if (typeof window === 'undefined') return
  try {
    const v1 = window.localStorage.getItem('userConfig:v1')
    if (!v1) return
    const old = JSON.parse(v1)
    saveConfig({
      theme: old.darkMode ? 'dark' : 'light',
      language: old.lang ?? 'en',
    })
    window.localStorage.removeItem('userConfig:v1')
  } catch {
    // best effort
  }
}
```

### Incorrect

```tsx
// No version. No try-catch. Stores 20-field object including a token.
localStorage.setItem('userConfig', JSON.stringify(fullUserResponse))
const data = JSON.parse(localStorage.getItem('userConfig'))
```

What goes wrong:
- Safari private mode → `setItem` throws → uncaught error breaks the page.
- Schema change later → old data deserialized as new shape → runtime crashes.
- Token in localStorage → XSS exfiltration risk.
- SSR → `localStorage` undefined → hydration error.

### Storing minimal fields — example

```tsx
// User object has 20+ fields; only persist what the UI reads.
function cachePrefs(user: FullUser) {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(
      'prefs:v1',
      JSON.stringify({
        theme: user.preferences.theme,
        notifications: user.preferences.notifications,
      }),
    )
  } catch {}
}
```

Tokens, role flags, internal IDs — leave server-side. They expire, can be revoked, and don't belong in long-lived browser storage.

Sources:

- [Vercel: client-localstorage-schema](https://github.com/vercel-labs/agent-skills/blob/main/skills/react-best-practices/rules/client-localstorage-schema.md)
- [MDN — localStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage)
