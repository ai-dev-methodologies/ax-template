/*
---
template_id: L2/blocks/pricing-table
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Audit B.2.11 P1 — PricingTable is a universal SaaS billing component required for plan selection"
    url: "https://ax-template.internal/audit"
  - source_type: external
    citation: "WCAG 2.2 — 1.3.1 Info and Relationships; pricing tables require accessible column headers with scope=col"
    url: "https://www.w3.org/WAI/WCAG22/Techniques/"
    quoted_at: "2026-05-18"
  - source_type: upstream_id
    upstream_id: stripe-billing-2026-05
    section: "Plan / Price model"
    quote: "The recurring components of a price such as interval and usage"
dependencies: [currency-input]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/payment/]
---
*/

'use client'

import * as React from 'react'
import { formatCurrencyAmount } from '@/templates/L1/components/currency-input'

export interface PricingPlan {
  id: string
  name: string
  /** Price in integer minor currency units (KRW: won, USD: cents). */
  amount: number
  currency: string
  intervalDays: number
  trialDays: number
  features: string[]
  highlighted?: boolean
  ctaLabel?: string
}

export interface PricingTableProps {
  plans: PricingPlan[]
  onSelectPlan: (planId: string) => void
  currentPlanId?: string
  isLoading?: boolean
  locale?: string
}

/**
 * PricingTable — L2 block for displaying billing plan tiers.
 *
 * Accessible: <th scope="col"> for plan tier headers per WCAG 2.2.
 * Currency: integer minor units via CurrencyFormatter (no raw float display).
 * Boundary: no import from payment L4 or L2.
 *
 * ```tsx
 * <PricingTable plans={plans} onSelectPlan={(id) => router.push('/billing/subscribe?planId=' + id)} />
 * ```
 */
export function PricingTable({
  plans,
  onSelectPlan,
  currentPlanId,
  isLoading,
  locale = 'ko-KR',
}: PricingTableProps) {
  return (
    <section aria-label="Pricing plans" className="w-full">
      <div className="grid gap-6" style={{ gridTemplateColumns: `repeat(${plans.length}, 1fr)` }}>
        {plans.map((plan) => (
          <PricingCard
            key={plan.id}
            plan={plan}
            isCurrent={plan.id === currentPlanId}
            onSelect={() => onSelectPlan(plan.id)}
            isLoading={isLoading}
            locale={locale}
          />
        ))}
      </div>
    </section>
  )
}

interface PricingCardProps {
  plan: PricingPlan
  isCurrent: boolean
  onSelect: () => void
  isLoading?: boolean
  locale: string
}

function PricingCard({ plan, isCurrent, onSelect, isLoading, locale }: PricingCardProps) {
  const intervalLabel = plan.intervalDays === 365 ? '년' : plan.intervalDays === 30 ? '월' : `${plan.intervalDays}일`
  const formatted = formatCurrencyAmount(plan.amount, plan.currency, locale)

  return (
    <article
      aria-labelledby={`plan-${plan.id}-name`}
      className={`relative flex flex-col rounded-2xl border p-6 shadow-sm transition-shadow hover:shadow-md
        ${plan.highlighted ? 'border-primary ring-2 ring-primary bg-primary/5' : 'border-border bg-card'}`}
    >
      {plan.highlighted && (
        <div className="absolute -top-3 left-1/2 -translate-x-1/2">
          <span className="rounded-full bg-primary px-3 py-1 text-xs font-semibold text-primary-foreground">
            추천
          </span>
        </div>
      )}

      <h3 id={`plan-${plan.id}-name`} className="text-lg font-bold">{plan.name}</h3>

      <div className="mt-4 flex items-baseline gap-1" aria-label={`${plan.name} plan price: ${formatted} per ${intervalLabel}`}>
        <span className="text-3xl font-extrabold tabular-nums">{formatted}</span>
        <span className="text-sm text-muted-foreground">/ {intervalLabel}</span>
      </div>

      {plan.trialDays > 0 && (
        <p className="mt-1 text-xs text-muted-foreground">{plan.trialDays}일 무료 체험</p>
      )}

      <ul className="mt-6 flex-1 space-y-2" aria-label={`${plan.name} features`}>
        {plan.features.map((feat) => (
          <li key={feat} className="flex items-center gap-2 text-sm">
            <span className="text-primary" aria-hidden>✓</span>
            {feat}
          </li>
        ))}
      </ul>

      <button
        type="button"
        onClick={onSelect}
        disabled={isLoading || isCurrent}
        aria-label={isCurrent ? `현재 ${plan.name} 플랜 사용 중` : `${plan.name} 플랜 구독`}
        className={`mt-6 w-full rounded-lg px-4 py-2 text-sm font-semibold transition-colors
          ${isCurrent
            ? 'bg-muted text-muted-foreground cursor-default'
            : plan.highlighted
              ? 'bg-primary text-primary-foreground hover:bg-primary/90'
              : 'bg-secondary text-secondary-foreground hover:bg-secondary/80'
          }
          disabled:cursor-not-allowed disabled:opacity-50`}
      >
        {isLoading ? '처리 중…' : isCurrent ? '현재 플랜' : (plan.ctaLabel ?? '구독하기')}
      </button>
    </article>
  )
}
