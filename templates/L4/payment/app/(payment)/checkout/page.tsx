/*
---
template_id: L4/payment/app/(payment)/checkout/page
layer: L4
domain: payment
domain_mode: full_trio
backend_operation_id: createPayment
evidence:
  - source_type: internal
    rationale: "L4 payment vertical — CHECKOUT page composing L2 PaymentCheckoutForm + PaymentMethodPicker + IdempotencyKeyHandler + SlowProviderWarning. Idempotency key lifecycle and 3s slow-provider warning per PAYMENT-PROVIDER-007."
  - source_type: external
    citation: "Stripe API — Idempotent Requests (idempotency key lifecycle)"
    url: "https://stripe.com/docs/api/idempotent_requests"
  - source_type: external
    citation: "TanStack Query v5 — useMutation for POST requests"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useMutation"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices]
---
*/
'use client'

import * as React from 'react'
import { useMutation } from '@tanstack/react-query'
import PaymentCheckoutForm, {
  type PaymentCheckoutFormValues,
} from 'templates/L2/blocks/payment-checkout-form'
import PaymentMethodPicker, {
  type PaymentMethod,
} from 'templates/L2/blocks/payment-method-picker'
import IdempotencyKeyHandler from 'templates/L2/blocks/idempotency-key-handler'
import SlowProviderWarning from 'templates/L2/blocks/slow-provider-warning'

// ─── types ──────────────────────────────────────────────────────────────────

interface CreatePaymentRequest {
  orderId: string
  amount: number
  currency: string
  paymentMethodToken: string
  idempotencyKey: string
}

interface PaymentResponse {
  id: string
  orderId: string
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'UNKNOWN'
  amount: number
  currency: string
  replayed?: boolean
}

// ─── constants ───────────────────────────────────────────────────────────────

/** Per PAYMENT-PROVIDER-007: show SlowProviderWarning after 3 000 ms */
const SLOW_PROVIDER_THRESHOLD_MS = 3_000

const DEFAULT_PAYMENT_METHODS: PaymentMethod[] = [
  { id: 'card', label: 'Credit / Debit Card', description: 'Visa, Mastercard, Amex' },
  { id: 'kakao_pay', label: 'Kakao Pay', description: 'Kakao Pay 간편결제' },
  { id: 'naver_pay', label: 'Naver Pay', description: 'Naver Pay 간편결제' },
  { id: 'bank_transfer', label: 'Bank Transfer', description: 'Online bank transfer (realtime)' },
]

// ─── fetcher ────────────────────────────────────────────────────────────────

async function createPayment(req: CreatePaymentRequest): Promise<PaymentResponse> {
  const res = await fetch('/api/payments', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': req.idempotencyKey,
    },
    body: JSON.stringify({
      orderId: req.orderId,
      amount: req.amount,
      currency: req.currency,
      paymentMethodToken: req.paymentMethodToken,
    }),
  })
  if (!res.ok && res.status !== 202) {
    throw new Error(`Payment request failed: ${res.status}`)
  }
  return res.json() as Promise<PaymentResponse>
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * CheckoutPage — L4 payment checkout page.
 *
 * Composes:
 *   L2 PaymentMethodPicker  → selectable payment method cards
 *   L2 PaymentCheckoutForm  → card fields + amount display + pay button
 *   L2 IdempotencyKeyHandler → manages Idempotency-Key lifecycle (render prop)
 *   L2 SlowProviderWarning  → shown after 3 000 ms per PAYMENT-PROVIDER-007
 *
 * Key UX policies (contracts/payment-ui.yaml):
 *   - Idempotency key generated on mount; regenerated after each successful payment
 *   - Server returns { replayed: true } when key matches existing transaction
 *     → "already processed" state shown instead of charging again
 *   - SlowProviderWarning appears at 3 000 ms and auto-hides when request completes
 *   - On success → redirect to /success/{orderId}
 *   - On failure → redirect to /failure/{orderId}
 *
 * Fork instructions:
 *   1. Replace DEFAULT_PAYMENT_METHODS with your provider's supported methods.
 *   2. Update DEFAULT_ORDER.orderId and amount from your order state / URL params.
 *   3. Implement real tokenisation in handleSubmit before sending paymentMethodToken.
 *   4. Add authentication headers in the fetch call if not handled by an interceptor.
 */
export default function CheckoutPage() {
  const [selectedMethod, setSelectedMethod] = React.useState<string>('card')
  const [replayedKey, setReplayedKey] = React.useState(false)
  const [formError, setFormError] = React.useState<string | null>(null)

  // Fork: read orderId from URL params or order state
  const orderId = 'ORDER-PLACEHOLDER'
  const amount = 10000   // 10 000 minor units (e.g. KRW 10 000 원)
  const currency = 'KRW'

  const mutation = useMutation({
    mutationFn: createPayment,
  })

  function handleSubmit(
    values: PaymentCheckoutFormValues,
    idempotencyKey: string,
    regenerate: () => void,
  ) {
    setFormError(null)
    setReplayedKey(false)

    mutation.mutate(
      {
        orderId,
        amount,
        currency,
        // Fork: replace with real tokenisation (e.g. Stripe.js createToken)
        paymentMethodToken: `tok_${selectedMethod}_${values.cardNumber.slice(-4)}`,
        idempotencyKey,
      },
      {
        onSuccess: (data) => {
          if (data.replayed) {
            // Idempotency replay: same key → same transaction, not a new charge
            setReplayedKey(true)
            return
          }
          // Regenerate key so next submit creates a fresh transaction
          regenerate()

          if (data.status === 'COMPLETED' || data.status === 'PENDING') {
            window.location.href = `/success/${data.orderId}`
          } else {
            window.location.href = `/failure/${data.orderId}`
          }
        },
        onError: (err: Error) => {
          setFormError(err.message ?? 'Payment failed. Please try again.')
        },
      }
    )
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-lg">
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight">Checkout</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Complete your payment securely.
        </p>
      </div>

      {/* Idempotency replay banner */}
      {replayedKey && (
        <div
          role="status"
          aria-live="polite"
          className="mb-4 rounded-md border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800 dark:border-blue-800/40 dark:bg-blue-900/20 dark:text-blue-300"
        >
          ✓ This payment has already been processed. No additional charge was made.
          <a href={`/methods`} className="ml-2 underline hover:no-underline">
            View payment history
          </a>
        </div>
      )}

      {/* Method selection */}
      <div className="mb-6 rounded-lg border bg-card p-4">
        <PaymentMethodPicker
          methods={DEFAULT_PAYMENT_METHODS}
          selected={selectedMethod}
          onSelect={setSelectedMethod}
        />
      </div>

      {/* Checkout form + idempotency key + slow provider warning */}
      <div className="rounded-lg border bg-card p-6 shadow-sm space-y-4">
        <IdempotencyKeyHandler>
          {(idempotencyKey, regenerate) => (
            <>
              {/* Slow provider warning — appears at 3 000 ms per PAYMENT-PROVIDER-007 */}
              <SlowProviderWarning
                isLoading={mutation.isPending}
                thresholdMs={SLOW_PROVIDER_THRESHOLD_MS}
              />

              <PaymentCheckoutForm
                amount={amount}
                currency={currency}
                isLoading={mutation.isPending}
                errorMessage={formError ?? undefined}
                onSubmit={(values) => handleSubmit(values, idempotencyKey, regenerate)}
              />
            </>
          )}
        </IdempotencyKeyHandler>
      </div>
    </div>
  )
}
