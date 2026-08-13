---
title: Outside a feature, import it only through its published barrel — never deep into a slice's internals
impact: HIGH
impactDescription: "A feature's slice barrel (index.ts) is its public API — the one surface app/, components/, lib/, and other features are meant to depend on. A non-feature file that reaches past the barrel into a specific internal file (@/features/<f>/<slice>/<file>) couples itself to an implementation detail the feature owner never promised to keep stable; renaming or restructuring a file inside the feature now breaks callers the feature owner has no way to discover from the feature's own directory."
tags: [architecture, layering, imports, feature-layout, public-api, eslint]
applicable_to: [react, nextjs]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ARCH-002"
verification:
  type: lint
  rule_id: "ax/no-feature-internal-import"
  status: shipped
  notes: "Shipped + enabled: ax/no-feature-internal-import governs every NON-feature importer (app/, components/, lib/, project root, and any other out-of-tree file) — feature-to-feature deep imports are governed separately by ax/no-cross-feature-deep-import, so the two rules never double-report the same violation. It flags an import that resolves into src/features/<feature>/... at any depth beyond the feature root or a slice's barrel (feature root, a bare slice directory, or an explicit index file all count as the barrel — everything deeper is internal). Fires on static import, dynamic import(), and require() alike. Registered in the plugin and enforcing."
provenance: { pilot: false, pipeline_version: "2026-06-08", pipeline_steps: [phaseA_frontend_decomposition_design, phaseB_rule_authoring, phaseC_teeth_proof] }
audit:
  accuracy: { status: verified, last_verified: "2026-08-13" }
  freshness: { status: current, last_verified: "2026-08-13", next_review_by: "2026-11-11" }
  completeness: { status: complete, amendments: ["Catalog doc authored post-ship — rule and tests predate this file; see practices-react/eslint-plugin-ax/rules/no-feature-internal-import.js and tests/no-feature-internal-import.test.js."] }
  gap_check: { status: complete }
upstream:
  - id: feature-sliced-design-public-api
    title: "Feature-Sliced Design — Public API reference"
    url: "https://feature-sliced.design/docs/reference/public-api"
    role: seed
evidence:
  - source_type: external
    anchors: generic_principle_only
    citation: "Feature-Sliced Design — Public API reference: a slice's public API is the contract and gate a consumer must go through; only what is exported there is reachable from outside. (Anchors only the generic published-API/gate principle; ax-template's own barrel-detection rule — feature root, a slice directory, or any explicit index file counts as the barrel — and the app/features/shared layer split are an ax-template layer decision, not an FSD requirement.)"
    url: "https://feature-sliced.design/docs/reference/public-api"
    quote: "A public API is a contract between a group of modules, like a slice, and the code that uses it. It also acts as a gate, only allowing access to certain objects, and only through that public API."
    quoted_at: "2026-08-13"
sibling_rules: [no-upward-layer-import, no-cross-feature-deep-import]
---

## Outside a feature, import it only through its published barrel

**Impact: HIGH — A feature's barrel is the one surface it promises to keep stable. Reaching past it couples a caller to an implementation detail the feature owner can rename or delete without ever knowing a caller depends on it.**

`ax/no-feature-internal-import` governs every file OUTSIDE the feature tree — `src/app/`,
`src/components/`, `src/lib/`, and the project root. It requires those files to consume a
feature only through its **published barrel**: the feature root (`@/features/<feature>`) or
a slice barrel (`@/features/<feature>/<slice>`). A "barrel" import is one that resolves to a
directory (its `index`) — reaching past that into a named file inside the slice is a
**feature-internal import** and is forbidden from outside the feature.

(Feature-to-feature deep imports are a separate concern, covered by
`ax/no-cross-feature-deep-import` — this rule targets non-feature importers only, so the two
rules never fire on the same import.)

### Incorrect — an app page reaches past the slice barrel into a specific file

```tsx
// src/app/(auth)/login/page.tsx
// VIOLATION: reaches past the login slice's barrel into its internal LoginForm.tsx file
import { LoginForm } from '@/features/auth/login/LoginForm'

export default function LoginPage() {
  return <LoginForm />
}
```

The feature owner is free to rename `LoginForm.tsx`, split it into two files, or move it
into a subdirectory — none of that is a breaking change from the feature's own perspective,
because the barrel (`@/features/auth/login`, i.e. its `index.ts`) is the only promised
surface. This import silently depends on a file the feature owner never agreed to keep in
place.

### Incorrect — a shared component reaches into a feature's internal file

```tsx
// src/components/nav/Nav.tsx
// VIOLATION: components/ is the shared layer; it must import payment's BARREL, not its
// internal capture.ts implementation file
import { capture } from '@/features/payment/panel/capture'
```

### Correct — import the feature's published barrel

```tsx
// src/app/(auth)/login/page.tsx
// The login slice's index.ts re-exports LoginForm — this is the public surface.
import { LoginForm } from '@/features/auth/login'

export default function LoginPage() {
  return <LoginForm />
}
```

```tsx
// Also correct: an explicit index import, or the feature-root barrel.
import { X } from '@/features/auth/login/index'
import { Auth } from '@/features/auth'
```

### What counts as "the barrel" (not flagged)

1. The feature root: `@/features/<feature>`.
2. A single slice directory: `@/features/<feature>/<slice>` (resolves to that slice's
   `index`).
3. An explicit index file at any level: `@/features/<feature>/<slice>/index` (any module
   extension — `.ts`, `.tsx`, `.js`, `.jsx`, `.mjs`, `.cjs`, `.mts`, `.cts`).

Anything with one more path segment past those three shapes — `@/features/<feature>/<slice>/<file>`
— is feature-internal and flagged when the importer is outside the feature.

### What this rule does NOT flag

- **A feature importing its own internals** — `src/features/auth/login/LoginForm.tsx`
  importing a sibling file inside the same feature is unrestricted; this rule only governs
  non-feature importers.
- **A feature importing another feature's internals** — that shape is real, but it is
  `ax/no-cross-feature-deep-import`'s job, not this rule's, so the two never double-report.
- **Imports that don't resolve into `src/features/**`** — shared kernel imports
  (`@/components/...`, `@/lib/...`) and bare/external specifiers (`react`, `@ax/ui`) are out
  of scope.

Reference: [Feature-Sliced Design — Public API](https://feature-sliced.design/docs/reference/public-api)

Reference: [practices-react/rules/no-cross-feature-deep-import.md](no-cross-feature-deep-import.md) — the sibling rule covering feature-to-feature deep imports.
