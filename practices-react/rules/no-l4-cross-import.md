---
title: "L4 domain pages must not import from other L4 domains"
rule_id: no-l4-cross-import
impact: HIGH
impactDescription: "Cross-importing between L4 domains creates tight coupling, makes domains non-independently deployable, and creates circular dependency risks that break tree-shaking and code-splitting"
tags:
  - l4-layer
  - domain-isolation
  - imports
  - architecture
applicable_to:
  - nextjs
  - react
provenance_class: internal_design
protects_template_id: templates/L4/
failing_fixture_path: practices/evals/fixtures/no-l4-cross-import/fail_cross_import/
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-ADVANCED-001"
verification:
  type: review
  status: manual
  notes: "No file under templates/L4/<domain-A>/ may import from templates/L4/<domain-B>/. Shared cross-cutting concerns (auth state, user context) must be sourced from shared hooks (hooks/), context providers (providers/), or L1/L2 components — never from another L4 domain."
evidence:
  - source_type: external
    citation: "Next.js documentation — Domain-driven architecture: each feature domain should be self-contained with no cross-domain imports at the route layer"
    url: "https://nextjs.org/docs/app/building-your-application/routing/colocation"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "Vercel best practices — Vertical slice architecture: L4 domains are independent vertical slices; cross-slice imports create coupling that breaks hot reloading and incremental static regeneration"
    url: "https://vercel.com/blog/how-we-optimized-package-imports-in-next-js"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## L4 domain pages must not import from other L4 domains

**Impact: HIGH — L4 domains are independent vertical slices. Cross-domain imports couple their deployment, break tree-shaking between route segments, and create circular dependency risks as each domain grows.**

L4 is the feature layer — auth, payment, notification, file-storage, crud are all L4 domains. Each domain owns its pages, server actions, and domain-specific components. Cross-importing means that changing domain A's internals can silently break domain B, and that both domains must be bundled together even when only one changes.

### The violation — L4 payment importing from L4 auth

```typescript
// ❌ WRONG — templates/L4/payment/PaymentPage.tsx imports from L4/auth
"use client";
// VIOLATION: importing auth domain's store and components directly
import { useAuthStore } from "templates/L4/auth/store/authStore";
import { AuthGuard } from "templates/L4/auth/components/AuthGuard";

export default function PaymentPage() {
  const { user } = useAuthStore(); // couples payment bundle to auth bundle
  return <AuthGuard><div>Pay for {user?.name}</div></AuthGuard>;
}
```

### Correct — shared hooks for cross-cutting concerns

```typescript
// ✅ CORRECT — payment uses shared hooks, not L4/auth internals
"use client";
// Shared hooks in hooks/ are the contract layer between L4 domains
import { useCurrentUser } from "hooks/useCurrentUser";
import { useRequireAuth } from "hooks/useRequireAuth";

export default function PaymentPage() {
  const user = useCurrentUser();   // shared contract — no auth bundle coupling
  useRequireAuth();                 // redirects if not authenticated

  return <div>Pay for {user?.name}</div>;
}
```

### Allowed import directions from L4

| Source (inside L4/domain) | Target | Allowed? |
|---|---|---|
| `templates/L4/payment/` | `templates/L1/components/` | ✅ |
| `templates/L4/payment/` | `templates/L2/blocks/` | ✅ |
| `templates/L4/payment/` | `templates/L3/pages/` | ✅ |
| `templates/L4/payment/` | `hooks/`, `providers/`, `lib/` | ✅ |
| `templates/L4/payment/` | `templates/L4/auth/` | ❌ violation |
| `templates/L4/payment/` | `templates/L4/notification/` | ❌ violation |

### Why this rule exists

During SP8-SP11 (L4 domain implementation) all cross-cutting concerns (auth state, current user, toast queue, error boundary) were moved to `hooks/` and `providers/`. Any L4 domain that directly imports from another L4 domain is bypassing this shared layer and re-coupling.

Reference: [Next.js App Router — route colocation](https://nextjs.org/docs/app/building-your-application/routing/colocation)

Reference: [Failing fixture: practices/evals/fixtures/no-l4-cross-import/fail_cross_import/PaymentPage.tsx](practices/evals/fixtures/no-l4-cross-import/fail_cross_import/PaymentPage.tsx)
