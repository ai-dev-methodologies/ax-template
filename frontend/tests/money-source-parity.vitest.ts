import { readFileSync } from 'fs'
import { join } from 'path'
import { describe, it, expect } from 'vitest'

// BACKLOG P3-65 — `fractionDigitsFor`'s ISO-4217 special-cases fallback tables
// (ZERO_DECIMAL_CURRENCIES / THREE_DECIMAL_CURRENCIES) exist as TWO independently
// maintained copies: templates/L0/fork-receiver-kit/money.ts (exported, imported by
// templates/L1/components/currency-input.tsx) and frontend/apps/pay/src/lib/money.ts
// (apps/pay cannot import the L0 kit at runtime — Next.js rejects module resolution
// outside the project root unless `experimental.externalDir` is set, which apps/pay's
// next.config.ts does not set; see pay-money-fraction-digits.vitest.ts's own note on
// this boundary). Promoting `fractionDigitsFor` to a shared `@ax/core` package is a
// registered follow-up (out of scope here — it would require wiring a new workspace
// package, not just editing these two files).
//
// Until that promotion lands, the two tables can silently drift — a hand-edit to one
// copy's Set (e.g. someone adding a currency to only one file) would NOT be caught by
// either file's own tests, because Node's ICU already resolves fraction digits for
// every real ISO 4217 code via Intl directly; the local Sets are a FALLBACK path that
// normal test execution never exercises. Testing through the public API (formatMinor/
// fractionDigitsFor) therefore cannot detect drift — this is a SOURCE-parity check by
// design: it reads both files' text and asserts their fallback tables are byte-for-byte
// the same currency SET (order-independent), which is the "최소한" (at minimum) floor
// the two copies must hold until they are unified.

function extractCurrencySet(source: string, constName: string): Set<string> {
  const pattern = new RegExp(`const ${constName}: ReadonlySet<string> = new Set\\(\\[([^\\]]*)\\]\\)`)
  const match = pattern.exec(source)
  if (!match) {
    throw new Error(`money-source-parity: could not find ${constName} in source — did the declaration shape change?`)
  }
  const codes = match[1]
    .split(',')
    .map((entry) => entry.trim().replace(/^['"]|['"]$/g, ''))
    .filter((entry) => entry.length > 0)
  return new Set(codes)
}

function readMoneySource(relativePath: string): string {
  return readFileSync(join(process.cwd(), relativePath), 'utf8')
}

describe('fractionDigitsFor fallback tables — source parity between the two independently maintained copies (P3-65)', () => {
  const l0Source = readMoneySource('../templates/L0/fork-receiver-kit/money.ts')
  const paySource = readMoneySource('apps/pay/src/lib/money.ts')

  it('ZERO_DECIMAL_CURRENCIES matches exactly between templates/L0 and apps/pay', () => {
    const l0Set = extractCurrencySet(l0Source, 'ZERO_DECIMAL_CURRENCIES')
    const paySet = extractCurrencySet(paySource, 'ZERO_DECIMAL_CURRENCIES')
    expect([...paySet].sort()).toEqual([...l0Set].sort())
  })

  it('THREE_DECIMAL_CURRENCIES (BHD/IQD/JOD/KWD/LYD/OMR/TND) matches exactly between templates/L0 and apps/pay', () => {
    const l0Set = extractCurrencySet(l0Source, 'THREE_DECIMAL_CURRENCIES')
    const paySet = extractCurrencySet(paySource, 'THREE_DECIMAL_CURRENCIES')
    expect([...paySet].sort()).toEqual([...l0Set].sort())
  })

  it('neither table is accidentally empty (a parity check against two empty sets would vacuously pass)', () => {
    const l0Zero = extractCurrencySet(l0Source, 'ZERO_DECIMAL_CURRENCIES')
    const l0Three = extractCurrencySet(l0Source, 'THREE_DECIMAL_CURRENCIES')
    expect(l0Zero.size).toBeGreaterThan(0)
    expect(l0Three.size).toBeGreaterThan(0)
    expect(l0Three.has('BHD')).toBe(true)
  })
})
