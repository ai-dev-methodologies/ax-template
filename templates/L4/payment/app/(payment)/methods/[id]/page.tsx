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
imports_from: [L0, L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices]
---
*/
'use client'

import * as React from 'react'
import { useQuery } from '@tanstack/react-query'
import DetailPage from 'templates/L3/pages/detail-page/[id]/page'
import { fractionDigitsFor, parseMinor, toMajorUnits } from 'templates/L0/fork-receiver-kit/money'

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

/**
 * Format a wire amount for display. `amount` is integer MINOR units — the sole response encoding of
 * contracts/payment-openapi.yaml#MoneyAmount — so the major-unit value MUST be derived at the
 * currency's own ISO 4217 minor-unit width through the L0 money kit. A hard-coded `/ 100` renders
 * every zero-decimal currency 100x too small (canonical KRW 12900 → "₩129" instead of "₩12,900"),
 * and a hard-coded 2 fraction digits prints ₩ amounts with cents (BACKLOG P2-27, same defect class).
 */
function formatAmount(amount: number | string, currency: string): string {
  const digits = fractionDigitsFor(currency)
  let major: string
  try {
    major = toMajorUnits(parseMinor(amount), digits)
  } catch {
    // parseMinor throws on a non-integer wire value (major units leaked onto the wire). Show the
    // raw value rather than silently rendering a wrong number.
    return `${currency} ${String(amount)}`
  }
  try {
    // `major` is an exact decimal string; Number() is the display-edge conversion. A fork-receiver
    // on an ES2023 lib can pass `major` straight to format() (Intl.NumberFormat V3 string input).
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency,
      minimumFractionDigits: digits,
      maximumFractionDigits: digits,
    }).format(Number(major))
  } catch {
    // Unknown ISO code — Intl throws; `major` is still the correct major-unit value.
    return `${currency} ${major}`
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
