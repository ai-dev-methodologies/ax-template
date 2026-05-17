/*
---
template_id: L4/payment/app/(payment)/methods/[id]/page
layer: L4
domain: payment
domain_mode: full_trio
backend_operation_id: getPayment
evidence:
  - source_type: internal
    rationale: "L4 payment vertical — PAYMENT DETAIL page. Renders full payment details for a given payment id via getPayment endpoint. Shows receipt, status, and available actions (refund/void)."
  - source_type: external
    citation: "Next.js 15 App Router dynamic routes — params prop for [id] segment"
    url: "https://nextjs.org/docs/app/building-your-application/routing/dynamic-routes"
  - source_type: external
    citation: "TanStack Query v5 — useQuery for server-state data fetching"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices]
---
*/
'use client'

import * as React from 'react'
import { useQuery } from '@tanstack/react-query'
import DetailPage from 'templates/L3/pages/detail-page/[id]/page'

// ─── types ──────────────────────────────────────────────────────────────────

type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'UNKNOWN' | 'REFUNDED' | 'VOIDED'

interface PaymentResponse {
  id: string
  orderId: string
  status: PaymentStatus
  amount: number
  currency: string
  paymentMethod: string
  createdAt: string
  providerRef?: string
  failureReason?: string
}

// ─── fetcher ────────────────────────────────────────────────────────────────

async function fetchPayment(id: string): Promise<PaymentResponse> {
  const res = await fetch(`/api/payments/${id}`, {
    headers: { 'Content-Type': 'application/json' },
  })
  // PAYMENT-AUTHZ-003: returns 404 for cross-user access (not 403)
  if (res.status === 404) throw new Error('Payment not found')
  if (!res.ok) throw new Error(`Failed to fetch payment: ${res.status}`)
  return res.json() as Promise<PaymentResponse>
}

// ─── helpers ─────────────────────────────────────────────────────────────────

function formatAmount(amount: number, currency: string): string {
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency,
      minimumFractionDigits: 0,
    }).format(amount / 100)
  } catch {
    return `${currency} ${(amount / 100).toFixed(2)}`
  }
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const REFUNDABLE_STATUSES: PaymentStatus[] = ['COMPLETED']
const VOIDABLE_STATUSES: PaymentStatus[] = ['PENDING']

// ─── component ──────────────────────────────────────────────────────────────

/**
 * PaymentDetailPage — L4 payment detail page.
 *
 * Composes:
 *   L3 detail-page → page chrome (title, back link, actions slot, sections slot)
 *
 * IDOR protection: fetch returns 404 (not 403) for cross-user access per
 * PAYMENT-AUTHZ-003.
 *
 * Fork instructions:
 *   1. Replace fetch with your API client or tRPC query.
 *   2. Add order line items to sectionsSlot.
 *   3. Implement actual void action (POST /api/payments/{id}/void).
 *   4. Add admin override UI if ADMIN role is required.
 */
export default function PaymentDetailPage({ params }: { params: { id: string } }) {
  const { id } = params

  const { data: payment, isLoading, isError } = useQuery<PaymentResponse>({
    queryKey: ['payments', id],
    queryFn: () => fetchPayment(id),
  })

  if (isLoading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" aria-label="Loading" />
      </div>
    )
  }

  if (isError || !payment) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-2xl">
        <div role="alert" className="rounded-lg border border-destructive/40 bg-destructive/5 px-6 py-4 text-sm text-destructive">
          Payment not found or you do not have permission to view it.
          <a href="/methods" className="ml-2 underline hover:no-underline">
            Back to payment history
          </a>
        </div>
      </div>
    )
  }

  const canRefund = REFUNDABLE_STATUSES.includes(payment.status)
  const canVoid   = VOIDABLE_STATUSES.includes(payment.status)

  const sectionsSlot = (
    <dl className="divide-y rounded-lg border bg-card">
      <div className="grid grid-cols-3 gap-4 px-6 py-4">
        <dt className="text-sm font-medium text-muted-foreground">Order ID</dt>
        <dd className="col-span-2 text-sm font-mono">{payment.orderId}</dd>
      </div>

      <div className="grid grid-cols-3 gap-4 px-6 py-4">
        <dt className="text-sm font-medium text-muted-foreground">Status</dt>
        <dd className="col-span-2 text-sm font-medium">{payment.status}</dd>
      </div>

      <div className="grid grid-cols-3 gap-4 px-6 py-4">
        <dt className="text-sm font-medium text-muted-foreground">Amount</dt>
        <dd className="col-span-2 text-sm tabular-nums font-bold">
          {formatAmount(payment.amount, payment.currency)}
        </dd>
      </div>

      <div className="grid grid-cols-3 gap-4 px-6 py-4">
        <dt className="text-sm font-medium text-muted-foreground">Method</dt>
        <dd className="col-span-2 text-sm">{payment.paymentMethod}</dd>
      </div>

      <div className="grid grid-cols-3 gap-4 px-6 py-4">
        <dt className="text-sm font-medium text-muted-foreground">Date</dt>
        <dd className="col-span-2 text-sm">{formatDate(payment.createdAt)}</dd>
      </div>

      {payment.providerRef && (
        <div className="grid grid-cols-3 gap-4 px-6 py-4">
          <dt className="text-sm font-medium text-muted-foreground">Reference</dt>
          <dd className="col-span-2 text-sm font-mono text-muted-foreground">
            {payment.providerRef}
          </dd>
        </div>
      )}

      {payment.failureReason && (
        <div className="grid grid-cols-3 gap-4 px-6 py-4">
          <dt className="text-sm font-medium text-muted-foreground">Failure</dt>
          <dd className="col-span-2 text-sm text-destructive">{payment.failureReason}</dd>
        </div>
      )}
    </dl>
  )

  const actionsSlot = (
    <div className="flex gap-2">
      {canRefund && (
        <a
          href={`/refund/${id}`}
          className="inline-flex items-center rounded-md border bg-background px-4 py-2 text-sm font-medium hover:bg-accent transition-colors"
        >
          Request refund
        </a>
      )}
      {canVoid && (
        <button
          type="button"
          onClick={() => {
            // Fork: implement void via POST /api/payments/{id}/void
            window.location.href = `/methods/${id}`
          }}
          className="inline-flex items-center rounded-md border border-destructive/40 bg-destructive/5 px-4 py-2 text-sm font-medium text-destructive hover:bg-destructive/10 transition-colors"
        >
          Void payment
        </button>
      )}
    </div>
  )

  return (
    <DetailPage
      title={`Payment ${payment.orderId}`}
      backHref="/methods"
      backLabel="Back to payment history"
      actionsSlot={actionsSlot}
      sectionsSlot={sectionsSlot}
    />
  )
}
