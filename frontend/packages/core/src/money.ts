/**
 * fractionDigitsFor — canonical @ax/core implementation, single-sourced for
 * every in-monorepo app (BACKLOG P3-72).
 *
 * Before this file existed, `fractionDigitsFor` (+ its two ISO 4217 fallback
 * tables + memo cache) was hand-copied into `frontend/apps/pay/src/lib/money.ts`
 * because apps/pay could not import `templates/L0/fork-receiver-kit/money.ts`
 * directly: Next.js rejects module resolution outside the project root unless
 * `experimental.externalDir` is set, and `templates/` sits outside `frontend/`.
 * `@ax/core` has no such boundary problem — it is an ordinary frontend
 * workspace package (`frontend/packages/core`), already `transpilePackages`'d
 * and path-aliased into apps/pay's tsconfig — so apps/pay now imports this
 * copy instead of maintaining its own.
 *
 * `templates/L0/fork-receiver-kit/money.ts` KEEPS its own copy of this same
 * function — that is intentional, not residual drift. Every L0 file's
 * frontmatter declares `imports_from: []`, and the kit's README states it
 * "survives the fork verbatim": fork-receivers copy the L0 directory wholesale
 * into their own codebase, which will not have an `@ax/core` package. L0 must
 * stay import-free by construction, so it cannot be collapsed into this file.
 * `frontend/tests/money-source-parity.vitest.ts` guards that the two remaining
 * canonical copies (this file and the L0 kit) do not drift.
 */

/** ISO 4217 currencies with 0 minor-unit decimal places (no fractional unit). */
const ZERO_DECIMAL_CURRENCIES: ReadonlySet<string> = new Set([
  'BIF', 'CLP', 'DJF', 'GNF', 'ISK', 'JPY', 'KMF', 'KRW',
  'PYG', 'RWF', 'UGX', 'VND', 'VUV', 'XAF', 'XOF', 'XPF',
]);

/**
 * ISO 4217 currencies with 3 minor-unit decimal places (1 dinar = 1000 fils).
 * Fallback only — {@link fractionDigitsFor} asks Intl/ICU first.
 */
const THREE_DECIMAL_CURRENCIES: ReadonlySet<string> = new Set([
  'BHD', 'IQD', 'JOD', 'KWD', 'LYD', 'OMR', 'TND',
]);

/** Memo for the Intl probe — `fractionDigitsFor` is called per render/keystroke. */
const FRACTION_DIGITS_CACHE = new Map<string, number>();

/**
 * fractionDigitsFor — the ISO 4217 minor-unit width for a currency code, read
 * from ICU (`Intl.NumberFormat(...).resolvedOptions().maximumFractionDigits`,
 * which carries the full ISO 4217 table) rather than a hand-maintained list.
 * The explicit fallback sets above are for runtimes built without currency
 * data (and for codes ICU does not know, which default to 2 — the ISO
 * default).
 */
export function fractionDigitsFor(currency: string): number {
  const code = currency.trim().toUpperCase();
  const cached = FRACTION_DIGITS_CACHE.get(code);
  if (cached !== undefined) return cached;

  let digits: number | undefined;
  try {
    const resolved = new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: code,
    }).resolvedOptions().maximumFractionDigits;
    if (Number.isInteger(resolved) && resolved >= 0) digits = resolved;
  } catch {
    // Invalid/unsupported code, or a runtime without currency data — fall back.
  }
  if (digits === undefined) {
    digits = ZERO_DECIMAL_CURRENCIES.has(code)
      ? 0
      : THREE_DECIMAL_CURRENCIES.has(code)
        ? 3
        : 2;
  }

  FRACTION_DIGITS_CACHE.set(code, digits);
  return digits;
}
