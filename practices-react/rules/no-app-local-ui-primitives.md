---
title: Per-persona apps must reuse the shared catalog (@ax/ui / @ax/blocks) — never define app-local UI primitives
impact: HIGH
impactDescription: "When each app re-implements its own Button/Input/Card, the design system fragments: tokens drift, a11y fixes land in one copy and not the others, and the monorepo's whole reason for a shared catalog is defeated. Apps must import primitives from @ax/ui and composed blocks from @ax/blocks. A bespoke app-local copy (a components/ui/** module, or a component named like a catalog primitive) is a hard error."
tags: [monorepo, design-system, components, reuse, consistency]
applicable_to: [react, nextjs, vite]
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-CATALOG-088"
verification:
  type: lint
  rule_id: "ax/no-app-local-ui-primitives"
  status: shipped
  notes: "Shipped + enabled: ax/no-app-local-ui-primitives is scoped to files under an apps/ segment. It flags (a) defining/exporting a component named like a catalog primitive (Button/Input/Label/Field/Card+Card*/Alert/Badge/Spinner/Switch) and (b) importing a local components/ui/** module. Re-exporting from @ax/ui or @ax/blocks is allowed. The root web-shell app (not under apps/**) is exempt because it predates the catalog. Registered in the plugin index and wired as error for apps/** in frontend/eslint.config.mjs."
provenance: { pilot: false, pipeline_version: "2026-06-05", pipeline_steps: [phaseA_monorepo_foundation, phaseB_rule_authoring, phaseC_teeth_proof] }
audit:
  accuracy: { status: verified, last_verified: "2026-06-05" }
  freshness: { status: current, last_verified: "2026-06-05", next_review_by: "2026-09-03" }
  completeness: { status: complete, amendments: ["Scope to apps/** only; exempt the predating web-shell", "Allow re-exports from the catalog packages"] }
  gap_check: { status: complete }
upstream:
  - id: turborepo-internal-packages
    title: "Turborepo: Sharing code with internal packages"
    url: "https://turborepo.com/docs/core-concepts/internal-packages"
    role: seed
evidence:
  - source_type: external
    citation: "Turborepo Handbook — Internal Packages: sharing UI/code across apps in a monorepo via a single internal package, so consuming apps import from the shared package rather than copying source."
    url: "https://turborepo.com/docs/core-concepts/internal-packages"
    quote: "Internal Packages are libraries whose source code lives inside your Workspace. ... You can quickly make Internal Packages to share code within your monorepo and choose to publish them to the npm registry if you need to later."
sibling_rules: [rerender-no-inline-components]
---

## Per-persona apps must reuse the shared catalog — never define app-local UI primitives

**Impact: HIGH — This is a design-system integrity rule. The whole point of the `@ax/ui` + `@ax/blocks` catalog is that every per-persona app renders the *same* primitives. An app-local `Button` silently forks the design system.**

This monorepo hosts a shared component catalog:

- `@ax/ui` — design-system primitives (`Button`, `Input`, `Label`, `Field`, `Card` + `Card*` family, `Alert`, `Badge`, `Spinner`, `Switch`, plus `cn`).
- `@ax/blocks` — composed 21st.dev-style blocks built on `@ax/ui`.

Per-persona apps live under `frontend/apps/**`. They **consume** the catalog; they must not re-implement it.

### Incorrect — an app defines its own primitive (apps/enterprise/src/Button.tsx)

```tsx
// apps/enterprise/src/Button.tsx
export function Button({ children }: { children: React.ReactNode }) {
  return <button className="rounded bg-blue-600 px-3 py-2">{children}</button>
}
```

This forks the catalog: token drift, divergent focus/hover/disabled states, and any a11y fix to the real `@ax/ui` Button never reaches this copy.

### Incorrect — an app imports a local components/ui module

```tsx
// apps/enterprise/src/login.tsx
import { Button } from './components/ui/button' // ← app-local copy, forbidden
```

### Correct — import the primitive from the shared catalog

```tsx
// apps/enterprise/src/login.tsx
import { Button, Card, CardHeader, CardTitle } from '@ax/ui'
import { StatusBadge } from '@ax/blocks'

export function Login() {
  return (
    <Card>
      <CardHeader><CardTitle>로그인</CardTitle></CardHeader>
      <Button>계속</Button>
    </Card>
  )
}
```

### What the rule flags (only in files under `apps/**`)

1. A **definition** of a component named like a catalog primitive that returns JSX — `function Button() { return <.../> }`, `const Card = () => <.../>`, or a `forwardRef(...)`/`memo(...)` factory assigned to a primitive name.
2. An **export** that surfaces a catalog-primitive name — `export { Button }`, `export { MyThing as Badge }` — unless it re-exports from `@ax/ui` / `@ax/blocks`.
3. An **import** from a local `components/ui/**` module (any relative specifier with a `components/ui` segment).

### What it does NOT flag

- Importing primitives from `@ax/ui` / blocks from `@ax/blocks` — that is the required pattern.
- Re-exporting from the catalog packages (`export { Button } from '@ax/ui'`).
- An app-local function that happens to be named like a primitive but does not return JSX (a plain helper).
- Components with non-catalog names (`LoginPanel`, `DashboardHeader`, …) — apps compose freely; only the reserved primitive names are protected.
- The root web-shell app (everything **not** under `apps/**`) — it predates the catalog and is exempt.

### Why "I just need a small tweak" isn't a reason

If a catalog primitive is missing a variant, add the variant to `@ax/ui` so every app benefits — do not fork it into one app. The shared catalog is the single source of truth; that is exactly what makes six per-persona apps stay consistent.

Sources:
- [Turborepo: Sharing code with internal packages](https://turborepo.com/docs/core-concepts/internal-packages)
