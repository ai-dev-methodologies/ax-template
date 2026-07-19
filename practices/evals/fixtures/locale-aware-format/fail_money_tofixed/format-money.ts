// fail_money_tofixed/format-money.ts — isolates ONE detector: D3 money_toFixed.
// A .ts FORMATTER UTILITY (not a page component) — exactly the file kind the
// pre-broadening .tsx-only guard never scanned. `.toFixed()` on a money value
// emits a raw fixed-decimal string with no locale grouping/currency symbol.
// Deleting the D3 detector greens THIS fixture and no other.
export function formatTotal(total: number): string {
  return total.toFixed(2)
}
