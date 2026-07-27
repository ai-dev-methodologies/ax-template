import { describe, it, expect } from 'vitest'
import * as React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'

// BACKLOG P3-57 (b) — templates/L1/components/currency-formatter.tsx documented its
// `amount` prop as MINOR units ("1234 = ₩1,234 for KRW") but handed it to Intl
// UNSCALED — so for any currency with fraction digits > 0 it silently behaved as
// MAJOR units instead (its own usage-example comment said `amount={1234} currency="USD"`
// → "$1,234.00", i.e. treating 1234 as 1,234 *dollars*, not 1,234 *cents*). KRW-only
// callers never caught it: KRW has 0 fraction digits, so minor unit == major unit and
// the two readings coincide (the same reason apps/pay/src/lib/money.ts's docstring
// gives for why an equivalent MAJOR-unit bug there survived KRW-only screens).
//
// No real caller in this repo depended on the old MAJOR-unit behavior: neither of the
// two blocks whose frontmatter lists `dependencies: [currency-formatter]`
// (templates/L2/blocks/invoice-list.tsx, templates/L2/blocks/pricing-table.tsx)
// actually imports CurrencyFormatter/formatKrw in code — both import
// `formatCurrencyAmount` from currency-input.tsx instead (grep verified: no
// `from '.../currency-formatter'` import exists anywhere in templates/ or frontend/
// outside this component's own file). The frontmatter `dependencies:` lines are
// stale/aspirational declarations, not enforced imports. So the fix (contract →
// MINOR units, matching the doc + the catalog's now-canonical integer-minor wire) has
// zero caller-impact.
//
// Fix mirrors currency-input.tsx's formatCurrencyAmount exactly: scale via the L0
// kit's fractionDigitsFor (ICU-derived exponent) and toMajorUnits (BigInt string
// conversion), and hand Intl.NumberFormat#format the exact decimal STRING — never
// `amount / 10 ** digits`, which loses the last minor unit at the safe-integer
// boundary (float division), and never `Number(exactString)` either (see
// pay-money-fraction-digits.vitest.ts's safe-integer-boundary suite for the contrast:
// apps/pay's money.ts still does that Number() conversion and is documented there as
// a separate, unfixed limitation).
import { CurrencyFormatter, formatKrw } from '../../templates/L1/components/currency-formatter'

function renderText(el: React.ReactElement): string {
  const html = renderToStaticMarkup(el)
  const match = html.match(/>([^<]*)<\/span>/)
  return match ? match[1] : html
}

describe('CurrencyFormatter — MINOR-unit contract (0/2/3-decimal currencies)', () => {
  it('KRW (0 decimals): 1234 minor units renders ₩1,234 — unchanged, minor === major at 0 exponent', () => {
    expect(renderText(React.createElement(CurrencyFormatter, { amount: 1234 }))).toBe('₩1,234')
  })

  it('USD (2 decimals): 1234 minor units (cents) renders $12.34 — the corrected contract (pre-fix: $1,234.00)', () => {
    const rendered = renderText(
      React.createElement(CurrencyFormatter, { amount: 1234, currency: 'USD', locale: 'en-US' }),
    )
    expect(rendered).toBe('$12.34')
    expect(rendered).not.toBe('$1,234.00')
  })

  it('BHD (3 decimals): 1234 minor units (fils) renders BHD 1.234, not BHD 12.34 or BHD 1,234.00', () => {
    const rendered = renderText(
      React.createElement(CurrencyFormatter, { amount: 1234, currency: 'BHD', locale: 'en-US' }),
    )
    expect(rendered).toContain('1.234')
    expect(rendered).not.toContain('12.34')
    expect(rendered).not.toContain('1,234.00')
  })
})

describe('formatKrw — regression: byte-identical to the pre-fix output', () => {
  it('1234 minor units (== major units at 0 exponent) still renders ₩1,234', () => {
    expect(formatKrw(1234)).toBe('₩1,234')
  })

  it('0 renders ₩0, not an empty/undefined string', () => {
    expect(formatKrw(0)).toBe('₩0')
  })
})

describe('CurrencyFormatter — safe-integer-boundary case for the scaled path (exact, unlike money.ts)', () => {
  it('USD at Number.MAX_SAFE_INTEGER keeps the last cent (toMajorUnits BigInt string, not Number() or / 10**digits)', () => {
    const rendered = renderText(
      React.createElement(CurrencyFormatter, {
        amount: Number.MAX_SAFE_INTEGER,
        currency: 'USD',
        locale: 'en-US',
      }),
    )
    expect(rendered).toContain('409.91')
    expect(rendered).not.toContain('409.90')
  })

  it('BHD at Number.MAX_SAFE_INTEGER keeps the last fils (3-decimal exponent, exact)', () => {
    const rendered = renderText(
      React.createElement(CurrencyFormatter, {
        amount: Number.MAX_SAFE_INTEGER,
        currency: 'BHD',
        locale: 'en-US',
      }),
    )
    expect(rendered).toContain('740.991')
    expect(rendered).not.toContain('740.990')
  })
})
