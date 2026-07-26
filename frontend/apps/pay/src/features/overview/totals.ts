/**
 * Per-currency aggregation for the overview tiles.
 *
 * A ledger can hold rows in more than one currency. Summing them into one number
 * is not an approximation — it is meaningless: ₩12,900 + $10.99 has no value
 * without an FX rate, and rendering the arithmetic sum (13,999) under whichever
 * currency happened to come first in the page is a wrong number shown as fact.
 * The overview screen therefore aggregates PER CURRENCY and renders one total per
 * currency. No FX conversion is invented here; the app has no rate source.
 *
 * Amounts are the raw wire values: integer MINOR units (PaymentBodyMapper.toBody
 * emits `Money.toMinorUnits` — see frontend/tests/_fixtures/money-contract.golden.json,
 * where a $10.99 payment is `amount: 1099`). Summing minor units keeps the
 * arithmetic in integers; formatting happens at the edge (lib/money.formatMinor).
 */

/** The subset of the payment wire row this aggregation needs. */
export interface CurrencyRow {
  currency: string;
  amount?: number | null;
  capturedAmount?: number | null;
  balance?: number | null;
  state: string;
}

export interface CurrencyTotals {
  currency: string;
  /** Σ amount — everything that was charged, in minor units. */
  gross: number;
  /** Σ balance of CAPTURED / PARTIAL_REFUNDED rows — realized revenue. */
  captured: number;
  /** Σ (capturedAmount - balance) of refunded rows. */
  refunded: number;
  /** Number of ledger rows in this currency. */
  count: number;
}

const num = (v: number | null | undefined): number => (typeof v === 'number' ? v : 0);

/**
 * Group the ledger rows by currency and total each group independently.
 *
 * Ordering is deterministic and independent of page order: most rows first, ties
 * broken by currency code. The first entry is the screen's "primary" currency —
 * a presentation choice, never a merge of the others.
 */
export function summarizeByCurrency(rows: readonly CurrencyRow[]): CurrencyTotals[] {
  const byCurrency = new Map<string, CurrencyTotals>();

  for (const row of rows) {
    const currency = row.currency || 'KRW';
    const totals: CurrencyTotals = byCurrency.get(currency) ?? {
      currency,
      gross: 0,
      captured: 0,
      refunded: 0,
      count: 0,
    };

    const next: CurrencyTotals = {
      currency,
      gross: totals.gross + num(row.amount),
      captured:
        totals.captured +
        (row.state === 'CAPTURED' || row.state === 'PARTIAL_REFUNDED' ? num(row.balance) : 0),
      refunded:
        totals.refunded +
        (row.state === 'REFUNDED' || row.state === 'PARTIAL_REFUNDED'
          ? num(row.capturedAmount) - num(row.balance)
          : 0),
      count: totals.count + 1,
    };
    byCurrency.set(currency, next);
  }

  return [...byCurrency.values()].sort(
    (a, b) => b.count - a.count || a.currency.localeCompare(b.currency),
  );
}
