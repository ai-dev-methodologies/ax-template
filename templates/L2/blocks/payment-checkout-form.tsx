/*
---
template_id: L2/blocks/payment-checkout-form
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "PCI DSS v4.0 — cardholder data input never stored in raw form client-side"
    url: "https://www.pcisecuritystandards.org/document_library/"
  - source_type: internal
    rationale: "L2 payment block per PRD §4.11 — accepts onSubmit prop; no concrete payment provider SDK import."
dependencies: [button, input, label]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/payment/]
---
*/
import * as React from 'react'

export interface PaymentCheckoutFormValues {
  cardNumber: string
  expiry: string
  cvc: string
  cardholderName: string
}

export interface PaymentCheckoutFormProps {
  /** Amount in minor currency units (e.g. cents) */
  amount: number
  /** ISO 4217 currency code */
  currency: string
  /** Called with raw form values — L4 tokenises before sending to provider */
  onSubmit: (values: PaymentCheckoutFormValues) => void
  isLoading?: boolean
  errorMessage?: string
  /** Slot for payment provider logo / trust signals */
  trustSlot?: React.ReactNode
}

/** Format amount for display (e.g. 1099 USD → "$10.99") */
function formatAmount(amount: number, currency: string): string {
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency,
      minimumFractionDigits: 2,
    }).format(amount / 100)
  } catch {
    return `${currency} ${(amount / 100).toFixed(2)}`
  }
}

export default function PaymentCheckoutForm({
  amount,
  currency,
  onSubmit,
  isLoading = false,
  errorMessage,
  trustSlot,
}: PaymentCheckoutFormProps) {
  const [values, setValues] = React.useState<PaymentCheckoutFormValues>({
    cardNumber: '',
    expiry: '',
    cvc: '',
    cardholderName: '',
  })

  function set<K extends keyof PaymentCheckoutFormValues>(key: K, value: string) {
    setValues(prev => ({ ...prev, [key]: value }))
  }

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    onSubmit(values)
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-4 w-full">
      <div className="rounded-md border border-border bg-muted/30 p-3 text-center">
        <p className="text-sm text-muted-foreground">Amount due</p>
        <p className="text-2xl font-bold tabular-nums">
          {formatAmount(amount, currency)}
        </p>
      </div>

      <div className="space-y-2">
        <label htmlFor="pcf-name" className="text-sm font-medium leading-none">
          Cardholder name
        </label>
        <input
          id="pcf-name"
          type="text"
          autoComplete="cc-name"
          required
          disabled={isLoading}
          value={values.cardholderName}
          onChange={e => set('cardholderName', e.target.value)}
          placeholder="Full name on card"
          className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
        />
      </div>

      <div className="space-y-2">
        <label htmlFor="pcf-card" className="text-sm font-medium leading-none">
          Card number
        </label>
        <input
          id="pcf-card"
          type="text"
          autoComplete="cc-number"
          inputMode="numeric"
          required
          disabled={isLoading}
          value={values.cardNumber}
          onChange={e => set('cardNumber', e.target.value)}
          placeholder="1234 5678 9012 3456"
          maxLength={19}
          className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm font-mono focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <label htmlFor="pcf-expiry" className="text-sm font-medium leading-none">
            Expiry
          </label>
          <input
            id="pcf-expiry"
            type="text"
            autoComplete="cc-exp"
            inputMode="numeric"
            required
            disabled={isLoading}
            value={values.expiry}
            onChange={e => set('expiry', e.target.value)}
            placeholder="MM / YY"
            maxLength={7}
            className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm font-mono focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
          />
        </div>

        <div className="space-y-2">
          <label htmlFor="pcf-cvc" className="text-sm font-medium leading-none">
            CVC
          </label>
          <input
            id="pcf-cvc"
            type="text"
            autoComplete="cc-csc"
            inputMode="numeric"
            required
            disabled={isLoading}
            value={values.cvc}
            onChange={e => set('cvc', e.target.value)}
            placeholder="123"
            maxLength={4}
            className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm font-mono focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
          />
        </div>
      </div>

      {errorMessage && (
        <p role="alert" className="text-sm text-destructive">
          {errorMessage}
        </p>
      )}

      {trustSlot && <div className="flex justify-center">{trustSlot}</div>}

      <button
        type="submit"
        disabled={isLoading}
        className="inline-flex w-full items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
      >
        {isLoading ? 'Processing…' : `Pay ${formatAmount(amount, currency)}`}
      </button>
    </form>
  )
}
