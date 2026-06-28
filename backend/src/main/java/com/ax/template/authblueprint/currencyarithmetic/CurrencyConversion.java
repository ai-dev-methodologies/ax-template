package com.ax.template.authblueprint.currencyarithmetic;

/**
 * An explicit, RECORDED conversion — the only sanctioned way to combine amounts of different
 * currencies. It carries the {@code fromCurrency}, the {@code toCurrency}, and the
 * already-determined {@code convertedMinorUnits} (integer minor units in the target currency).
 *
 * <p>The exchange RATE is deliberately OUT of scope: this value supplies the converted amount, it
 * does NOT compute or look up a rate. Applying it via {@link CurrencyMoney#convertedVia} re-tags an
 * amount from {@code fromCurrency} to {@code toCurrency}; the cross-currency step is therefore
 * explicit and auditable, never an implicit coercion. Both codes are validated as ISO-4217 alpha-3.
 */
public record CurrencyConversion(String fromCurrency, String toCurrency, long convertedMinorUnits) {

    public CurrencyConversion {
        fromCurrency = CurrencyMoney.requireIso4217(fromCurrency);
        toCurrency = CurrencyMoney.requireIso4217(toCurrency);
    }
}
