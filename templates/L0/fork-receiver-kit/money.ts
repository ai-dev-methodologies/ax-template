/*
---
template_id: L0/fork-receiver-kit/money
layer: L0
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Martin Fowler — Money pattern (store amounts as integer minor units; never use binary floating point for money)"
    url: "https://martinfowler.com/eaaCatalog/money.html"
  - source_type: internal
    rationale: "FDW1 (frontend dogfood): the practices-react rule currency-amount-precision-explicit forbids float math and mandates integer minor units, but shipped NO helper — so everyone reinvents the conversion and risks the exact 19.99*100 = 1998.9999… float bug the rule warns about (senior + staff both flagged it). String-based, round-half-up, bigint-safe."
imports_from: []
imports_forbidden: [L1, L2, L3, L4, app/, lib/]
---
*/

/**
 * money — convert between human "major" decimal strings and integer "minor"
 * units (cents/원-subunits) WITHOUT binary floating point. Backend money is an
 * integer minor-unit `long` (billing-l0) — keep the client representation
 * integer too and only format at the very edge.
 *
 * `toMinorUnits('19.99')  -> 1999n`
 * `toMajorUnits(1999n)     -> '19.99'`
 *
 * ## Wire type at the JSON boundary (FMW4d)
 *
 * Three representations exist; do NOT confuse them:
 *
 * | Stage            | Type        | Why |
 * |------------------|-------------|-----|
 * | On the wire      | JSON number | Backend money is a Java `long` (integer minor units). Over JSON that arrives as a JS `number`, exact for integers up to `Number.MAX_SAFE_INTEGER` (2^53−1 ≈ 9.0e15 — 9 quadrillion 원). |
 * | In client math   | `bigint`    | `parseMinor(wire)` lifts the wire value into a `bigint` so additions/multiplications cannot overflow or pick up float error. ALL money arithmetic happens in `bigint`. |
 * | User-facing text | `string`    | `toMajorUnits` / `Intl.NumberFormat` at the very edge only. |
 *
 * NEVER store money as a JS `number` you do arithmetic on (the `19.99 * 100 =
 * 1998.9999…` float bug), and NEVER `JSON.stringify` a `bigint` directly (it
 * throws) — use {@link serializeMinor} to choose the wire form. For magnitudes
 * beyond `Number.MAX_SAFE_INTEGER` the only lossless wire form is a decimal
 * string; `serializeMinor` returns one and your API contract must accept it.
 */

/**
 * toMinorUnits — parse a decimal amount string (or number) into integer minor
 * units as a bigint. Rounds half-up on any digits beyond `fractionDigits`.
 * Throws `RangeError` on a non-decimal input rather than silently producing NaN.
 */
export function toMinorUnits(amount: string | number, fractionDigits = 2): bigint {
  const raw = typeof amount === 'number' ? String(amount) : amount.trim()
  const negative = raw.startsWith('-')
  const body = raw.replace(/^[+-]/, '')
  if (body === '' || body === '.' || !/^\d*\.?\d*$/.test(body)) {
    throw new RangeError(`toMinorUnits: not a decimal amount: ${JSON.stringify(amount)}`)
  }

  const dot = body.indexOf('.')
  const intPart = dot === -1 ? body : body.slice(0, dot)
  const fracRaw = dot === -1 ? '' : body.slice(dot + 1)

  let frac = fracRaw
  let roundUp = false
  if (frac.length > fractionDigits) {
    // half-up: inspect the first dropped digit
    roundUp = frac.charCodeAt(fractionDigits) >= 53 /* '5' */
    frac = frac.slice(0, fractionDigits)
  } else {
    frac = frac.padEnd(fractionDigits, '0')
  }

  let minor = BigInt((intPart || '0') + (frac || ''))
  if (roundUp) minor += 1n
  return negative ? -minor : minor
}

/**
 * toMajorUnits — format integer minor units as a fixed-precision major-unit
 * string (no thousands separators; use Intl.NumberFormat for display locale).
 */
export function toMajorUnits(minor: bigint | number, fractionDigits = 2): string {
  const n = typeof minor === 'bigint' ? minor : BigInt(Math.trunc(minor))
  const negative = n < 0n
  const digits = (negative ? -n : n).toString().padStart(fractionDigits + 1, '0')
  const cut = digits.length - fractionDigits
  const intPart = digits.slice(0, cut)
  const fracPart = digits.slice(cut)
  const body = fractionDigits > 0 ? `${intPart}.${fracPart}` : intPart
  return negative ? `-${body}` : body
}

