// CLEAN — the feature container the thin route delegates to. Composed from
// catalog L2 blocks (PricingTable = plan cards + upgrade CTA button,
// UsageMeter = usage widget, BillingHistory + Pagination = paginated
// invoice/event list) plus the L0 use-url-list-state hook for URL-backed
// page state and the parse-page-envelope contract module. State updates are
// immutable throughout (no ax/no-array-mutate-on-state, no
// ax/no-server-state-in-local-state) and this file stays out of app/, so it
// never trips ax/no-god-route (that rule only fires on app/**/(page|layout)).

'use client'

import * as React from 'react'
import { PricingTable, type PricingPlan } from '@/../templates/L2/blocks/pricing-table'
import { UsageMeter } from '@/../templates/L2/blocks/usage-meter'
import { BillingHistory, type BillingHistoryItem } from '@/../templates/L2/blocks/billing-history'
import Pagination from '@/../templates/L2/blocks/pagination'
import { useUrlListState } from '@/../templates/L0/fork-receiver-kit/use-url-list-state'
import { parsePageEnvelope } from './parse-page-envelope'

const PLANS: PricingPlan[] = [
  { id: 'starter', name: 'Starter', amount: 0, currency: 'KRW', intervalDays: 30, trialDays: 0, features: ['API 1,000회/월'] },
  { id: 'pro', name: 'Pro', amount: 29000, currency: 'KRW', intervalDays: 30, trialDays: 14, features: ['API 50,000회/월', '우선 지원'], highlighted: true },
  { id: 'team', name: 'Team', amount: 99000, currency: 'KRW', intervalDays: 30, trialDays: 14, features: ['API 무제한', '전담 매니저'] },
]

/** Server response shape for GET /api/billing/history — the canonical envelope. */
interface BillingHistoryResponse {
  data: BillingHistoryItem[]
  pagination: { page: number; pageSize: number; totalElements: number; totalPages: number; hasMore: boolean }
}

async function fetchBillingHistory(page: number, pageSize: number): Promise<BillingHistoryResponse> {
  const res = await fetch(`/api/billing/history?page=${page}&pageSize=${pageSize}`)
  return res.json()
}

export function SubscriptionDashboard() {
  const [currentPlanId, setCurrentPlanId] = React.useState('pro')
  const [changingPlan, setChangingPlan] = React.useState(false)
  const [events, setEvents] = React.useState<BillingHistoryItem[]>([])
  const [totalEvents, setTotalEvents] = React.useState(0)
  const { page, pageSize, setPage } = useUrlListState({ defaultPageSize: 10 })

  React.useEffect(() => {
    let cancelled = false
    fetchBillingHistory(page, pageSize).then((raw) => {
      if (cancelled) return
      const parsed = parsePageEnvelope<BillingHistoryItem>(raw)
      // Immutable — full replacement from the server response, never a
      // mutation of the previous array.
      setEvents(parsed.items)
      setTotalEvents(parsed.total)
    })
    return () => {
      cancelled = true
    }
  }, [page, pageSize])

  function handleSelectPlan(planId: string) {
    setChangingPlan(true)
    setCurrentPlanId(planId)
    setChangingPlan(false)
  }

  return (
    <div className="space-y-8">
      <PricingTable
        plans={PLANS}
        currentPlanId={currentPlanId}
        isLoading={changingPlan}
        onSelectPlan={handleSelectPlan}
      />

      <UsageMeter label="API Calls" usage={32450} limit={50000} unit="calls" />

      <section>
        <BillingHistory events={events} />
        <Pagination page={page} pageSize={pageSize} total={totalEvents} onPageChange={setPage} />
      </section>
    </div>
  )
}
