/**
 * Money formatting for the fintech-trust app.
 *
 * The backend uses TWO money representations and this app must render both
 * correctly — getting either wrong is the one unforgivable bug in a payments UI:
 *
 *   1. PAYMENT domain  — `amount` is in MAJOR currency units, encoded as a JSON
 *      number that the backend scaled per ISO 4217 (KRW scale 0 -> 12900 means
 *      ₩12,900; USD scale 2 -> 70.00 means $70.00). The wire value is already a
 *      proper decimal; we must NOT multiply or divide it.
 *
 *   2. BILLING domain  — `amount` is in MINOR units, encoded as a JSON integer
 *      (`long`). 29000 with currency KRW means ₩29,000 (KRW has 0 minor digits,
 *      so minor == major); 2999 with currency USD means $29.99 (2 minor digits).
 *      We must divide by 10^fractionDigits to reach the major value.
 *
 * Both paths converge on Intl.NumberFormat with the correct fraction digits and
 * grouping, then the rendered string is shown in a tabular-nums context (see
 * .ax-fintech / .ax-money in globals.css) so digit columns line up across rows.
 *
 * We deliberately avoid floating-point arithmetic on the value itself:
 *   - major values are handed straight to Intl (no math),
 *   - minor->major conversion uses integer string surgery, not `/ 100`,
 * so a value like 2999 never round-trips through a binary float.
 */

/** ISO 4217 minor-unit (fraction) digits for the currencies this app handles. */
const FRACTION_DIGITS: Record<string, number> = {
  KRW: 0,
  USD: 2,
  EUR: 2,
  JPY: 0,
  GBP: 2,
};

function fractionDigitsFor(currency: string): number {
  return FRACTION_DIGITS[currency.toUpperCase()] ?? 2;
}

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
 * Format a PAYMENT-domain amount (MAJOR units, already-scaled decimal). Accepts
 * the raw wire value (number | string). Returns a localized currency string,
 * e.g. 12900/"KRW" -> "₩12,900", 70/"USD" -> "$70.00".
 */
export function formatMajor(amount: number | string | null | undefined, currency: string): string {
  if (amount === null || amount === undefined) return '—';
  const numeric = typeof amount === 'string' ? Number(amount) : amount;
  if (!Number.isFinite(numeric)) return '—';
  return formatter(currency).format(numeric);
}

/**
 * Format a BILLING-domain amount (MINOR units, integer long). Accepts the raw
 * wire value (number | string). The minor->major step is integer-string based,
 * so e.g. 2999/"USD" -> "$29.99" with no float drift, 29000/"KRW" -> "₩29,000".
 */
export function formatMinor(amount: number | string | null | undefined, currency: string): string {
  if (amount === null || amount === undefined) return '—';
  // Normalize to an integer string (the wire type is a JSON integer / long).
  const minorString =
    typeof amount === 'string' ? amount.trim() : String(Math.trunc(amount));
  if (!/^-?\d+$/.test(minorString)) return '—';
  const major = minorToMajorString(minorString, currency);
  return formatter(currency).format(Number(major));
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
