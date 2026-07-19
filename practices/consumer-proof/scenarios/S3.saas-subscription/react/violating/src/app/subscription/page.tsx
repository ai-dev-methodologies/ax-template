// VIOLATING — ax/no-god-route
// A Next.js App Router "use client" route file that absorbed plan-change
// form state + usage-quota business logic + billing-history pagination +
// inline UI markup, instead of delegating to a @/features/subscription
// container — realistic shape an AI assistant produces when asked to "add a
// subscription dashboard page" without being pointed at the L2 blocks.
// Grows past the 100-line ax/no-god-route threshold. State updates are kept
// immutable on purpose so ONLY the god-route size signal fires here.
'use client'

import { useEffect, useState } from 'react'

interface BillingEvent {
  id: string
  eventType: string
  occurredAt: string
}

const PLANS = [
  { id: 'starter', name: 'Starter', amount: 0, features: ['API 1,000회/월'] },
  { id: 'pro', name: 'Pro', amount: 29000, features: ['API 50,000회/월', '우선 지원'] },
  { id: 'team', name: 'Team', amount: 99000, features: ['API 무제한', '전담 매니저'] },
]

export default function SubscriptionPage() {
  const [currentPlanId, setCurrentPlanId] = useState('pro')
  const [changingPlan, setChangingPlan] = useState(false)
  const [changeError, setChangeError] = useState<string | null>(null)
  const [confirmingDowngrade, setConfirmingDowngrade] = useState(false)
  const [pendingPlanId, setPendingPlanId] = useState<string | null>(null)

  const [usage, setUsage] = useState(0)
  const [limit, setLimit] = useState(50000)
  const [usageLoading, setUsageLoading] = useState(true)

  const [events, setEvents] = useState<BillingEvent[]>([])
  const [page, setPage] = useState(1)
  const [pageSize] = useState(10)
  const [totalEvents, setTotalEvents] = useState(0)
  const [historyLoading, setHistoryLoading] = useState(true)

  useEffect(() => {
    setUsageLoading(true)
    fetch('/api/billing/usage')
      .then((res) => res.json())
      .then((data) => {
        setUsage(data.usage)
        setLimit(data.limit)
        setUsageLoading(false)
      })
  }, [])

  useEffect(() => {
    setHistoryLoading(true)
    fetch(`/api/billing/history?page=${page}&pageSize=${pageSize}`)
      .then((res) => res.json())
      .then((data) => {
        setEvents(data.items)
        setTotalEvents(data.total)
        setHistoryLoading(false)
      })
  }, [page, pageSize])

  function isDowngrade(fromPlanId: string, toPlanId: string): boolean {
    const order = ['starter', 'pro', 'team']
    return order.indexOf(toPlanId) < order.indexOf(fromPlanId)
  }

  function requestPlanChange(planId: string) {
    if (planId === currentPlanId) return
    if (isDowngrade(currentPlanId, planId)) {
      setPendingPlanId(planId)
      setConfirmingDowngrade(true)
      return
    }
    applyPlanChange(planId)
  }

  function confirmDowngrade() {
    if (pendingPlanId) applyPlanChange(pendingPlanId)
    setConfirmingDowngrade(false)
    setPendingPlanId(null)
  }

  function cancelDowngrade() {
    setConfirmingDowngrade(false)
    setPendingPlanId(null)
  }

  function applyPlanChange(planId: string) {
    setChangingPlan(true)
    setChangeError(null)
    fetch('/api/billing/subscription', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ planId }),
    })
      .then((res) => {
        if (!res.ok) throw new Error('plan change failed')
        return res.json()
      })
      .then(() => {
        setCurrentPlanId(planId)
        setChangingPlan(false)
      })
      .catch(() => {
        setChangeError('플랜 변경에 실패했습니다.')
        setChangingPlan(false)
      })
  }

  const usagePercent = limit > 0 ? Math.round((usage / limit) * 100) : 0
  const totalPages = Math.max(1, Math.ceil(totalEvents / pageSize))

  return (
    <main className="space-y-8 p-8">
      <h1 className="text-xl font-bold">구독 관리</h1>

      <section aria-label="Pricing plans" className="grid grid-cols-3 gap-6">
        {PLANS.map((plan) => (
          <article key={plan.id} className="rounded-2xl border p-6">
            <h3 className="text-lg font-bold">{plan.name}</h3>
            <p className="mt-4 text-3xl font-extrabold">{plan.amount.toLocaleString()}원</p>
            <ul className="mt-6 space-y-2">
              {plan.features.map((feat) => (
                <li key={feat}>{feat}</li>
              ))}
            </ul>
            <button
              type="button"
              disabled={changingPlan || plan.id === currentPlanId}
              onClick={() => requestPlanChange(plan.id)}
              className="mt-6 w-full rounded-lg border px-4 py-2"
            >
              {plan.id === currentPlanId ? '현재 플랜' : '선택'}
            </button>
          </article>
        ))}
      </section>

      {changeError && <p role="alert">{changeError}</p>}

      {confirmingDowngrade && (
        <div role="alertdialog" className="rounded-lg border p-4">
          <p>다운그레이드하시겠습니까?</p>
          <button type="button" onClick={confirmDowngrade}>확인</button>
          <button type="button" onClick={cancelDowngrade}>취소</button>
        </div>
      )}

      <section aria-label="Usage">
        <h2 className="font-semibold">API Calls</h2>
        {usageLoading ? (
          <p>로딩 중...</p>
        ) : (
          <div
            role="progressbar"
            aria-valuenow={usagePercent}
            aria-valuemin={0}
            aria-valuemax={100}
            className="h-2 w-full rounded bg-muted"
          >
            <div className="h-2 rounded bg-primary" style={{ width: `${usagePercent}%` }} />
          </div>
        )}
        <p className="text-sm text-muted-foreground">{usage.toLocaleString()} / {limit.toLocaleString()} calls</p>
      </section>

      <section aria-label="Billing history">
        <h2 className="font-semibold">결제 내역</h2>
        {historyLoading ? (
          <p>로딩 중...</p>
        ) : (
          <ul>
            {events.map((event) => (
              <li key={event.id}>{event.eventType} — {event.occurredAt}</li>
            ))}
          </ul>
        )}
        <nav aria-label="Pagination">
          <button type="button" disabled={page <= 1} onClick={() => setPage(page - 1)}>이전</button>
          <span>{page} / {totalPages}</span>
          <button type="button" disabled={page >= totalPages} onClick={() => setPage(page + 1)}>다음</button>
        </nav>
      </section>
    </main>
  )
}
