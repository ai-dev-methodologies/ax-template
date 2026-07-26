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
 * 2999/"USD" -> "$29.99" with no float drift, 29000/"KRW" -> "₩29,000".
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
