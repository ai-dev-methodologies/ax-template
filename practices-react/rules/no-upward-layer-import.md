---
title: Layers are single-direction (app -> features -> shared) — a module must never import from a higher layer
impact: HIGH
impactDescription: "Layers exist to bound blast radius: shared code (components/lib) is meant to be safe to import from anywhere, and a feature is meant to be safe to delete without touching app or shared. An upward import (shared -> features, shared -> app, or features -> app) breaks both guarantees — the 'foundational' code now depends on the thing built on top of it, so deleting or changing a feature can break shared UI, and a change anywhere in shared risks a circular rebuild through the layer it was supposed to be beneath."
tags: [architecture, layering, imports, feature-layout, eslint]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ARCH-001"
verification:
  type: lint
  rule_id: "ax/no-upward-layer-import"
  status: shipped
  notes: "Shipped + enabled: ax/no-upward-layer-import classifies every file under settings.ax's configured src layout (default: src/app -> app, src/features/<feature> -> features, src/components + src/lib -> shared) and flags any import whose target layer outranks the importer's layer (app=3 > features=2 > shared=1). Same-layer feature<->feature imports are exempted here — governed by ax/no-cross-feature-deep-import instead. Fires on static import, dynamic import(), and require() alike (lib/feature-layout.js's shared importVisitors), so switching import syntax cannot bypass it. Registered in the plugin and enforcing."
provenance: { pilot: false, pipeline_version: "2026-06-08", pipeline_steps: [phaseA_frontend_decomposition_design, phaseB_rule_authoring, phaseC_teeth_proof] }
audit:
  accuracy: { status: verified, last_verified: "2026-08-13" }
  freshness: { status: current, last_verified: "2026-08-13", next_review_by: "2026-11-11" }
  completeness: { status: complete, amendments: ["Catalog doc authored post-ship — rule and tests predate this file; see practices-react/eslint-plugin-ax/rules/no-upward-layer-import.js and tests/no-upward-layer-import.test.js."] }
  gap_check: { status: complete }
upstream:
  - id: acyclic-dependencies-principle
    title: "Wikipedia — Acyclic dependencies principle"
    url: "https://en.wikipedia.org/wiki/Acyclic_dependencies_principle"
    role: seed
  - id: feature-sliced-design-layers
    title: "Feature-Sliced Design — Layers (import rule)"
    url: "https://feature-sliced.design/docs/reference/layers"
    role: seed
evidence:
  - source_type: external
    anchors: generic_principle_only
    citation: "Acyclic Dependencies Principle (Martin, Agile Software Development: Principles, Patterns, and Practices) — the dependency graph of packages must have no cycles; a package that depends on a sibling it may in turn be depended upon by introduces a cycle that prevents independent build and release. (Anchors only the generic acyclic/single-direction dependency principle; the concrete app/features/shared layer split and its rank order are an ax-template layer decision, not a requirement of the principle itself.)"
    url: "https://en.wikipedia.org/wiki/Acyclic_dependencies_principle"
    quoted_at: "2026-08-13"
  - source_type: external
    anchors: generic_principle_only
    citation: "Feature-Sliced Design — Layers reference: layers are ordered and a module may only import downward. (Anchors only the generic single-direction-import-between-layers principle; ax-template's own three-layer app/features/shared split, directory names, and settings.ax configurability are an ax-template layer decision, not an FSD requirement — this project does not adopt FSD's full layer set or naming.)"
    url: "https://feature-sliced.design/docs/reference/layers"
    quote: "A module (file) in a slice can only import other slices when they are located on layers strictly below."
    quoted_at: "2026-08-13"
sibling_rules: [no-feature-internal-import, no-cross-feature-deep-import]
---

## Layers are single-direction — a module must never import from a higher layer

**Impact: HIGH — Layers only bound blast radius if the dependency direction actually holds. An upward import silently turns "foundational" code into code that depends on the thing built on top of it.**

