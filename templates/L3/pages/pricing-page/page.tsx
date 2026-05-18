/*
---
template_id: L3/pages/pricing-page
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Audit C.2 P1 — pricing-page is a required L3 page template for billing SaaS UI"
    url: "https://ax-template.internal/audit"
  - source_type: external
    citation: "Next.js 15 App Router file conventions — page.tsx"
    url: "https://nextjs.org/docs/app/building-your-application/routing/pages"
    quoted_at: "2026-05-18"
imports_from: [L1, L2]
imports_forbidden: [L4]
---
*/

import * as React from 'react'
import { PricingTable, PricingPlan } from '@/templates/L2/blocks/pricing-table'
import { PlanComparison, ComparisonFeature, ComparisonPlan } from '@/templates/L2/blocks/plan-comparison'

export interface PricingPageProps {
  /** Plans to display in the pricing table. */
  plans: PricingPlan[]
  /** Feature comparison rows. */
  comparisonFeatures?: ComparisonFeature[]
  /** Current user's plan ID (to highlight current plan). */
  currentPlanId?: string
  /** Called when user clicks a plan CTA. */
  onSelectPlan: (planId: string) => void
  isLoading?: boolean
  locale?: string
  /** Optional page title override. Default: "요금제 선택" */
  title?: string
  /** Optional page subtitle. */
  subtitle?: string
}

/**
 * PricingPage — L3 page template composing PricingTable + PlanComparison.
 *
 * L4 forks use this as the `/billing/pricing` route page.
 *
 * Usage:
 * ```tsx
 * // app/(billing)/pricing/page.tsx
 * import PricingPage from '@/templates/L3/pages/pricing-page/page'
 * export default function PricingRoute() {
 *   return <PricingPage plans={plans} onSelectPlan={handleSelect} />
 * }
 * ```
 */
export default function PricingPage({
  plans,
  comparisonFeatures,
  currentPlanId,
  onSelectPlan,
  isLoading,
  locale = 'ko-KR',
  title = '요금제 선택',
  subtitle,
}: PricingPageProps) {
  const comparisonPlans: ComparisonPlan[] = plans.map((p) => ({
    id: p.id,
    name: p.name,
    amount: p.amount,
    currency: p.currency,
    intervalDays: p.intervalDays,
  }))

  return (
    <main className="mx-auto max-w-6xl px-4 py-12 space-y-16">
      <header className="text-center space-y-3">
        <h1 className="text-4xl font-extrabold tracking-tight">{title}</h1>
        {subtitle && (
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">{subtitle}</p>
        )}
      </header>

      <section aria-label="요금제">
        <PricingTable
          plans={plans}
          onSelectPlan={onSelectPlan}
          currentPlanId={currentPlanId}
          isLoading={isLoading}
          locale={locale}
        />
      </section>

      {comparisonFeatures && comparisonFeatures.length > 0 && (
        <section aria-label="기능 비교">
          <h2 className="mb-6 text-2xl font-bold text-center">상세 기능 비교</h2>
          <PlanComparison
            plans={comparisonPlans}
            features={comparisonFeatures}
            onSelectPlan={onSelectPlan}
            currentPlanId={currentPlanId}
            locale={locale}
          />
        </section>
      )}
    </main>
  )
}
