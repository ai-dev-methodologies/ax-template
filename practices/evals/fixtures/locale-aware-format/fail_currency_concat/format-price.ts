// fail_currency_concat/format-price.ts — isolates ONE detector: D4
// currency_symbol_concat. A .ts FORMATTER UTILITY. String-concatenating a
// currency symbol hard-codes the symbol and its position instead of using
// Intl.NumberFormat currency style. Deleting the D4 detector greens THIS
// fixture and no other.
export function formatPrice(amount: number): string {
  return '$' + amount
}