`ax/no-upward-layer-import` enforces a three-layer directory convention (configurable via
`eslint.config.mjs`'s flat-config `settings.ax`, default shown below):

```text
src/app/                          -- routing layer (top, rank 3)
src/features/<feature>/<slice>/   -- feature slices (rank 2)
src/components/, src/lib/         -- shared kernel (bottom, rank 1)
```

The allowed import direction is **app -> features -> shared** — never the reverse. A module
may import from its own layer or any layer strictly below it; importing from a layer above
is always a violation.

### Incorrect — a shared component imports a feature

```tsx
// src/components/nav/Nav.tsx — SHARED layer (rank 1)
// VIOLATION: shared imports from features (rank 2) — upward
import { LoginForm } from '@/features/auth/login'

export function Nav() {
  return <nav><LoginForm /></nav>
}
```

`src/components/**` is meant to be safe to import from anywhere — app, every feature, and
other shared code. Once `Nav` depends on `features/auth`, deleting or refactoring the auth
feature can break navigation, and the "foundational" layer is no longer foundational.

### Incorrect — a feature imports from app

```tsx
// src/features/payment/checkout/CheckoutForm.tsx — FEATURES layer (rank 2)
// VIOLATION: features imports from app (rank 3) — upward
import { rootMetadata } from '@/app/layout'

export function CheckoutForm() {
  return <form aria-label={rootMetadata.title}>{/* ... */}</form>
}
```

A feature is meant to be deletable without touching `app/`. A feature that reaches up into
`app/` couples the routing layer to a specific feature's internals — removing or moving the
feature now requires editing `app/` too.

### Correct — respect the app -> features -> shared direction

```tsx
// src/app/(auth)/login/page.tsx — APP layer, importing DOWNWARD — fine
import { LoginForm } from '@/features/auth/login'
import { Card } from '@/components/ui/card'

export default function LoginPage() {
  return <Card><LoginForm /></Card>
}
```

```tsx
// src/features/payment/checkout/CheckoutForm.tsx — FEATURES importing shared — fine
import { Button } from '@/components/ui/button'
import { formatCurrency } from '@/lib/format'

export function CheckoutForm() {
  return <Button>{formatCurrency(4200)}</Button>
}
```

### Allowed vs. forbidden import direction

| Importer layer | Target layer | Allowed? |
|---|---|---|
| app | features | Yes — downward |
| app | shared | Yes — downward |
| features | shared | Yes — downward |
| shared | shared | Yes — same layer |
| features | features (own or sibling barrel) | Governed by `no-cross-feature-deep-import`, not this rule |
| shared | features | No — upward |
| shared | app | No — upward |
| features | app | No — upward |

### What this rule does NOT flag

- **Same-layer feature-to-feature imports** — `src/features/billing/*` importing
  `src/features/payment/*` is a same-rank (features -> features) import, out of scope for
  this rule. See `ax/no-cross-feature-deep-import` for the sibling-feature isolation rule.
- **Bare/external module specifiers** (`react`, `next/navigation`, `@ax/ui`) — anything that
  does not resolve into the configured `src/` layout is not classified into a layer and is
  not this rule's concern.
- **Files outside the configured `src/` tree entirely.**

### Configuring a different layout

Projects with a different `srcDir`, path alias, or top-level directory names declare them
via flat-config `settings.ax` (`eslint.config.mjs`); the layer **ranks** and the
app -> features -> shared **direction** are fixed, but directory names are not:

```js
// eslint.config.mjs
export default [
  {
    settings: {
      ax: {
        srcDir: 'src',
        alias: { '@/': 'src/' },
        layers: {
          app: ['app'],
          features: ['features', 'modules'],
          shared: ['components', 'lib', 'ui'],
        },
      },
    },
  },
]
```

Reference: [Wikipedia — Acyclic dependencies principle](https://en.wikipedia.org/wiki/Acyclic_dependencies_principle)

Reference: [Feature-Sliced Design — Layers](https://feature-sliced.design/docs/reference/layers)

Reference: [practices-react/rules/no-l4-cross-import.md](no-l4-cross-import.md) — the L4-page analog of the same acyclic-dependency principle, applied to `templates/L4/` domains rather than `src/features/`.
