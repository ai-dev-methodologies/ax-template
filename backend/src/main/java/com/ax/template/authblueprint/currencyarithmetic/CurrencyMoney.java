package com.ax.template.authblueprint.currencyarithmetic;

import java.util.Currency;

/**
 * A currency-TAGGED monetary value — an amount in integer minor units paired with its ISO-4217
 * alpha-3 currency code. An immutable, framework-free value object (a {@code record}) whose
 * arithmetic is <b>fail-closed across currencies</b>: this is the type {@code common/Money} is not.
 * {@code common/Money} is a precision utility over a bare {@code long} that carries NO currency, so
 * it cannot reject a cross-currency add; {@link CurrencyMoney} adds the missing currency tag and the
 * guard.
 *
 * <p>The single portable invariant (currency-arithmetic-l0):
 * <ul>
 *   <li><b>FAIL-CLOSED</b> — {@link #plus}/{@link #minus} require both operands to carry the SAME
 *       currency; a mismatch throws {@link CurrencyArithmeticException} (CURRENCY_MISMATCH → 422)
 *       BEFORE producing any value. There is no silent-coercion path (no implicit re-tag, no
 *       left-operand-wins, no zero-rate assumption), and the guard is symmetric across add/subtract.</li>
 *   <li><b>SAME-CURRENCY OK</b> — same-currency arithmetic returns a new amount with the exact
 *       integer sum/difference in that same currency ({@code Math.addExact}/{@code subtractExact};
 *       never binary float).</li>
 *   <li><b>EXPLICIT RECORDED CONVERSION</b> — the ONLY cross-currency seam is
 *       {@link #convertedVia(CurrencyConversion)}, which re-tags one operand into the other's
 *       currency using a SUPPLIED converted amount (the exchange rate itself is out of scope — this
 *       type neither looks up nor computes a rate).</li>
 * </ul>
 *
 * <p>Money is integer minor units throughout (Fowler's Money pattern — never binary float).
 */
public record CurrencyMoney(long minorUnits, String currency) {

    /** Normalize + validate the currency tag at construction — every CurrencyMoney is ISO-4217. */
    public CurrencyMoney {
        currency = requireIso4217(currency);
    }

    /**
     * Fail-closed addition: the addend MUST carry the same currency, else this throws
     * {@link CurrencyArithmeticException} (CURRENCY_MISMATCH) before producing any value. On a
     * match, returns a new amount with the exact integer sum in the same currency.
     */
    public CurrencyMoney plus(CurrencyMoney addend) {
        requireSameCurrency(addend);
        return new CurrencyMoney(Math.addExact(minorUnits, addend.minorUnits), currency);
    }

    /**
     * Fail-closed subtraction — symmetric with {@link #plus}: a differing currency throws, so
     * subtraction is never a back-door around the currency-tag check.
     */
    public CurrencyMoney minus(CurrencyMoney subtrahend) {
        requireSameCurrency(subtrahend);
        return new CurrencyMoney(Math.subtractExact(minorUnits, subtrahend.minorUnits), currency);
    }

    /**
     * The ONLY sanctioned cross-currency seam: apply an explicit, recorded {@link CurrencyConversion}
     * to re-tag this amount into the conversion's {@code toCurrency}. Rejects a conversion whose
     * {@code fromCurrency} does not match this amount's currency. The converted amount is SUPPLIED by
     * the conversion — no exchange rate is computed or looked up here (rate math is out of scope).
     */
    public CurrencyMoney convertedVia(CurrencyConversion conversion) {
        if (!conversion.fromCurrency().equals(currency)) {
            throw CurrencyArithmeticException.conversionMismatch(currency, conversion.fromCurrency());
        }
        return new CurrencyMoney(conversion.convertedMinorUnits(), conversion.toCurrency());
    }

    private void requireSameCurrency(CurrencyMoney other) {
        if (!currency.equals(other.currency)) {
            throw CurrencyArithmeticException.currencyMismatch(currency, other.currency);
        }
    }

    /**
     * Validate + normalize an ISO-4217 alpha-3 code via {@link Currency#getInstance(String)}; a
     * non-ISO code yields a 422 CURRENCY_INVALID rather than a raw {@link IllegalArgumentException}.
     * Package-private so {@link CurrencyConversion} reuses the same validation (DRY).
     */
    static String requireIso4217(String code) {
        if (code == null) {
            throw CurrencyArithmeticException.invalidCurrency("null");
        }
        try {
            return Currency.getInstance(code).getCurrencyCode();
        } catch (IllegalArgumentException e) {
            throw CurrencyArithmeticException.invalidCurrency(code);
        }
    }
}
