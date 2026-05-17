/*
---
template_id: L4/payment/app/(payment)/methods/new/page
layer: L4
domain: payment
domain_mode: full_trio
backend_operation_id: createPayment
evidence:
  - source_type: internal
    rationale: "L4 payment vertical — ADD METHOD page. Uses L2 PaymentMethodPicker in form mode to select a payment method type, then redirects to /checkout with the selected method pre-selected."
  - source_type: external
    citation: "Next.js 15 App Router — useRouter for programmatic navigation"
    url: "https://nextjs.org/docs/app/building-your-application/routing/programmatic-navigation"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices]
---
*/
'use client'

import * as React from 'react'
import PaymentMethodPicker, {
  type PaymentMethod,
} from 'templates/L2/blocks/payment-method-picker'

// ─── constants ───────────────────────────────────────────────────────────────

const AVAILABLE_METHODS: PaymentMethod[] = [
  { id: 'card', label: 'Credit / Debit Card', description: 'Visa, Mastercard, Amex' },
  { id: 'kakao_pay', label: 'Kakao Pay', description: 'Kakao Pay 간편결제' },
  { id: 'naver_pay', label: 'Naver Pay', description: 'Naver Pay 간편결제' },
  { id: 'bank_transfer', label: 'Bank Transfer', description: 'Online bank transfer (realtime)' },
]

// ─── component ──────────────────────────────────────────────────────────────

/**
 * AddPaymentMethodPage — L4 payment "add method" page.
 *
 * Uses L2 PaymentMethodPicker to let the user select a payment method type,
 * then proceeds to /checkout with the selected method pre-selected.
 *
 * Fork instructions:
 *   1. Replace AVAILABLE_METHODS with the methods your payment provider supports.
 *   2. Pass the selected method ID via URL param to /checkout (e.g. /checkout?method=kakao_pay).
 *   3. If saving payment methods server-side, add a mutation here before redirecting.
 */
export default function AddPaymentMethodPage() {
  const [selected, setSelected] = React.useState<string>('card')

  function handleContinue() {
    // Navigate to checkout with the selected method pre-chosen
    window.location.href = `/checkout?method=${encodeURIComponent(selected)}`
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-lg">
      <div className="mb-6">
        <a
          href="/methods"
          className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground transition-colors"
        >
          ← Back to payment history
        </a>
      </div>

      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight">Select payment method</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Choose how you would like to pay.
        </p>
      </div>

      <div className="rounded-lg border bg-card p-4 shadow-sm">
        <PaymentMethodPicker
          methods={AVAILABLE_METHODS}
          selected={selected}
          onSelect={setSelected}
        />
      </div>

      <div className="mt-6">
        <button
          type="button"
          onClick={handleContinue}
          className="inline-flex w-full items-center justify-center rounded-md bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground shadow hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring transition-colors"
        >
          Continue to checkout
        </button>
      </div>
    </div>
  )
}
