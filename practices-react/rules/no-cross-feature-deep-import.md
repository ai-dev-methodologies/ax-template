---
title: A feature must not deep-import another feature's internals — cross-feature reuse goes through the target's barrel or the shared kernel
impact: HIGH
impactDescription: "Features are meant to be siblings: independently understandable, independently deletable, composed at the app layer or via the shared kernel. A feature that reaches past another feature's barrel into its internal files creates the same undeclared coupling as two backend modules sharing a private class directly — a change inside the target feature that its own owner considers purely internal now silently breaks a sibling feature."
tags: [architecture, layering, imports, feature-layout, feature-isolation, eslint]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ARCH-003"
verification:
  type: lint
  rule_id: "ax/no-cross-feature-deep-import"
  status: shipped
  notes: "Shipped + enabled: ax/no-cross-feature-deep-import governs files INSIDE a feature (src/features/<A>/**) and flags an import that resolves into a DIFFERENT feature (src/features/<B>/...) past B's barrel — the feature's own internals and a cross-feature BARREL import (@/features/<B> or @/features/<B>/<slice>) are both allowed; only a deep import past B's barrel is a violation. Fires on static import, dynamic import(), and require() alike — the ESLint RuleTester suite specifically asserts dynamic import() and require() are not a bypass. Registered in the plugin and enforcing."
provenance: { pilot: false, pipeline_version: "2026-06-08", pipeline_steps: [phaseA_frontend_decomposition_design, phaseB_rule_authoring, phaseC_teeth_proof] }
audit:
  accuracy: { status: verified, last_verified: "2026-08-13" }
  freshness: { status: current, last_verified: "2026-08-13", next_review_by: "2026-11-11" }
  completeness: { status: complete, amendments: ["Catalog doc authored post-ship — rule and tests predate this file; see practices-react/eslint-plugin-ax/rules/no-cross-feature-deep-import.js and tests/no-cross-feature-deep-import.test.js (RuleTester asserts dynamic import()/require() cannot bypass the rule)."] }
  gap_check: { status: complete }
upstream:
  - id: feature-sliced-design-slices-segments
    title: "Feature-Sliced Design — Slices and segments reference"
    url: "https://feature-sliced.design/docs/reference/slices-segments"
    role: seed
evidence:
  - source_type: external
    anchors: generic_principle_only
    citation: "Feature-Sliced Design — Slices and segments reference: an ideal slice is independent from its sibling slices on the same layer (zero coupling). (Anchors only the generic same-layer sibling-isolation principle; ax-template's own barrel-exemption shape — a cross-feature import IS allowed through the target's published barrel, only a deep internal import is forbidden — and the app/features/shared layer split are an ax-template layer decision, not an FSD requirement.)"
    url: "https://feature-sliced.design/docs/reference/slices-segments"
    quote: "An ideal slice is independent from other slices on its layer (zero coupling) and contains most of the code related to its primary goal (high cohesion)."
    quoted_at: "2026-08-13"
sibling_rules: [no-upward-layer-import, no-feature-internal-import]
---

## A feature must not deep-import another feature's internals

**Impact: HIGH — Features are siblings, not a shared implementation pool. Reaching past a sibling feature's barrel creates the exact coupling the feature boundary exists to prevent — a change the target feature's owner considers purely internal can silently break a caller they have no way to discover.**

`ax/no-cross-feature-deep-import` governs files **inside** a feature
(`src/features/<A>/**`). It allows a feature to import anything within its own tree, the
shared kernel (`@/components/**`, `@/lib/**`, `@ax/ui`, `@ax/blocks`), and another feature's
**published barrel** — but forbids reaching past that barrel into a sibling feature's
internal files.

(For a non-feature importer — `app/`, `components/`, `lib/` — reaching into a feature's
internals is a separate concern, covered by `ax/no-feature-internal-import`.)

### Incorrect — billing reaches into payment's internal file

```tsx
// src/features/billing/checkout/CheckoutForm.tsx — inside feature "billing"
// VIOLATION: reaches past feature "payment"'s barrel into its internal capture.ts
import { capture } from '@/features/payment/panel/capture'

export function CheckoutForm() {
  const result = capture({ amount: 4200 })
  return <div>{result.status}</div>
}
```

`billing` now depends on the exact file layout of `payment/panel/capture.ts`. If the
`payment` feature owner splits `capture.ts` into two files, or moves the capture logic under
a different slice, `billing` breaks — with no signal visible from inside `payment`'s own
directory that a sibling feature depends on that path.

### Incorrect — dynamic import() and require() are not an escape hatch

```tsx
// src/features/billing/checkout/CheckoutForm.tsx
// VIOLATION: switching import syntax does not change what is being imported
const capture = await import('@/features/payment/panel/capture')
const captureCjs = require('@/features/payment/panel/capture')
```

### Correct — go through the target feature's published barrel

```tsx
// src/features/billing/checkout/CheckoutForm.tsx
// Cross-feature reuse through payment's PUBLISHED barrel — allowed.
import { Payment } from '@/features/payment'
import { PaymentPanel } from '@/features/payment/panel'

export function CheckoutForm() {
  return <PaymentPanel amount={4200} />
}
```

### Correct — or move the shared logic to the kernel

```tsx
// src/features/billing/checkout/CheckoutForm.tsx
// If capture() is genuinely shared, the fix is to lift it to the shared kernel —
// not to import it out of payment's internals.
import { Button } from '@/components/ui/button'
import { formatCurrency } from '@/lib/format'

export function CheckoutForm() {
  return <Button>{formatCurrency(4200)}</Button>
}
```

### What counts as the target feature's "barrel" (allowed)

Same shape as `no-feature-internal-import`: the target feature's root
(`@/features/<B>`), a single slice directory (`@/features/<B>/<slice>`), or an explicit
index file. Anything one segment deeper is a deep internal import.

### What this rule does NOT flag

- **A feature importing its own internals** (`billing` importing another file inside
  `billing`) — unrestricted; only cross-feature imports are governed.
- **A cross-feature BARREL import** — `@/features/payment` or
  `@/features/payment/panel` from inside `billing` is the intended, allowed way to reuse
  another feature.
- **Imports of the shared kernel** — `@/components/**`, `@/lib/**`, `@ax/ui`, `@ax/blocks`
  — never a feature, so never in scope for this rule.
- **A non-feature file reaching into a feature's internals** — `src/app/page.tsx` importing
  `@/features/payment/panel/internal` is `ax/no-feature-internal-import`'s job, not this
  rule's (the importer here is not itself inside a feature).

Reference: [Feature-Sliced Design — Slices and segments](https://feature-sliced.design/docs/reference/slices-segments)

Reference: [practices-react/rules/no-feature-internal-import.md](no-feature-internal-import.md) — the sibling rule covering non-feature importers reaching into a feature.
