/**
 * Money formatting for the fintech-trust app.
 *
 * The app handles two kinds of value and must render both correctly — getting
 * either wrong is the one unforgivable bug in a payments UI:
 *
 *   1. WIRE values (PAYMENT + BILLING domains) — MINOR units, encoded as a JSON
 *      integer (`long`). Since the P1-68 unification BOTH domains emit this:
 *      PaymentBodyMapper.toBody runs every amount through common/Money.toMinorUnits,
 *      so a $10.99 USD payment arrives as 1099 and a ₩12,900 payment as 12900 (KRW
 *      has 0 minor digits, so minor == major — which is exactly why the earlier
 *      MAJOR-unit assumption survived KRW-only screens). Render with formatMinor;
 *      it divides by 10^fractionDigits via integer string surgery.
 *
 *   2. LOCALLY TYPED values — a MAJOR-unit decimal the user just entered in a form
 *      (the checkout amount field), not yet sent anywhere. Render with formatMajor,
 *      which hands the value to Intl untouched.
 *
 * Both paths converge on Intl.NumberFormat with the correct fraction digits and
 * grouping, then the rendered string is shown in a tabular-nums context (see
 * .ax-fintech / .ax-money in globals.css) so digit columns line up across rows.
 *
 * We deliberately avoid floating-point arithmetic on the value itself:
 *   - major values are handed straight to Intl (no math),
 *   - minor->major conversion uses integer string surgery, not `/ 100`,
 * so a value like 2999 never round-trips through a binary float.
 *
 * `fractionDigitsFor` (the ISO 4217 minor-unit width lookup) is imported from
 * `@ax/core` rather than hand-copied here (BACKLOG P3-72) — @ax/core has no
 * cross-project-root resolution problem the templates/L0 kit has for this app
 * (see @ax/core's money.ts for the full history: apps/pay used to carry its
 * own copy because it could not import templates/L0/fork-receiver-kit/money.ts
 * directly).
 */
import { fractionDigitsFor } from '@ax/core';

/** Build a grouping/decimal formatter for a currency, in Korean locale. */
function formatter(currency: string): Intl.NumberFormat {
  const digits = fractionDigitsFor(currency);
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: currency.toUpperCase(),
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  });
}

/**
 * Convert a MINOR-unit integer string to its MAJOR decimal string WITHOUT
 * floating-point math. e.g. ("2999","USD") -> "29.99"; ("29000","KRW") -> "29000".
 */
function minorToMajorString(minor: string, currency: string): string {
  const digits = fractionDigitsFor(currency);
  const negative = minor.startsWith('-');
  const raw = (negative ? minor.slice(1) : minor).replace(/^0+(?=\d)/, '');
  if (digits === 0) return (negative ? '-' : '') + raw;
  const padded = raw.padStart(digits + 1, '0');
  const whole = padded.slice(0, padded.length - digits);
  const frac = padded.slice(padded.length - digits);
  return `${negative ? '-' : ''}${whole}.${frac}`;
}

/**
 * Group an unsigned decimal-digit string into locale-style 3-digit chunks
 * (e.g. "1234567" -> "1,234,567"), without ever passing the value through a
 * `Number`. This is the standard grouping `Intl.NumberFormat` itself applies —
 * used here only to reproduce it for magnitudes bigger than
 * `Number.MAX_SAFE_INTEGER`, where the value can no longer make that round
 * trip losslessly.
 */
function groupDigits(intPart: string, separator: string): string {
  const chunks: string[] = [];
  let i = intPart.length;
  while (i > 3) {
    chunks.unshift(intPart.slice(i - 3, i));
    i -= 3;
  }
  chunks.unshift(intPart.slice(0, i));
  return chunks.join(separator);
}

