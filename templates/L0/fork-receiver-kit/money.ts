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
