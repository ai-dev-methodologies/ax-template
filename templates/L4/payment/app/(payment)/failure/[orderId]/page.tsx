/*
---
template_id: L4/payment/app/(payment)/failure/[orderId]/page
layer: L4
domain: payment
domain_mode: full_trio
backend_operation_id: getPayment
evidence:
  - source_type: internal
    rationale: "L4 payment vertical — FAILURE page. Renders an error state for a failed payment identified by orderId. Provides retry link back to /checkout. Page is idempotent (safe for webhook redirects)."
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
  failureReason?: string
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

// ─── failure reason labels ────────────────────────────────────────────────────

const FAILURE_REASON_LABELS: Record<string, string> = {
  CARD_DECLINED: 'Your card was declined. Please try a different payment method.',
  INSUFFICIENT_FUNDS: 'Insufficient funds. Please check your account balance.',
  EXPIRED_CARD: 'Your card has expired. Please use a valid card.',
  INVALID_CVC: 'The security code (CVC) is incorrect. Please try again.',
  PROVIDER_TIMEOUT: 'The payment provider did not respond in time. No charge was made.',
  UNKNOWN: 'An unexpected error occurred. Please try again or contact support.',
}

function getFailureLabel(reason?: string): string {
  if (!reason) return FAILURE_REASON_LABELS.UNKNOWN
  return FAILURE_REASON_LABELS[reason] ?? FAILURE_REASON_LABELS.UNKNOWN
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * PaymentFailurePage — L4 payment failure page.
 *
 * Idempotency guarantee: the same orderId always renders the same error state.
 * Safe for payment provider webhook redirects — no side effects on render.
 *
 * Fork instructions:
 *   1. Replace fetch with your API client or tRPC query.
 *   2. Add support contact / help link for specific failure reasons.
 *   3. Implement analytics tracking for payment failures.
 *   4. Update retry href if your checkout URL differs.
 */
export default function PaymentFailurePage({ params }: { params: { orderId: string } }) {
  const { orderId } = params

  const { data: payment, isLoading } = useQuery<PaymentResponse>({
    queryKey: ['payments', orderId],
    queryFn: () => fetchPayment(orderId),
    // Don't retry on failure page — just show what we have
    retry: 0,
  })

  if (isLoading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" aria-label="Loading payment details" />
      </div>
    )
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-lg">
      {/* Failure header */}
      <div className="mb-8 text-center">
        <div
          aria-hidden="true"
          className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-red-100 text-3xl dark:bg-red-900/30"
        >
          ✕
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-destructive">
          Payment failed
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          {payment
            ? getFailureLabel(payment.failureReason)
            : 'Your payment could not be processed.'}
        </p>
      </div>

      {/* Error details */}
      {payment && (
        <div className="mb-6 rounded-lg border border-destructive/30 bg-destructive/5 px-6 py-4">
          <dl className="space-y-2">
            <div className="flex justify-between">
              <dt className="text-sm font-medium text-muted-foreground">Order ID</dt>
              <dd className="text-sm font-mono">{payment.orderId}</dd>
            </div>
            {payment.amount > 0 && (
              <div className="flex justify-between">
                <dt className="text-sm font-medium text-muted-foreground">Amount</dt>
                <dd className="text-sm tabular-nums">
                  {formatAmount(payment.amount, payment.currency)}
                </dd>
              </div>
            )}
            <div className="flex justify-between">
              <dt className="text-sm font-medium text-muted-foreground">Status</dt>
              <dd>
                <span className="inline-flex items-center rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-medium text-red-800 dark:bg-red-900/30 dark:text-red-400">
                  {payment.status}
                </span>
              </dd>
            </div>
          </dl>
        </div>
      )}

      {/* Actions — retry link goes to checkout */}
      <div className="flex gap-3">
        <a
          href="/checkout"
          className="flex-1 inline-flex items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow hover:bg-primary/90 transition-colors"
        >
          Try again
        </a>
        <a
          href="/methods"
          className="flex-1 inline-flex items-center justify-center rounded-md border bg-background px-4 py-2 text-sm font-medium hover:bg-accent transition-colors"
        >
          Payment history
        </a>
      </div>

      <p className="mt-4 text-center text-xs text-muted-foreground">
        No charge was made for this transaction.
      </p>
    </div>
  )
}
