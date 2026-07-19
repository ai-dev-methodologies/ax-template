// fail_manual_format/receipt.tsx — deliberately violates
// locale-aware-number-date-format (practices-react/rules/).
// VIOLATION: raw .toLocaleString() (locale-blind — no locale/options
// argument, silently follows the runtime default locale) AND manual
// date-part string concatenation instead of Intl.DateTimeFormat.
export default function ReceiptPage({ order }: { order: { total: number; paidAt: string } }) {
  const paidAt = new Date(order.paidAt)

  // VIOLATION: bare toLocaleString() — no locale/options, not reproducible
  // across server + client, and ignores the caller's actual locale.
  const totalDisplay = order.total.toLocaleString()

  // VIOLATION: manual getMonth()/getDate()/getFullYear() concatenation —
  // hard-codes US month/day/year order; breaks for ko-KR (yyyy.MM.dd) and
  // any other locale with a different field order.
  const dateDisplay =
    (paidAt.getMonth() + 1) + '/' + paidAt.getDate() + '/' + paidAt.getFullYear()

  return (
    <div>
      <p>Total: {totalDisplay}</p>
      <p>Paid at: {dateDisplay}</p>
    </div>
  )
}
