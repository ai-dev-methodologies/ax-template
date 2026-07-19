// pass_intl_format/receipt.tsx — the correct rewrite of
// fail_manual_format/receipt.tsx, satisfying
// locale-aware-number-date-format (practices-react/rules/).
// Both amounts and dates are formatted via the Intl API, so the display
// order and grouping/symbol follow the caller's actual locale (ECMA-402 —
// see rule evidence) instead of a hard-coded US-shaped string.
export default function ReceiptPage({
  order,
  locale,
}: {
  order: { total: number; paidAt: string; currency: string }
  locale: string
}) {
  const paidAt = new Date(order.paidAt)

  const totalDisplay = new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: order.currency,
  }).format(order.total)

  const dateDisplay = new Intl.DateTimeFormat(locale, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(paidAt)

  return (
    <div>
      <p>Total: {totalDisplay}</p>
      <p>Paid at: {dateDisplay}</p>
    </div>
  )
}
