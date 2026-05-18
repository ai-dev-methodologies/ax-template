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
a11y_criteria:
  - "Rendered text includes currency symbol for screen readers"
dependencies: []
imports_from: []
imports_forbidden: [L2, L3, L4, app/, lib/auth/]
---
*/

export interface CurrencyFormatterProps {
  /** Monetary amount in the minor unit (e.g., 1234 = ₩1,234 for KRW) */
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
 * Formats a numeric amount as a localised currency string using Intl.NumberFormat.
 * Locked constraint: KRW always renders as "₩1,234" (₩ prefix, 0 decimals).
 *
 * Usage:
 * ```tsx
 * <CurrencyFormatter amount={1234} />        // → ₩1,234
 * <CurrencyFormatter amount={1234} currency="USD" locale="en-US" /> // → $1,234.00
 * ```
 */
export function CurrencyFormatter({
  amount,
  currency = 'KRW',
  locale = 'ko-KR',
  className,
}: CurrencyFormatterProps) {
  const formatted = new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    // KRW: ISO 4217 mandates 0 decimal places
    minimumFractionDigits: currency === 'KRW' ? 0 : undefined,
    maximumFractionDigits: currency === 'KRW' ? 0 : undefined,
  }).format(amount)

  return (
    <span className={className} aria-label={`${formatted}`}>
      {formatted}
    </span>
  )
}

/**
 * Utility: format KRW amount without rendering a component.
 * Result: "₩1,234" — no decimals, ₩ prefix, comma grouping.
 */
export function formatKrw(amount: number): string {
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount)
}
