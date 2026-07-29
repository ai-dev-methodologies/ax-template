import { readFileSync } from 'fs'
import { join } from 'path'
import { describe, it, expect } from 'vitest'

// BACKLOG P3-65 / P3-72 — `fractionDigitsFor`'s ISO-4217 special-cases fallback
// tables (ZERO_DECIMAL_CURRENCIES / THREE_DECIMAL_CURRENCIES) used to exist as
// TWO independently maintained copies: templates/L0/fork-receiver-kit/money.ts
// (exported, imported by templates/L1/components/currency-input.tsx and every
// L2/L4 money-handling template) and frontend/apps/pay/src/lib/money.ts.
//
// P3-72 promoted `fractionDigitsFor` to `frontend/packages/core/src/money.ts`
// (`@ax/core`) — apps/pay no longer carries its own copy, it imports the
// function (see the import-boundary check below). The L0 kit copy is NOT
// collapsed into `@ax/core`: every L0 file's frontmatter declares
// `imports_from: []` and the kit's README states it "survives the fork
// verbatim" — fork-receivers copy the L0 directory wholesale into their own
// codebase, which has no `@ax/core` package. So two canonical copies remain
// by design: templates/L0 (self-contained, for forks) and @ax/core (for
// in-monorepo apps). This test guards THOSE two against drift, and separately
// asserts apps/pay imports rather than re-declares the tables.
//
// Testing through the public API (formatMinor/fractionDigitsFor) cannot catch
// this drift — Node's ICU already resolves fraction digits for every real ISO
// 4217 code via Intl directly, so the local Sets are a FALLBACK path normal
// test execution never exercises. Hence a SOURCE-parity check: read both
// files' text and assert their fallback tables are byte-for-byte the same
// currency SET (order-independent).

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

describe('fractionDigitsFor fallback tables — source parity between the two remaining canonical copies (P3-65/P3-72)', () => {
  const l0Source = readMoneySource('../templates/L0/fork-receiver-kit/money.ts')
  const coreSource = readMoneySource('packages/core/src/money.ts')

  it('ZERO_DECIMAL_CURRENCIES matches exactly between templates/L0 and @ax/core', () => {
    const l0Set = extractCurrencySet(l0Source, 'ZERO_DECIMAL_CURRENCIES')
    const coreSet = extractCurrencySet(coreSource, 'ZERO_DECIMAL_CURRENCIES')
    expect([...coreSet].sort()).toEqual([...l0Set].sort())
  })

  it('THREE_DECIMAL_CURRENCIES (BHD/IQD/JOD/KWD/LYD/OMR/TND) matches exactly between templates/L0 and @ax/core', () => {
    const l0Set = extractCurrencySet(l0Source, 'THREE_DECIMAL_CURRENCIES')
    const coreSet = extractCurrencySet(coreSource, 'THREE_DECIMAL_CURRENCIES')
    expect([...coreSet].sort()).toEqual([...l0Set].sort())
  })

  it('neither table is accidentally empty (a parity check against two empty sets would vacuously pass)', () => {
    const l0Zero = extractCurrencySet(l0Source, 'ZERO_DECIMAL_CURRENCIES')
    const l0Three = extractCurrencySet(l0Source, 'THREE_DECIMAL_CURRENCIES')
    expect(l0Zero.size).toBeGreaterThan(0)
    expect(l0Three.size).toBeGreaterThan(0)
    expect(l0Three.has('BHD')).toBe(true)
  })
})

describe('apps/pay imports fractionDigitsFor from @ax/core instead of re-declaring it (P3-72 import test)', () => {
  const paySource = readMoneySource('apps/pay/src/lib/money.ts')

  it('imports fractionDigitsFor from @ax/core', () => {
    expect(/import\s*\{[^}]*\bfractionDigitsFor\b[^}]*\}\s*from\s*['"]@ax\/core['"]/.test(paySource)).toBe(true)
  })

  it('no longer declares its own ZERO_DECIMAL_CURRENCIES / THREE_DECIMAL_CURRENCIES tables', () => {
    expect(paySource.includes('ZERO_DECIMAL_CURRENCIES')).toBe(false)
    expect(paySource.includes('THREE_DECIMAL_CURRENCIES')).toBe(false)
  })
})
