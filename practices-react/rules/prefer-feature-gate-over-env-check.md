---
title: "Feature flag checks must use FeatureGate or the feature-flags API — not process.env"
rule_id: prefer-feature-gate-over-env-check
impact: HIGH
impactDescription: "Direct process.env checks for feature flags bypass the runtime admin UI, require redeployment to toggle, and cannot be dynamically controlled without rebuilding the app."
tags:
  - feature-flags
  - runtime-control
  - process-env
  - l4-template
  - l2-block
applicable_to:
  - react
  - nextjs
provenance_class: internal_design
applies_to: paths_created_after_2026-05-18
protects_template_id: templates/L2/blocks/feature-gate.tsx
failing_fixture_path: practices-react/evals/fixtures/feature_gate/fail_process_env_check/
spec_ref: "specs/feature-flags-frontend-l0.yaml#FF-FE-004"
verification:
  type: regex_scan
  pattern: "process\\.env\\.(NEXT_PUBLIC_)?FEATURE_|process\\.env\\.(NEXT_PUBLIC_)?FF_"
  status: fixture_driven
  notes: |
    Fixture _run.sh implements the check via a Python regex scan.
    Pass fixture: uses FeatureGate component — exits 0.
    Fail fixture: uses process.env.NEXT_PUBLIC_FEATURE_NEW_CHECKOUT — exits 1.
evidence:
  - source_type: external
    citation: "Next.js Docs — Environment variables and the limitation of build-time NEXT_PUBLIC_ variables (cannot be changed at runtime without rebuild)"
    url: "https://nextjs.org/docs/app/building-your-application/configuring/environment-variables"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "Martin Fowler — Feature Toggles (aka Feature Flags): Release toggles should be dynamic and externally managed, not baked into the build artifact"
    url: "https://martinfowler.com/articles/feature-toggles.html"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
next_review_by: "2026-11-18"
---

## Feature flag checks must use `FeatureGate` or the feature-flags API — not `process.env`

**Impact: HIGH — `process.env` feature flags require a full rebuild + redeployment to change. Runtime feature flag control via the admin API allows instant toggling without downtime.**

**Scope:** This rule applies **only to files created on or after 2026-05-18**. The feature-flags domain L4 templates are the canonical implementation.

### The violation — `process.env` for feature flag control

```tsx
// ❌ WRONG — build-time constant; cannot toggle without redeployment
const isNewCheckoutEnabled = process.env.NEXT_PUBLIC_FEATURE_NEW_CHECKOUT === 'true'

export default function CheckoutPage() {
  if (!isNewCheckoutEnabled) return <LegacyCheckout />
  return <NewCheckout />
}
```

### Correct — use `FeatureGate` (client-side) or middleware (server-side)

**Client-side gate:**
```tsx
// ✅ CORRECT — runtime-controlled via admin API; no rebuild needed
import { FeatureGate } from '@/templates/L2/blocks/feature-gate'

export default function CheckoutPage() {
  return (
    <FeatureGate name="new-checkout" fallback={<LegacyCheckout />}>
      <NewCheckout />
    </FeatureGate>
  )
}
```

**Server-side gate (middleware):**
```ts
// ✅ CORRECT — evaluated at request time in Next.js middleware
// templates/L4/feature-flags/middleware.ts
const FLAGGED_ROUTES: Record<string, string> = {
  '/new-checkout': 'new-checkout',
}
```

### Why this rule exists

| | `process.env` | FeatureGate / API |
|--|--|--|
| Toggle without rebuild | ❌ No | ✅ Yes |
| Admin UI control | ❌ No | ✅ Yes |
| Fail-closed on unknown flag | ❌ No (depends on default) | ✅ Yes |
| Runtime observability | ❌ No | ✅ Yes |
| Emergency kill-switch | ❌ Slow (redeploy) | ✅ Instant |

The `FeatureGate` L2 block (see `templates/L2/blocks/feature-gate.tsx`) fetches
`GET /api/v1/feature-flags/{name}/active` at render time. The result is cached
in the backend (Caffeine 30s TTL) so evaluation is fast and consistent.

See `blueprints/feature-flags-manifest.yaml` for the full feature-flags domain policy.

### Detect the violation

Pattern: `process.env.NEXT_PUBLIC_FEATURE_*` or `process.env.NEXT_PUBLIC_FF_*` in `.tsx`/`.jsx` files.

The `_run.sh` fixture script in `practices-react/evals/fixtures/feature_gate/` implements this as a Python regex scan.
