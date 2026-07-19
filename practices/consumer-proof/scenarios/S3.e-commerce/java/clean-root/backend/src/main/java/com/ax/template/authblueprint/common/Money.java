package com.ax.template.authblueprint.common;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Money — thin restatement of the catalog's canonical long-minor -> BigDecimal-major
 * conversion seam (see backend/src/main/java/.../common/Money.java in the real tree,
 * and money_boundary_seam_guard.sh). Included in this fixture root ONLY so the clean
 * checkout fixtures have something realistic to import/call — the guard itself is a
 * text scanner and does not require this class to compile.
 */
public final class Money {

    private Money() {}

    public static int fractionDigits(String currency) {
        int fd = Currency.getInstance(currency).getDefaultFractionDigits();
        return fd < 0 ? 0 : fd;
    }

    public static BigDecimal toMajorUnits(long minorUnits, String currency) {
        return BigDecimal.valueOf(minorUnits, fractionDigits(currency));
    }
}
