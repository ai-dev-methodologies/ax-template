/*
---
template_id: L2/blocks/plan-comparison
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Audit B.2.11 P2→P1 — PlanComparison for feature-level plan diff"
    url: "https://ax-template.internal/audit"
  - source_type: external
    citation: "WCAG 2.2 — 1.3.1 Info and Relationships; comparison tables need scope=col headers"
    url: "https://www.w3.org/WAI/WCAG22/Techniques/"
    quoted_at: "2026-05-18"
dependencies: [pricing-table]
imports_from: [L1, L2]
imports_forbidden: [L4, app/, lib/payment/]
---
*/

'use client'

import * as React from 'react'
import { formatCurrencyAmount } from '@/templates/L1/components/currency-input'

export interface ComparisonFeature {
  category: string
  name: string
  /** Map of planId → value. true = included, false = excluded, string = custom label. */
  values: Record<string, boolean | string>
}

export interface ComparisonPlan {
  id: string
  name: string
  amount: number
  currency: string
  intervalDays: number
}

export interface PlanComparisonProps {
  plans: ComparisonPlan[]
  features: ComparisonFeature[]
  onSelectPlan?: (planId: string) => void
  currentPlanId?: string
  locale?: string
}

/**
 * PlanComparison — L2 feature comparison table for billing plans.
 *
 * Accessible: <th scope="col"> for each plan column per WCAG 2.2 BILLING-FE-003.
 * Grouped by feature category.
 * Boundary: no import from payment L4 or payment L2.
 *
 * ```tsx
 * <PlanComparison plans={plans} features={features} onSelectPlan={handleSelect} />
 * ```
 */
export function PlanComparison({
  plans,
  features,
  onSelectPlan,
  currentPlanId,
  locale = 'ko-KR',
}: PlanComparisonProps) {
  const categories = Array.from(new Set(features.map((f) => f.category)))

  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-sm" aria-label="Plan feature comparison">
        <thead>
          <tr>
            <th scope="col" className="py-3 text-left font-semibold text-muted-foreground">기능</th>
            {plans.map((plan) => (
              <th key={plan.id} scope="col" className="py-3 text-center font-bold">
                <div>{plan.name}</div>
                <div className="mt-1 text-base font-extrabold tabular-nums">
                  {formatCurrencyAmount(plan.amount, plan.currency, locale)}
                  <span className="text-xs font-normal text-muted-foreground ml-1">
                    / {plan.intervalDays === 365 ? '년' : '월'}
                  </span>
                </div>
                {onSelectPlan && (
                  <button
                    type="button"
                    onClick={() => onSelectPlan(plan.id)}
                    disabled={plan.id === currentPlanId}
                    aria-label={`Subscribe to ${plan.name} plan`}
                    className="mt-2 rounded px-3 py-1 text-xs font-semibold bg-primary text-primary-foreground
                               hover:bg-primary/90 disabled:opacity-50 disabled:cursor-default"
                  >
                    {plan.id === currentPlanId ? '현재' : '구독'}
                  </button>
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {categories.map((category) => (
            <React.Fragment key={category}>
              <tr>
                <td
                  colSpan={plans.length + 1}
                  className="pt-4 pb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground"
                >
                  {category}
                </td>
              </tr>
              {features
                .filter((f) => f.category === category)
                .map((feat) => (
                  <tr key={feat.name} className="border-t border-border/50">
                    <td className="py-2 text-muted-foreground">{feat.name}</td>
                    {plans.map((plan) => {
                      const val = feat.values[plan.id]
                      return (
                        <td key={plan.id} className="py-2 text-center">
                          {val === true ? (
                            <span aria-label="포함" className="text-primary font-bold">✓</span>
                          ) : val === false ? (
                            <span aria-label="미포함" className="text-muted-foreground">—</span>
                          ) : (
                            <span>{String(val)}</span>
                          )}
                        </td>
                      )
                    })}
                  </tr>
                ))}
            </React.Fragment>
          ))}
        </tbody>
      </table>
    </div>
  )
}
