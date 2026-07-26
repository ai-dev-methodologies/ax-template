/*
---
template_id: L4/payment/app/(payment)/refund/[orderId]/page
layer: L4
domain: payment
domain_mode: full_trio
backend_operation_id: refundPayment
evidence:
  - source_type: internal
    rationale: "L4 payment vertical — REFUND REQUEST page. Renders a refund form for a given orderId. Calls POST /api/payments/{id}/refund. Owner-only per PAYMENT-AUTHZ-002."
  - source_type: external
    citation: "TanStack Query v5 — useMutation for POST requests"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useMutation"
  - source_type: external
    citation: "OWASP ASVS V4.2.1 — authorization checks for financial mutations"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
provenance_class: internal_design
imports_from: [L0, L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices]
---
*/
'use client'

import * as React from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { fractionDigitsFor, parseMinor, toMajorUnits } from 'templates/L0/fork-receiver-kit/money'

// ─── types ──────────────────────────────────────────────────────────────────

interface PaymentResponse {
  id: string
  orderId: string
  status: string
  amount: number
  currency: string
  paymentMethod: string
  createdAt: string
}

interface RefundRequest {
  paymentId: string
  amount?: number   // Optional: partial refund in integer MINOR units. Full refund if omitted.
  reason?: string
}

interface RefundResponse {
  id: string
  paymentId: string
  amount: number
  currency: string
  status: 'PENDING' | 'COMPLETED' | 'FAILED'
  createdAt: string
}

// ─── fetcher ────────────────────────────────────────────────────────────────

async function fetchPayment(id: string): Promise<PaymentResponse> {
  const res = await fetch(`/api/payments/${id}`, {
    headers: { 'Content-Type': 'application/json' },
  })
  if (res.status === 404) throw new Error('Payment not found')
  if (!res.ok) throw new Error(`Failed to fetch payment: ${res.status}`)
  return res.json() as Promise<PaymentResponse>
}

async function requestRefund(req: RefundRequest): Promise<RefundResponse> {
  const res = await fetch(`/api/payments/${req.paymentId}/refund`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      amount: req.amount,
      reason: req.reason,
    }),
  })
  // PAYMENT-AUTHZ-002: 403 if caller is not the payment owner
  if (res.status === 403) throw new Error('You are not authorised to refund this payment.')
  if (!res.ok) throw new Error(`Refund request failed: ${res.status}`)
  return res.json() as Promise<RefundResponse>
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

// ─── component ──────────────────────────────────────────────────────────────

/**
 * RefundPage — L4 payment refund request page.
 *
 * Authorization: only the payment owner may request a refund (PAYMENT-AUTHZ-002).
 * The backend enforces this via @PreAuthorize; the UI shows a 403 error gracefully.
 *
 * Fork instructions:
 *   1. Replace fetch calls with your API client or tRPC mutations.
 *   2. Add partial refund support by wiring an amount input.
 *   3. Add reason code selection if your provider requires it.
 *   4. Wire the success redirect to your order management page.
 */
export default function RefundPage({ params }: { params: { orderId: string } }) {
  const { orderId } = params
  const queryClient = useQueryClient()
  const [reason, setReason] = React.useState('')
  const [error, setError] = React.useState<string | null>(null)
  const [succeeded, setSucceeded] = React.useState(false)

  const { data: payment, isLoading: paymentLoading, isError: paymentError } = useQuery<PaymentResponse>({
    queryKey: ['payments', orderId],
    queryFn: () => fetchPayment(orderId),
  })

  const refundMutation = useMutation({
    mutationFn: requestRefund,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] })
      setSucceeded(true)
    },
    onError: (err: Error) => {
      setError(err.message ?? 'Refund request failed. Please try again.')
    },
  })

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)
    if (!payment) return
    refundMutation.mutate({
      paymentId: orderId,
      reason: reason.trim() || undefined,
      // Fork: add partial amount input and pass it here
    })
  }

  if (paymentLoading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" aria-label="Loading" />
      </div>
    )
  }

  if (paymentError || !payment) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-lg">
        <div role="alert" className="rounded-lg border border-destructive/40 bg-destructive/5 px-6 py-4 text-sm text-destructive">
          Payment not found or you do not have permission to refund it.
          <a href="/methods" className="ml-2 underline hover:no-underline">
            Back to payment history
          </a>
        </div>
      </div>
    )
  }

  if (succeeded) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-lg text-center">
        <div
          aria-hidden="true"
          className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-green-100 text-3xl dark:bg-green-900/30"
        >
          ✓
        </div>
        <h1 className="text-2xl font-bold text-green-700 dark:text-green-400">
          Refund requested
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Your refund request has been submitted. Funds typically appear within 5–10 business days.
        </p>
        <a
          href="/methods"
          className="mt-6 inline-flex items-center justify-center rounded-md bg-primary px-6 py-2.5 text-sm font-medium text-primary-foreground shadow hover:bg-primary/90 transition-colors"
        >
          Back to payment history
        </a>
      </div>
    )
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-lg">
      <div className="mb-6">
        <a
          href={`/methods/${orderId}`}
          className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground transition-colors"
        >
          ← Back to payment details
        </a>
      </div>

      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight">Request a refund</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Refund request for order {payment.orderId}.
        </p>
      </div>

      {/* Payment summary */}
      <div className="mb-6 rounded-lg border bg-card px-6 py-4">
        <dl className="space-y-2">
          <div className="flex justify-between">
            <dt className="text-sm font-medium text-muted-foreground">Amount</dt>
            <dd className="text-sm font-bold tabular-nums">
              {formatAmount(payment.amount, payment.currency)}
            </dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-sm font-medium text-muted-foreground">Method</dt>
            <dd className="text-sm">{payment.paymentMethod}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-sm font-medium text-muted-foreground">Status</dt>
            <dd className="text-sm">{payment.status}</dd>
          </div>
        </dl>
      </div>

      {/* Refund form */}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="space-y-2">
          <label htmlFor="refund-reason" className="text-sm font-medium leading-none">
            Reason <span className="text-muted-foreground">(optional)</span>
          </label>
          <textarea
            id="refund-reason"
            rows={3}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            disabled={refundMutation.isPending}
            placeholder="Describe the reason for the refund…"
            className="flex w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 resize-none"
          />
        </div>

        {error && (
          <div role="alert" className="rounded-md bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {error}
          </div>
        )}

        <div className="flex gap-3">
          <a
            href={`/methods/${orderId}`}
            className="flex-1 inline-flex items-center justify-center rounded-md border bg-background px-4 py-2 text-sm font-medium hover:bg-accent transition-colors"
          >
            Cancel
          </a>
          <button
            type="submit"
            disabled={refundMutation.isPending}
            className="flex-1 inline-flex items-center justify-center rounded-md bg-destructive px-4 py-2 text-sm font-medium text-destructive-foreground shadow hover:bg-destructive/90 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50 transition-colors"
          >
            {refundMutation.isPending ? 'Submitting…' : 'Request full refund'}
          </button>
        </div>
      </form>
    </div>
  )
}