/**
 * Render an exact major-unit decimal STRING (as produced by
 * {@link minorToMajorString}) through `Intl.NumberFormat`, without ever
 * converting it to a `Number` first — `Number("90071992547409.91")` cannot
 * represent that value exactly (it rounds to `…409.9`), which is precisely the
 * float-precision bug this file's top-of-file doc comment says money.ts
 * "deliberately avoids". `Intl.NumberFormat#format` does accept a decimal
 * string on runtimes with the ES2023 string-argument overload, but apps/pay's
 * tsconfig ("lib": ["ES2020", ...]) predates it and widening `lib` is outside
 * this fix's scope — so instead we source every LITERAL part (currency
 * symbol/sign/spacing/group separator/decimal separator) from
 * `formatToParts` on a small representative value, and splice in our own
 * exact digits for the digit-bearing parts (`integer`/`group`/`decimal`/
 * `fraction`). No arithmetic touches the amount at any point.
 */
function formatExactMajorString(major: string, currency: string): string {
  const negative = major.startsWith('-');
  const unsigned = negative ? major.slice(1) : major;
  const dot = unsigned.indexOf('.');
  const intPart = dot === -1 ? unsigned : unsigned.slice(0, dot);
  const fracPart = dot === -1 ? '' : unsigned.slice(dot + 1);

  // A representative value with the correct sign carries every literal in the
  // right position (sign, currency symbol, spacing) for this locale/currency —
  // its digits are discarded, only its structure and separators are used.
  const reference = formatter(currency).formatToParts(negative ? -1 : 1);
  const groupSeparator = reference.find((part) => part.type === 'group')?.value ?? ',';
  const decimalSeparator = reference.find((part) => part.type === 'decimal')?.value ?? '.';
  const groupedInt = groupDigits(intPart, groupSeparator);
  const numberText = fracPart.length > 0 ? `${groupedInt}${decimalSeparator}${fracPart}` : groupedInt;

  let out = '';
  let numberSpliced = false;
  for (const part of reference) {
    if (part.type === 'integer' || part.type === 'group' || part.type === 'decimal' || part.type === 'fraction') {
      if (!numberSpliced) {
        out += numberText;
        numberSpliced = true;
      }
      continue;
    }
    out += part.value;
  }
  return out;
}

/**
 * Format an already-MAJOR amount (a decimal the user typed, or any value known to
 * be in major units). Accepts number | string. e.g. 12900/"KRW" -> "₩12,900",
 * 70/"USD" -> "$70.00". Do NOT use this on a wire value — those are minor units
 * (formatMajor(1099, "USD") renders US$1,099.00 for a $10.99 payment).
 */
export function formatMajor(amount: number | string | null | undefined, currency: string): string {
  if (amount === null || amount === undefined) return '—';
  const numeric = typeof amount === 'string' ? Number(amount) : amount;
  if (!Number.isFinite(numeric)) return '—';
  return formatter(currency).format(numeric);
}

/**
 * Format a WIRE amount (MINOR units, integer long) — the encoding BOTH the payment
 * and billing domains emit. Accepts the raw wire value (number | string). The
 * minor->major step is integer-string based, so e.g. 1099/"USD" -> "$10.99" and
 * 2999/"USD" -> "$29.99" with no float drift, 29000/"KRW" -> "₩29,000". The final
 * render step (formatExactMajorString) never converts that exact string to a
 * `Number` either, so precision holds even at the Number.MAX_SAFE_INTEGER boundary.
 */
export function formatMinor(amount: number | string | null | undefined, currency: string): string {
  if (amount === null || amount === undefined) return '—';
  // Normalize to an integer string (the wire type is a JSON integer / long).
  const minorString =
    typeof amount === 'string' ? amount.trim() : String(Math.trunc(amount));
  if (!/^-?\d+$/.test(minorString)) return '—';
  const major = minorToMajorString(minorString, currency);
  return formatExactMajorString(major, currency);
}

/**
 * Compact currency for tiles (e.g. "₩1.2M"). Falls back to the full format for
 * small magnitudes so exact small balances are never lossy in a summary tile.
 */
export function formatMajorCompact(amount: number | string | null | undefined, currency: string): string {
  if (amount === null || amount === undefined) return '—';
  const numeric = typeof amount === 'string' ? Number(amount) : amount;
  if (!Number.isFinite(numeric)) return '—';
  const digits = fractionDigitsFor(currency);
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: currency.toUpperCase(),
    notation: 'compact',
    maximumFractionDigits: numeric >= 1_000_000 ? 1 : digits,
  }).format(numeric);
}
