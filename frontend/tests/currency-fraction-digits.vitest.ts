import { describe, it, expect } from 'vitest'

// ISO 4217 minor-unit exponents in the GENERIC money components (codex R2 F3).
//
// Pre-fix defects, both on the money surface:
//   1. templates/L0/fork-receiver-kit/money.ts `fractionDigitsFor` knew only two widths —
//      0 for a hand-listed zero-decimal set, 2 for everything else. Every 3-decimal dinar
//      currency (BHD/KWD/OMR/JOD/TND/IQD/LYD) was therefore off by a factor of 10.
//   2. templates/L1/components/currency-input.tsx `formatCurrencyAmount` divided every
//      non-KRW/JPY currency by the LITERAL 100, so 1234 BHD minor units rendered as
//      "BHD 12.34" instead of "BHD 1.234" — a 10x misstatement of a price.
//
// The payment OpenAPI contract happens to restrict its own currency enum to KRW/USD, but these
// are catalog-generic components a fork-receiver may hand any ISO code, so the defect is real
// for them. KRW/USD behaviour is asserted here too — the fix must be byte-identical for the
// currencies already in use.
import {
  fractionDigitsFor,
  toMinorUnits,
  toMajorUnits,
} from '../../templates/L0/fork-receiver-kit/money'
import { formatCurrencyAmount } from '../../templates/L1/components/currency-input'

describe('fractionDigitsFor — real ISO 4217 exponents', () => {
  it('0-decimal currencies resolve to 0', () => {
    expect(fractionDigitsFor('KRW')).toBe(0)
    expect(fractionDigitsFor('JPY')).toBe(0)
    // Not in the KRW/JPY special case the L1 component used to hard-code:
    expect(fractionDigitsFor('VND')).toBe(0)
    expect(fractionDigitsFor('ISK')).toBe(0)
  })

  it('2-decimal currencies resolve to 2 (the ISO default, unchanged)', () => {
    expect(fractionDigitsFor('USD')).toBe(2)
    expect(fractionDigitsFor('EUR')).toBe(2)
    expect(fractionDigitsFor('GBP')).toBe(2)
  })

  it('3-decimal dinar currencies resolve to 3 (the defect this closes)', () => {
    expect(fractionDigitsFor('BHD')).toBe(3)
    expect(fractionDigitsFor('KWD')).toBe(3)
    expect(fractionDigitsFor('OMR')).toBe(3)
    expect(fractionDigitsFor('JOD')).toBe(3)
    expect(fractionDigitsFor('TND')).toBe(3)
  })

  it('input is normalised (whitespace / case) and an unknown code falls back to the ISO default 2', () => {
    expect(fractionDigitsFor(' krw ')).toBe(0)
    expect(fractionDigitsFor('bhd')).toBe(3)
    expect(fractionDigitsFor('ZZZ')).toBe(2)
    // A malformed code must not throw — the helper is called from render paths.
    expect(fractionDigitsFor('')).toBe(2)
  })
})

describe('minor <-> major round-trip at the currency exponent', () => {
  it('BHD: 1234 minor units is 1.234, not 12.34', () => {
    const digits = fractionDigitsFor('BHD')
    expect(toMajorUnits(1234n, digits)).toBe('1.234')
    expect(toMinorUnits('1.234', digits)).toBe(1234n)
  })

  it('KRW/USD regression: unchanged by the fix', () => {
    expect(toMajorUnits(12900n, fractionDigitsFor('KRW'))).toBe('12900')
    expect(toMinorUnits('12900', fractionDigitsFor('KRW'))).toBe(12900n)
    expect(toMajorUnits(1099n, fractionDigitsFor('USD'))).toBe('10.99')
    expect(toMinorUnits('10.99', fractionDigitsFor('USD'))).toBe(1099n)
  })
})

describe('formatCurrencyAmount (L1 currency-input) — scales by 10 ** exponent, never a literal 100', () => {
  it('BHD: 1234 minor units renders 1.234 (pre-fix: 12.34)', () => {
    const rendered = formatCurrencyAmount(1234, 'BHD', 'en-US')
    expect(rendered).toContain('1.234')
    expect(rendered).not.toContain('12.34')
  })

  it('KWD: 1500 minor units renders 1.500', () => {
    expect(formatCurrencyAmount(1500, 'KWD', 'en-US')).toContain('1.500')
  })

  it('KRW regression: 9900 minor units still renders ₩9,900', () => {
    expect(formatCurrencyAmount(9900, 'KRW', 'ko-KR')).toBe('₩9,900')
  })

  it('USD regression: 999 minor units still renders $9.99', () => {
    expect(formatCurrencyAmount(999, 'USD', 'en-US')).toBe('$9.99')
  })

  it('VND: a zero-decimal currency the old KRW/JPY special case missed', () => {
    // Pre-fix this divided by 100 and rendered 125.00; the amount IS 12500 đồng.
    expect(formatCurrencyAmount(12500, 'VND', 'en-US')).toContain('12,500')
  })
  it('safe-integer boundary: MAX_SAFE_INTEGER BHD keeps its last fils (no float division)', () => {
    // `amount / 10 ** digits` loses the final minor unit at the documented safe-integer
    // boundary (money.ts). Scaling through toMajorUnits' BigInt string keeps it exact.
    const rendered = formatCurrencyAmount(Number.MAX_SAFE_INTEGER, 'BHD', 'en-US')
    expect(rendered).toContain('740.991')
    expect(rendered).not.toContain('740.990')
  })

  it('safe-integer boundary: MAX_SAFE_INTEGER USD keeps its last cent', () => {
    const rendered = formatCurrencyAmount(Number.MAX_SAFE_INTEGER, 'USD', 'en-US')
    expect(rendered).toContain('409.91')
  })
})
