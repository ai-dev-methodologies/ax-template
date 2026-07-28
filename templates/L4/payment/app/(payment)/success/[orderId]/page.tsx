/*
---
template_id: L4/payment/app/(payment)/success/[orderId]/page
layer: L4
domain: payment
domain_mode: full_trio
backend_operation_id: getPayment
evidence:
  - source_type: internal
    rationale: "L4 payment vertical — SUCCESS page. Renders a receipt view for a completed payment. Page is idempotent: re-render for same orderId always returns the same state (safe for webhook redirects from payment providers)."
  - source_type: external
    citation: "Next.js 15 App Router dynamic routes — params prop for [orderId] segment"
    url: "https://nextjs.org/docs/app/building-your-application/routing/dynamic-routes"
  - source_type: external
    citation: "TanStack Query v5 — useQuery for server-state data fetching"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
provenance_class: internal_design
imports_from: [L0, L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices]
---
*/
'use client'

import * as React from 'react'
import { useQuery } from '@tanstack/react-query'
import PaymentSuccessView, { type PaymentResponse } from './payment-success-view'

// ─── fetcher ────────────────────────────────────────────────────────────────

async function fetchPayment(orderId: string): Promise<PaymentResponse> {
  const res = await fetch(`/api/payments/${orderId}`, {
    headers: { 'Content-Type': 'application/json' },
  })
  if (res.status === 404) throw new Error('Payment not found')
  if (!res.ok) throw new Error(`Failed to fetch payment: ${res.status}`)
  return res.json() as Promise<PaymentResponse>
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * PaymentSuccessPage — L4 payment success / receipt page.
 *
 * Idempotency guarantee: the same orderId always renders the same receipt.
 * Safe for payment provider webhook redirects — no side effects on render.
 *
 * Fork instructions:
 *   1. Replace fetch with your API client or tRPC query.
 *   2. Add order details (items, shipping, tax) to the receipt.
 *   3. Add "Download PDF receipt" or email trigger if required.
 *   4. Update the home link to your app's dashboard.
 */
export default function PaymentSuccessPage({ params }: { params: { orderId: string } }) {
  const { orderId } = params

  const { data: payment, isLoading, isError } = useQuery<PaymentResponse>({
    queryKey: ['payments', orderId],
    queryFn: () => fetchPayment(orderId),
  })

  if (isLoading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" aria-label="Loading payment details" />
      </div>
    )
  }

  if (isError || !payment) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-lg">
        <div role="alert" className="rounded-lg border border-destructive/40 bg-destructive/5 px-6 py-4 text-sm text-destructive">
          Could not load payment details.
          <a href="/methods" className="ml-2 underline hover:no-underline">
            View payment history
          </a>
        </div>
      </div>
    )
  }

  return <PaymentSuccessView payment={payment} />
}
