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
import { fractionDigitsFor, parseMinor, toMajorUnits } from 'templates/L0/fork-receiver-kit/money'

// ─── types ──────────────────────────────────────────────────────────────────

interface PaymentResponse {
  id: string
  orderId: string
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'UNKNOWN'
  amount: number
  currency: string
  createdAt: string
  providerRef?: string
}

// ─── fetcher ────────────────────────────────────────────────────────────────

async function fetchPayment(orderId: string): Promise<PaymentResponse> {
  const res = await fetch(`/api/payments/${orderId}`, {
    headers: { 'Content-Type': 'application/json' },
  })
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

  return (
    <div className="container mx-auto px-4 py-8 max-w-lg">
      {/* Success header */}
      <div className="mb-8 text-center">
        <div
          aria-hidden="true"
          className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-green-100 text-3xl dark:bg-green-900/30"
        >
          ✓
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-green-700 dark:text-green-400">
          Payment successful
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Your payment has been processed. A confirmation will be sent to your email.
        </p>
      </div>

      {/* Receipt card */}
      <div className="rounded-lg border bg-card shadow-sm">
        <div className="border-b px-6 py-4">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
            Receipt
          </h2>
        </div>

        <dl className="divide-y">
          <div className="grid grid-cols-2 gap-4 px-6 py-4">
            <dt className="text-sm font-medium text-muted-foreground">Order ID</dt>
            <dd className="text-sm font-mono text-right">{payment.orderId}</dd>
          </div>

          <div className="grid grid-cols-2 gap-4 px-6 py-4">
            <dt className="text-sm font-medium text-muted-foreground">Status</dt>
            <dd className="text-right">
              <span className="inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800 dark:bg-green-900/30 dark:text-green-400">
                {payment.status}
              </span>
            </dd>
          </div>

          <div className="grid grid-cols-2 gap-4 px-6 py-4">
            <dt className="text-sm font-medium text-muted-foreground">Amount</dt>
            <dd className="text-sm font-bold tabular-nums text-right">
              {formatAmount(payment.amount, payment.currency)}
            </dd>
          </div>

          <div className="grid grid-cols-2 gap-4 px-6 py-4">
            <dt className="text-sm font-medium text-muted-foreground">Date</dt>
            <dd className="text-sm text-right">{formatDate(payment.createdAt)}</dd>
          </div>

          {payment.providerRef && (
            <div className="grid grid-cols-2 gap-4 px-6 py-4">
              <dt className="text-sm font-medium text-muted-foreground">Reference</dt>
              <dd className="text-sm font-mono text-right text-muted-foreground">
                {payment.providerRef}
              </dd>
            </div>
          )}
        </dl>
      </div>

      {/* Actions */}
      <div className="mt-6 flex gap-3">
        <a
          href="/methods"
          className="flex-1 inline-flex items-center justify-center rounded-md border bg-background px-4 py-2 text-sm font-medium hover:bg-accent transition-colors"
        >
          View payment history
        </a>
        <a
          href="/"
          className="flex-1 inline-flex items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow hover:bg-primary/90 transition-colors"
        >
          Back to home
        </a>
      </div>
    </div>
  )
}
