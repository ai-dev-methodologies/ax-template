---
title: "When a Next.js page implements a multi-L4 composition matching a Business Pattern Recipe, the L4 domain README must declare applied_recipe; ad-hoc cross-L4 hook/store imports without that declaration are prohibited"
rule_id: prefer-recipe-composition-over-l4-cross-import
impact: HIGH
impactDescription: "Ad-hoc cross-L4 imports in Next.js pages that duplicate a Recipe composition create undeclared bundle coupling between domain route segments, break tree-shaking, and make the recipe audit trail invisible to recipe_governance_guard.sh"
tags:
  - architecture
  - recipe-composition
  - l4-layer
  - domain-isolation
  - nextjs
applicable_to:
  - nextjs
provenance_class: internal_design
protects_template_id: recipes/*/RECIPE.md
failing_fixture_path: practices/evals/fixtures/prefer-recipe-composition-over-l4-cross-import/fail_ad_hoc_cross_import/
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-L4-001"
verification:
  type: script
  notes: |
    ESLint import/no-restricted-paths or custom rule:
    Flag any L4/<domain>/ page that imports from 2+ sibling L4 domains
    when the domain README lacks applied_recipe: field.
    recipe_governance_guard.sh validates fixture-level compliance.
evidence:
  - source_type: external
    citation: "Next.js documentation — App Router: each route segment is an independent module; cross-segment imports create bundle coupling that prevents per-route code splitting"
    url: "https://nextjs.org/docs/app/building-your-application/routing/colocation"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "Vercel — How we optimized package imports in Next.js: barrel imports and cross-segment coupling prevent tree-shaking and inflate route bundle sizes"
    url: "https://vercel.com/blog/how-we-optimized-package-imports-in-next-js"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "토스 기술 블로그 — FE 플랫폼: 도메인별 번들 분리를 통해 route segment 간 의존을 끊고 각 도메인 번들이 독립적으로 로드되도록 합니다"
    url: "https://toss.tech/article/toss-frontend-chapter"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## prefer-recipe-composition-over-l4-cross-import (React/Next.js)

**Impact: HIGH — Next.js L4 route segments are independent modules. A page that imports hooks and stores from 2+ sibling L4 domains to replicate a Recipe composition couples those segments and prevents per-route tree-shaking. The composition must be declared via `applied_recipe:` on the domain README.**

**Incorrect — SaaS page wires multiple L4 hooks without recipe declaration:**

```typescript
// VIOLATION: templates/L4/saas/app/(saas)/dashboard/page.tsx
// imports from billing L4, feature-flags L4, and notification L4
// without applied_recipe: in templates/L4/saas/README.md
"use client";

// VIOLATION: ad-hoc cross-L4 imports — duplicates saas-subscription RECIPE.md
import { useSubscription } from "templates/L4/billing/hooks/useSubscription";
import { useFeatureFlags } from "templates/L4/feature-flags/hooks/useFeatureFlags";
import { useNotificationBanner } from "templates/L4/notification/hooks/useNotificationBanner";

export default function SaasDashboardPage() {
  // coupling billing + feature-flags + notification bundles into one route
  const { plan, usage } = useSubscription();
  const { isPremium } = useFeatureFlags("premium_features");
  const banner = useNotificationBanner();

  return (
    <div>
      <h1>Dashboard — {plan}</h1>
      {isPremium && <PremiumFeatures />}
      {banner && <Banner message={banner} />}
    </div>
  );
}
```

**Correct — domain README declares applied_recipe; wiring matches recipe contract:**

```typescript
// CORRECT: templates/L4/saas/README.md declares:
//   applied_recipe: saas-subscription
// recipe_governance_guard.sh validates this page's imports match RECIPE.md.
"use client";

// Still imports from billing + feature-flags + notification, but the recipe
// declaration makes the composition explicit and tooling-verifiable.
import { useSubscription } from "templates/L4/billing/hooks/useSubscription";
import { useFeatureFlags } from "templates/L4/feature-flags/hooks/useFeatureFlags";
import { useNotificationBanner } from "templates/L4/notification/hooks/useNotificationBanner";

// ✅ applied_recipe: saas-subscription in README.md = guard passes
export default function SaasDashboardPage() {
  const { plan, usage } = useSubscription();
  const { isPremium } = useFeatureFlags("premium_features");
  const banner = useNotificationBanner();
  return <div>...</div>;
}
```

### Allowed vs. prohibited

| Pattern | Allowed? | Reason |
|---|---|---|
| Single L4 domain imports shared L1/L2/hooks | ✅ | Below L4 layer — no coupling |
| L4 domain with `applied_recipe:` imports from recipe's `enabled_l4_domains` | ✅ | Declared, guard-verified |
| L4 domain without `applied_recipe:` imports from 2+ sibling L4 domains | ❌ | Ad-hoc recipe duplicate |

## Failing fixture

See: `practices/evals/fixtures/prefer-recipe-composition-over-l4-cross-import/fail_ad_hoc_cross_import/SaasPage.tsx` — three L4 cross-imports without recipe declaration.

See: `practices/evals/fixtures/prefer-recipe-composition-over-l4-cross-import/pass/SaasPage.tsx` — same imports with companion README declaring `applied_recipe: saas-subscription`.

Reference: https://nextjs.org/docs/app/building-your-application/routing/colocation
