/**
 * Currency-tagged monetary arithmetic of a commerce platform: a money value
 * object paired with its ISO-4217 currency whose addition and subtraction are
 * fail-closed across currencies. This is purely the "no silent cross-currency
 * arithmetic" invariant — the exchange-rate math is out of scope.
 *
 * <h2>Correctness invariant</h2>
 * <ol>
 *   <li><b>Fail-closed across currencies.</b> Adding or subtracting two amounts
 *       whose ISO-4217 currency codes differ, without an explicit recorded
 *       conversion, MUST throw ({@code CURRENCY_MISMATCH}, mapped to 422) before
 *       producing any value — never silently coerce, never assume a shared
 *       currency, never adopt one operand's currency for the other. The guard is
 *       symmetric: subtraction is never a back-door around addition's check.</li>
 *   <li><b>Same-currency works.</b> When both operands share a currency, the
 *       result is a new amount with the exact integer sum/difference
 *       ({@link java.lang.Math#addExact(long, long)} /
 *       {@link java.lang.Math#subtractExact(long, long)}) in that same currency —
 *       no rounding, no floating point.</li>
 *   <li><b>Explicit recorded conversion.</b> The only sanctioned cross-currency
 *       path re-tags one operand into the other's currency via a supplied,
 *       persisted conversion, after which an ordinary same-currency add succeeds.
 *       The converted amount is supplied (the rate is never computed or looked up
 *       here) and the conversion is recorded so the FX step is auditable.</li>
 * </ol>
 *
 * <h2>Key components (DDD shape)</h2>
 * <ul>
 *   <li><b>Value object (the invariant lives here)</b> — {@link CurrencyMoney},
 *       an immutable record of {@code long minorUnits} plus an ISO-4217 currency;
 *       its {@link CurrencyMoney#plus(CurrencyMoney)} and
 *       {@link CurrencyMoney#minus(CurrencyMoney)} throw
 *       {@link CurrencyArithmeticException} on a currency mismatch, and
 *       {@link CurrencyMoney#convertedVia(CurrencyConversion)} is the only
 *       cross-currency seam.</li>
 *   <li><b>Conversion / audit value types</b> — {@link CurrencyConversion}
 *       ({@code fromCurrency}, {@code toCurrency}, {@code convertedMinorUnits})
 *       and {@link ConversionRecord} (an {@code @Embeddable} audit trail via
 *       {@code @ElementCollection}).</li>
 *   <li><b>Aggregate root</b> — {@link CurrencyLedger} carries an immutable
 *       currency tag and {@code @Version}; its balance is mutated only through
 *       the sole-mutator {@link CurrencyArithmeticService}.</li>
 *   <li><b>Controller surface</b> — {@link CurrencyArithmeticController} (thin
 *       HTTP); {@link CurrencyArithmeticException} renders RFC 9457 Problem
 *       Details; {@link CurrencyArithmeticMetrics} emits a bounded (outcome)
 *       counter.</li>
 * </ul>
 *
 * <h2>Verification</h2>
 * Run {@code ./gradlew testCurrencyArithmetic} (spec
 * {@code currency-arithmetic-l0}, 4 families: FAILCLOSED-ADD /
 * FAILCLOSED-SUBTRACT / SAMECCY-OK / EXPLICIT-CONVERT). The package ships
 * {@code CurrencyArithmeticViolationProofTest} asserting the fail-closed and
 * immutable-tag invariants are structurally enforced.
 *
 * <h2>External grounding</h2>
 * Currency codes are <a href="https://www.iso.org/iso-4217-currency-codes.html">ISO&nbsp;4217</a>
 * alpha-3 (validated via {@link java.util.Currency#getInstance(String)}); a silent
 * cross-currency add is the incorrect-calculation weakness
 * <a href="https://cwe.mitre.org/data/definitions/682.html">CWE-682</a>; the
 * currency-tagged value-type discipline is Martin Fowler's
 * <a href="https://martinfowler.com/eaaCatalog/money.html">Money pattern</a>.
 */
package com.ax.template.authblueprint.currencyarithmetic;
