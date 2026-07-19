// CLEAN — the correct rewrite of the subscription god route.
// A thin "use client" route that delegates all plan-change state, usage
// tracking, and billing-history/pagination logic to the
// @/features/subscription container, staying well under the line threshold.
'use client'
import { SubscriptionDashboard } from '@/features/subscription/SubscriptionDashboard'

export default function SubscriptionPage() {
  return (
    <main className="p-8">
      <h1 className="text-xl font-bold">구독 관리</h1>
      <SubscriptionDashboard />
    </main>
  )
}
