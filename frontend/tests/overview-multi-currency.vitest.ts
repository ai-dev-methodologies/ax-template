import { describe, it, expect } from 'vitest'

// Multi-currency aggregation on the pay app's overview (codex R2 F2).
//
// Pre-fix defect (apps/pay/src/features/overview/components/overview-screen.tsx):
//   const currency = rows[0]?.currency ?? 'KRW'       // the FIRST row's currency
//   const grossTotal = rows.reduce((s, p) => s + (p.amount ?? 0), 0)   // ALL rows
// so a ledger holding ₩12,900 and $10.99 (wire 12900 + 1099) produced the number
// 13999 rendered in whichever currency happened to be first — a figure that is not
// a total of anything. The fix aggregates PER CURRENCY and renders one total per
// currency; no FX rate is invented (the app has no rate source).
//
// The aggregation is a pure module so it is testable without the app's `@/…` alias
// (frontend/vitest.config.ts maps `@/` to frontend/src, not to apps/pay/src). The
// screen holds NO money arithmetic of its own — it only formats what this function
// returns — so reverting the grouping here is the mutation lock for the screen.
import {
  summarizeByCurrency,
  type CurrencyRow,
} from '../apps/pay/src/features/overview/totals'
import { formatMinor } from '../apps/pay/src/lib/money'

// Wire rows: integer MINOR units, exactly as PaymentBodyMapper.toBody emits them
// (see tests/_fixtures/money-contract.golden.json — a $10.99 payment is 1099).
const MIXED_LEDGER: CurrencyRow[] = [
  {
    currency: 'KRW',
    amount: 12900,
    capturedAmount: 12900,
    balance: 12900,
    state: 'CAPTURED',
  },
  {
    currency: 'USD',
    amount: 1099,
    capturedAmount: 1099,
    balance: 1099,
    state: 'CAPTURED',
  },
]

describe('overview totals — per-currency aggregation', () => {
  it('never merges two currencies into one number', () => {
    const totals = summarizeByCurrency(MIXED_LEDGER)

    expect(totals).toHaveLength(2)
    expect(totals.map((t) => t.currency).sort()).toEqual(['KRW', 'USD'])
    expect(totals.map((t) => t.gross)).not.toContain(13999)
    expect(totals.find((t) => t.currency === 'KRW')?.gross).toBe(12900)
    expect(totals.find((t) => t.currency === 'USD')?.gross).toBe(1099)
  })

  it('renders one formatted total per currency — ₩12,900 and $10.99, never 13,999', () => {
    const rendered = summarizeByCurrency(MIXED_LEDGER).map((t) =>
      formatMinor(t.gross, t.currency),
    )

    expect(rendered).toHaveLength(2)
    expect(rendered).toContain('₩12,900')
    // ko-KR renders USD with the "US$" symbol; the value is what matters.
    expect(rendered.join(' ')).toContain('$10.99')
    // The pre-fix combined figure, in either currency's clothing.
    expect(rendered.join(' ')).not.toContain('13,999')
    expect(rendered.join(' ')).not.toContain('139.99')
  })

  it('captured / refunded are totalled inside each currency group only', () => {
    const totals = summarizeByCurrency([
      // KRW: 10000 captured, 3000 refunded -> balance 7000, PARTIAL_REFUNDED
      {
        currency: 'KRW',
        amount: 10000,
        capturedAmount: 10000,
        balance: 7000,
        state: 'PARTIAL_REFUNDED',
      },
      // USD: $10.99 fully refunded -> balance 0
      { currency: 'USD', amount: 1099, capturedAmount: 1099, balance: 0, state: 'REFUNDED' },
      { currency: 'USD', amount: 500, capturedAmount: 500, balance: 500, state: 'CAPTURED' },
    ])

    const krw = totals.find((t) => t.currency === 'KRW')!
    const usd = totals.find((t) => t.currency === 'USD')!

    expect(krw).toMatchObject({ gross: 10000, captured: 7000, refunded: 3000, count: 1 })
    expect(usd).toMatchObject({ gross: 1599, captured: 500, refunded: 1099, count: 2 })
    // A REFUNDED row contributes nothing to realized revenue in its own currency…
    expect(formatMinor(usd.captured, 'USD')).toContain('$5.00')
    // …and nothing at all to the other currency.
    expect(formatMinor(krw.captured, 'KRW')).toBe('₩7,000')
  })

  it('ordering is deterministic (most rows first, then currency code) — not page order', () => {
    const totals = summarizeByCurrency([
      { currency: 'USD', amount: 100, balance: 100, capturedAmount: 100, state: 'CAPTURED' },
      { currency: 'KRW', amount: 100, balance: 100, capturedAmount: 100, state: 'CAPTURED' },
      { currency: 'KRW', amount: 100, balance: 100, capturedAmount: 100, state: 'CAPTURED' },
    ])

    expect(totals.map((t) => t.currency)).toEqual(['KRW', 'USD'])
  })

  it('single-currency ledgers behave exactly as before (one group, no breakdown)', () => {
    const totals = summarizeByCurrency([
      { currency: 'KRW', amount: 12900, capturedAmount: 12900, balance: 12900, state: 'CAPTURED' },
      { currency: 'KRW', amount: 5000, capturedAmount: 5000, balance: 5000, state: 'CAPTURED' },
    ])

    expect(totals).toHaveLength(1)
    expect(totals[0]).toMatchObject({ currency: 'KRW', gross: 17900, captured: 17900, count: 2 })
  })

  it('an empty ledger yields no groups (the screen falls back to a zeroed KRW tile)', () => {
    expect(summarizeByCurrency([])).toEqual([])
  })
})
