// fail_manual_format/receipt.tsx — isolates ONE detector: D2 manual_date_concat
// (multiline). getFullYear()/getMonth()/getDate() joined with '+' across lines —
// hard-codes a field order that cannot switch per locale, and the MULTILINE
// shape is exactly what the pre-broadening single-line grep missed.
// Deleting the D2 detector greens THIS fixture and no other.
export default function ReceiptPage({ order }: { order: { paidAt: string } }) {
  const paidAt = new Date(order.paidAt)

  const dateDisplay =
    paidAt.getFullYear() +
    '.' +
    (paidAt.getMonth() + 1) +
    '.' +
    paidAt.getDate()

  return <p>Paid at: {dateDisplay}</p>
}
