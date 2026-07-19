// fail_bare_tolocale/amount-label.tsx — isolates ONE detector: D1
// bare_toLocaleString. `.toLocaleString()` with no locale/options argument
// silently follows the runtime default locale. Deleting the D1 detector greens
// THIS fixture and no other (no date concat, no toFixed, no currency symbol).
export default function AmountLabel({ amount }: { amount: number }) {
  const display = amount.toLocaleString()
  return <span>{display}</span>
}
