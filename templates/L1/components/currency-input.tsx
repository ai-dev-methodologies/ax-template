/*
---
template_id: L1/components/currency-input
layer: L1
provenance_class: locked_constraint
evidence:
  - source_type: upstream_id
    upstream_id: stripe-billing-2026-05
    section: "Amount encoding — minor units (integer)"
    quote: "All amounts on the Stripe API are in the smallest unit of the currency (e.g. cents for USD, yen for JPY)."
  - source_type: external
    citation: "ISO 4217 — KRW has 0 decimal places; integer won; ₩ prefix"
    url: "https://www.iso.org/iso-4217-currency-codes.html"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "MDN Intl.NumberFormat — currency formatting with minimumFractionDigits"
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat"
    quoted_at: "2026-05-18"
a11y_criteria:
  - "Input has aria-label including currency symbol"
  - "Formatted display value includes currency for screen readers"
dependencies: []
imports_from: []
imports_forbidden: [L2, L3, L4, app/, lib/auth/]
---
*/

'use client'

import * as React from 'react'

export interface CurrencyInputProps {
  /** Controlled value in integer minor currency units (KRW: won, USD: cents). */
  value: number
  /** Called with new value in integer minor units on every change. */
  onChange: (value: number) => void
  /** ISO 4217 currency code. Default: 'KRW' */
  currency?: string
  /** BCP 47 locale. Default: 'ko-KR' */
  locale?: string
  /** Minimum value in minor units. */
  min?: number
  /** Maximum value in minor units. */
  max?: number
  disabled?: boolean
  className?: string
  id?: string
}

/**
 * CurrencyInput — L1 controlled input for integer minor-unit currency amounts.
 *
 * Displays a formatted preview (e.g., "₩9,900") while the underlying value
 * is always stored as an integer minor unit. Never uses float internally.
 *
 * Rule: currency-amount-precision-explicit — all amounts are integer minor units.
 * Rule: no-billing-cross-import-from-payment — this component is billing-aware
 *   but lives in L1 (no L4 imports).
 *
 * Usage:
 * ```tsx
 * <CurrencyInput value={9900} onChange={setAmount} currency="KRW" />
 * // Displays ₩9,900; value stored as 9900 (integer won)
 * ```
 */
export function CurrencyInput({
  value,
  onChange,
  currency = 'KRW',
  locale = 'ko-KR',
  min,
  max,
  disabled,
  className,
  id,
}: CurrencyInputProps) {
  // Display: formatted currency string for the user
  const formatted = formatCurrencyAmount(value, currency, locale)

  // Raw numeric input for editing (no currency symbol)
  const [editing, setEditing] = React.useState(false)
  const [rawInput, setRawInput] = React.useState(String(value))

  const handleFocus = () => {
    setEditing(true)
    setRawInput(String(value))
  }

  const handleBlur = () => {
    setEditing(false)
    const parsed = parseInt(rawInput.replace(/[^0-9]/g, ''), 10)
    if (!isNaN(parsed)) {
      const clamped = clamp(parsed, min, max)
      onChange(clamped)
      setRawInput(String(clamped))
    } else {
      setRawInput(String(value))
    }
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setRawInput(e.target.value)
  }

  return (
    <div className={`relative ${className ?? ''}`}>
      <input
        id={id}
        type={editing ? 'number' : 'text'}
        value={editing ? rawInput : formatted}
        onChange={handleChange}
        onFocus={handleFocus}
        onBlur={handleBlur}
        disabled={disabled}
        aria-label={`${currency} amount: ${formatted}`}
        inputMode="numeric"
        className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm
                   text-right tabular-nums ring-offset-background
                   focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring
                   disabled:cursor-not-allowed disabled:opacity-50"
      />
    </div>
  )
}

/**
 * Formats an integer minor-unit amount as a currency string.
 * KRW: ₩9,900 (0 decimals). USD: $9.99 (2 decimals).
 */
export function formatCurrencyAmount(
  amount: number,
  currency = 'KRW',
  locale = 'ko-KR',
): string {
  const isZeroDecimal = currency === 'KRW' || currency === 'JPY'
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    minimumFractionDigits: isZeroDecimal ? 0 : 2,
    maximumFractionDigits: isZeroDecimal ? 0 : 2,
  }).format(amount)
}

function clamp(value: number, min?: number, max?: number): number {
  if (min !== undefined && value < min) return min
  if (max !== undefined && value > max) return max
  return value
}
