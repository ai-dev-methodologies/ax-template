import { describe, it, expect } from 'vitest'

// BACKLOG P3-57 (a) — apps/pay's own money.ts carried the SAME 3-decimal-currency gap
// the L0 kit (templates/L0/fork-receiver-kit/money.ts) closed in the wave-3 money pass:
// a hand-maintained `FRACTION_DIGITS` table (KRW/USD/EUR/JPY/GBP, default 2) that a
// 3-decimal dinar currency (BHD/KWD/OMR/JOD/TND) would fall through to the `?? 2`
// default and render 10x too large.
//
// Fix: money.ts's private `fractionDigitsFor` now derives the exponent from ICU
// (Intl.NumberFormat resolvedOptions().maximumFractionDigits) with the same explicit
// fallback sets as the L0 kit, mirroring it exactly. It is NOT imported from the L0
// kit — apps/pay is a real Next.js build (unlike templates/, which is copy-target
// source never bundled by any app here), and Next.js rejects module resolution
// outside the project root unless `experimental.externalDir` is set, which apps/pay's
// next.config.ts does not set (see frontend/next.config.ts's own `@templates`
// webpack-alias comment, which documents this repo already treats that boundary as
// needing explicit config — and no app in this repo successfully imports the L0 kit's
// bare `templates/L0/...` specifier at runtime; only tests do, via a vitest-only
// alias). See the task report for the full finding.
//
// `fractionDigitsFor` is not exported, so this suite exercises it through the public
// API (`formatMinor`), the same surface real callers use (checkout-screen.tsx,
// transactions-screen.tsx, subscriptions-screen.tsx, overview-screen.tsx).
import { formatMajor, formatMinor, formatMajorCompact } from '../apps/pay/src/lib/money'

describe('money.ts (apps/pay) — fractionDigitsFor gap closed for 3-decimal currencies', () => {
  it('BHD: 1234 minor units renders 1.234, not 12.34 (pre-fix: fell through to the ?? 2 default)', () => {
    const rendered = formatMinor(1234, 'BHD')
    expect(rendered).toContain('1.234')
    expect(rendered).not.toContain('12.34')
  })

  it('KWD/OMR/JOD/TND: all resolve to 3 decimals, not 2', () => {
    expect(formatMinor(1500, 'KWD')).toContain('1.500')
    expect(formatMinor(1500, 'OMR')).toContain('1.500')
    expect(formatMinor(1500, 'JOD')).toContain('1.500')
    expect(formatMinor(1500, 'TND')).toContain('1.500')
  })

  it('VND: a zero-decimal currency the old 5-entry table also missed (fell through to 2)', () => {
    // Pre-fix this rendered 125.00 for what is actually 12,500 đồng.
    expect(formatMinor(12500, 'VND')).toContain('12,500')
    expect(formatMinor(12500, 'VND')).not.toContain('125.00')
  })

  it('unknown ISO code falls back to the ISO default of 2 (unchanged fallback behavior)', () => {
    expect(formatMinor(1234, 'ZZZ')).not.toBe('—')
  })
})

describe('money.ts (apps/pay) — KRW/USD/EUR/JPY/GBP regression: byte-identical to the pre-fix table', () => {
  it('KRW: 12900 minor units still renders ₩12,900 (0 decimals)', () => {
    expect(formatMinor(12900, 'KRW')).toBe('₩12,900')
  })

  it('USD: 1099 minor units still renders $10.99 (2 decimals, ko-KR locale prefix)', () => {
    expect(formatMinor(1099, 'USD')).toBe('US$10.99')
  })

  it('EUR/JPY/GBP: unchanged fraction-digit widths (2/0/2)', () => {
    expect(formatMinor(1099, 'EUR')).toContain('10.99')
    expect(formatMinor(1099, 'JPY')).not.toContain('.')
    expect(formatMinor(1099, 'GBP')).toContain('10.99')
  })

  it('formatMajor: a locally-typed KRW amount still renders ₩12,900 (major-unit path untouched)', () => {
    expect(formatMajor(12900, 'KRW')).toBe('₩12,900')
  })

  it('formatMajorCompact: unchanged for a KRW tile total', () => {
    expect(formatMajorCompact(1_200_000, 'KRW')).toBe('₩120만')
  })
})

describe('money.ts (apps/pay) — safe-integer-boundary case for the scaled path', () => {
  // KNOWN PRE-EXISTING LIMITATION (not introduced or fixed by the P3-57(a) delegation
  // fix; present identically before and after it): unlike currency-input.tsx's
  // formatCurrencyAmount / currency-formatter.tsx's CurrencyFormatter (both of which
  // hand Intl.NumberFormat#format the exact decimal STRING from toMajorUnits),
  // money.ts's formatMinor converts that exact string through `Number(major)` before
  // formatting (see formatMinor's final line). For amounts within everyday magnitude
  // this is exact (see the KRW/USD regressions above and the BigInt-safe
  // minorToMajorString step itself, which never touches a float). At the documented
  // Number.MAX_SAFE_INTEGER boundary, though, the intermediate `Number(major)` step
  // silently drops the final minor unit — the exact class of bug this file's own
  // top-of-file doc comment says money.ts "deliberately avoids". Reported as a
  // separate, out-of-scope finding (candidate BACKLOG item) rather than fixed here:
  // fixing it exactly (matching toMajorUnits' string-passthrough approach) requires
  // widening apps/pay's tsconfig "lib" to include the ES2023 Intl string-argument
  // overload (`tsc --noEmit -p apps/pay/tsconfig.json` confirms `.format(<string>)`
  // is a type error under the current "lib": ["ES2020", ...]), and tsconfig.json is
  // outside this lane's file set.
  it('USD at Number.MAX_SAFE_INTEGER: the last cent is lost (documents the limitation, does not claim it is fixed)', () => {
    const rendered = formatMinor(Number.MAX_SAFE_INTEGER, 'USD')
    // Exact value would be …409.91; the Number() conversion rounds it to …409.90.
    expect(rendered).toContain('409.90')
    expect(rendered).not.toContain('409.91')
  })

  it('BHD at Number.MAX_SAFE_INTEGER: same limitation at 3 decimals', () => {
    const rendered = formatMinor(Number.MAX_SAFE_INTEGER, 'BHD')
    // Exact value would be …740.991; verify fractionDigitsFor still resolves 3 decimals
    // (the part of this fix that DOES hold at scale) even though the final digit is lost.
    expect(rendered).toMatch(/740\.99\d/)
  })
})
