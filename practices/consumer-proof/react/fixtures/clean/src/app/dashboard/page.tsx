// CLEAN — the correct rewrite of the god route.
// A thin "use client" route that delegates all form state + business logic + UI
// to a @/features/<f> container, staying well under the line threshold.
'use client'
import { OrderDashboard } from '@/features/orders'

export default function DashboardPage() {
  return (
    <main>
      <h1>Order Dashboard</h1>
      <OrderDashboard />
    </main>
  )
}