/**
 * parseMinor — lift an integer minor-unit value off the JSON wire into a
 * `bigint` for safe arithmetic. The backend sends minor units as a JSON
 * number (Java `long`); a contract that exceeds 2^53 sends a string instead.
 * Accepts either, plus a `bigint` pass-through. Throws `RangeError` on a
 * non-integer (e.g. a stray decimal that means someone leaked major units onto
 * the wire) rather than silently truncating.
 *
 * `parseMinor(1999)    -> 1999n`
 * `parseMinor('1999')  -> 1999n`
 */
export function parseMinor(value: number | string | bigint): bigint {
  if (typeof value === 'bigint') return value
  if (typeof value === 'number') {
    if (!Number.isInteger(value)) {
      throw new RangeError(`parseMinor: minor units must be an integer, got ${value}`)
    }
    return BigInt(value)
  }
  const trimmed = value.trim()
  if (!/^[+-]?\d+$/.test(trimmed)) {
    throw new RangeError(`parseMinor: not an integer minor-unit string: ${JSON.stringify(value)}`)
  }
  return BigInt(trimmed)
}

/**
 * serializeMinor — choose the lossless JSON wire form for a minor-unit
 * `bigint`. Returns a `number` when the value fits in `Number.MAX_SAFE_INTEGER`
 * (so `JSON.stringify` emits a plain number), and a decimal `string` otherwise
 * — because `JSON.stringify(bigint)` throws and `Number(hugeBigint)` would lose
 * precision. Your API contract for the large-value case must accept a string.
 *
 * `serializeMinor(1999n)                 -> 1999`
 * `serializeMinor(9_007_199_254_740_993n) -> '9007199254740993'`
 */
export function serializeMinor(minor: bigint): number | string {
  const MAX_SAFE = BigInt(Number.MAX_SAFE_INTEGER)
  if (minor <= MAX_SAFE && minor >= -MAX_SAFE) return Number(minor)
  return minor.toString()
}

/** ISO 4217 currencies with 0 minor-unit decimal places (no fractional unit). */
const ZERO_DECIMAL_CURRENCIES: ReadonlySet<string> = new Set([
  'BIF', 'CLP', 'DJF', 'GNF', 'ISK', 'JPY', 'KMF', 'KRW',
  'PYG', 'RWF', 'UGX', 'VND', 'VUV', 'XAF', 'XOF', 'XPF',
])

/**
 * ISO 4217 currencies with 3 minor-unit decimal places (1 dinar = 1000 fils).
 * Fallback only — {@link fractionDigitsFor} asks Intl/ICU first.
 */
const THREE_DECIMAL_CURRENCIES: ReadonlySet<string> = new Set([
  'BHD', 'IQD', 'JOD', 'KWD', 'LYD', 'OMR', 'TND',
])

/** Memo for the Intl probe — `fractionDigitsFor` is called per row/keystroke. */
const FRACTION_DIGITS_CACHE = new Map<string, number>()

/**
 * fractionDigitsFor — the ISO 4217 minor-unit width (the currency's "exponent")
 * for a currency code. Use this to size {@link toMinorUnits} / {@link toMajorUnits}
 * so a KRW '1500' becomes `1500n` (not `150000n`) — the bug a hard-coded `2`
 * would cause for 원/円 — and so 1234 BHD renders `1.234`, not `12.34`.
 *
 * The exponent is READ FROM ICU (`Intl.NumberFormat(...).resolvedOptions()
 * .maximumFractionDigits`), which carries the full ISO 4217 table, rather than
 * from a hand-maintained list: the previous zero-vs-two split silently rounded
 * every 3-decimal dinar currency (BHD/KWD/OMR/JOD/TND/IQD/LYD) by a factor of
 * 10, and these are GENERIC components that accept any ISO code even where one
 * API contract happens to restrict the set. The explicit tables below are the
 * fallback for runtimes built without currency data (and for codes ICU does not
 * know, which default to 2 — the ISO default).
 */
export function fractionDigitsFor(currency: string): number {
  const code = currency.trim().toUpperCase()
  const cached = FRACTION_DIGITS_CACHE.get(code)
  if (cached !== undefined) return cached

  let digits: number | undefined
  try {
    const resolved = new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: code,
    }).resolvedOptions().maximumFractionDigits
    if (Number.isInteger(resolved) && resolved >= 0) digits = resolved
  } catch {
    // Invalid/unsupported code, or a runtime without currency data — fall back.
  }
  if (digits === undefined) {
    digits = ZERO_DECIMAL_CURRENCIES.has(code)
      ? 0
      : THREE_DECIMAL_CURRENCIES.has(code)
        ? 3
        : 2
  }

  FRACTION_DIGITS_CACHE.set(code, digits)
  return digits
}
