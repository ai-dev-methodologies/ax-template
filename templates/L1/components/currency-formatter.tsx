/*
---
template_id: L1/components/currency-formatter
layer: L1
provenance_class: locked_constraint
evidence:
  - source_type: upstream_id
    upstream_id: next-intl-2026-05
    section: "KRW Currency Format"
    quote: "new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(1234) → '₩1,234'"
  - source_type: external
    citation: "ISO 4217 — KRW has 0 decimal places; ₩ symbol is the standard prefix"
    url: "https://www.iso.org/iso-4217-currency-codes.html"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "MDN Intl.NumberFormat — currency display and minimumFractionDigits"
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat"
    quoted_at: "2026-05-18"
  - source_type: upstream_id
    upstream_id: stripe-billing-2026-05
    section: "Zero-decimal currencies (amounts and currencies)"
    quote: "For the following zero-decimal currencies, the charge and the amount are the same, without requiring multiplication."
a11y_criteria:
  - "Rendered text includes currency symbol for screen readers"
dependencies: []
imports_from: [L0]
imports_forbidden: [L2, L3, L4, app/, lib/auth/]
---
*/

import { fractionDigitsFor, toMajorUnits } from 'templates/L0/fork-receiver-kit/money'

export interface CurrencyFormatterProps {
  /**
   * Monetary amount in the MINOR unit (the catalog's canonical wire encoding —
   * see templates/L0/fork-receiver-kit/money.ts and the payment/billing domains'
   * integer-minor-unit contract). E.g. 1234 = ₩1,234 for KRW (0 decimals),
   * 1234 = $12.34 for USD (2 decimals), 1234 = BHD 1.234 for BHD (3 decimals).
   */
  amount: number
  /** ISO 4217 currency code. Default: 'KRW' */
  currency?: string
  /** BCP 47 locale. Default: 'ko-KR' */
  locale?: string
  /** Optional CSS class */
  className?: string
}

/**
 * CurrencyFormatter — L1 primitive.
 *
 * Formats a MINOR-unit integer amount as a localised currency string using
 * Intl.NumberFormat. Locked constraint: KRW always renders as "₩1,234" (₩ prefix,
 * 0 decimals — minor unit == major unit for a 0-exponent currency, so this holds
 * under either a minor- or major-unit reading; that is exactly why an earlier
 * MAJOR-unit implementation of this component survived KRW-only callers).
 *
 * Usage:
 * ```tsx
 * <CurrencyFormatter amount={1234} />        // → ₩1,234
 * <CurrencyFormatter amount={1234} currency="USD" locale="en-US" /> // → $12.34
 * ```
 */
export function CurrencyFormatter({
  amount,
  currency = 'KRW',
  locale = 'ko-KR',
  className,
}: CurrencyFormatterProps) {
  const digits = fractionDigitsFor(currency)
  // `amount` is integer MINOR units. Scale via the L0 kit's BigInt string conversion,
  // NOT `amount / 10 ** digits` — binary float division loses the last minor unit at
  // the safe-integer boundary (see currency-input.tsx's formatCurrencyAmount, which
  // this mirrors). Intl.NumberFormat#format accepts a decimal string and parses it
  // exactly, so the digits survive even beyond Number.MAX_SAFE_INTEGER.
  const major = toMajorUnits(amount, digits)
  const formatted = new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  }).format(major)

  return (
    <span className={className} aria-label={`${formatted}`}>
      {formatted}
    </span>
  )
}

/**
 * Utility: format a MINOR-unit KRW amount without rendering a component.
 * Result: "₩1,234" — no decimals, ₩ prefix, comma grouping.
 */
export function formatKrw(amount: number): string {
  const digits = fractionDigitsFor('KRW') // ISO 4217: 0
  const major = toMajorUnits(amount, digits)
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(major)
}
