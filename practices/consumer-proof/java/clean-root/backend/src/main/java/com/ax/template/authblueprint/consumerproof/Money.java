package com.ax.template.authblueprint.consumerproof;

import java.math.BigDecimal;
import java.util.Currency;

// Minimal stand-in for the catalog's common/Money seam. The two-arg
// BigDecimal.valueOf(unscaled, scale) form is the SAFE conversion and is NOT
// matched by money_boundary_seam_guard (its argument is a parameter, not a
// money getter).
public final class Money {
    private Money() {}

    public static BigDecimal toMajorUnits(long minor, String currencyCode) {
        int fractionDigits = Currency.getInstance(currencyCode).getDefaultFractionDigits();
        return BigDecimal.valueOf(minor, fractionDigits);
    }
}
