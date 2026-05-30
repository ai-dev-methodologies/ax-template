package com.ax.template.authblueprint.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * Money — the canonical conversion seam between the two monetary representations
 * the catalog deliberately uses at different layers (the #39 money-l0 reconcile,
 * 2026-05-31; see practices/DECISIONS.md "Money representation — layered boundary").
 *
 * <h2>Two representations, one boundary (NOT a contradiction — a layered seam)</h2>
 * <ul>
 *   <li><b>Storage / domain layer</b> — integer minor units as a Java {@code long}
 *       (billing-l0 {@code currency-amount-precision-explicit}, ArchUnit-enforced on
 *       {@code ..billing..}; ecommerce {@code Product.price}; frontend L0 {@code money.ts}).
 *       This is Stripe/Toss canonical wire format and the Fowler Money pattern: exact,
 *       no binary float, no per-step rounding drift.</li>
 *   <li><b>Payment / PG-edge layer</b> — {@code BigDecimal} in MAJOR units (decimal),
 *       scaled to the ISO-4217 minor-unit count (payment-l0 {@code lang-bigdecimal-for-money}
 *       + {@code payment-iso-4217-currency}). PG provider APIs and partial-refund
 *       arithmetic need decimal precision at this edge.</li>
 * </ul>
 *
 * <p><b>The reconciled rule:</b> long minor-units is the canonical INTERNAL/storage
 * representation; {@code BigDecimal} major-units is a PG-edge transport reached ONLY
 * through this seam. The conversion is NOT {@code BigDecimal.valueOf(minorLong)} — that
 * leaves the value in minor units while payment interprets a {@code BigDecimal} as MAJOR
 * units, a silent 100x over-charge for every 2-decimal currency (USD/EUR/…); zero-decimal
 * currencies like KRW/JPY only coincidentally agree. {@code money_boundary_seam_guard.sh}
 * blocks the raw {@code BigDecimal.valueOf(<minor-getter>)} anti-pattern at the
 * domain→payment boundary.
 *
 * <p>Evidence: Martin Fowler, <i>Money pattern</i> (integer minor units; never binary
 * float) — https://martinfowler.com/eaaCatalog/money.html ; ISO 4217 minor-unit count via
 * {@link Currency#getDefaultFractionDigits()} (Java SE 21).
 */
public final class Money {

    private Money() {}

    /**
     * ISO-4217 minor-unit count for the currency (USD/EUR → 2, KRW/JPY → 0, BHD/KWD → 3).
     * Pseudo-currencies whose JDK fraction-digit count is {@code -1} (e.g. XAU, XXX) are
     * treated as 0-decimal so the conversion stays total.
     *
     * @throws IllegalArgumentException if {@code currency} is not a valid ISO-4217 code
     */
    public static int fractionDigits(String currency) {
        int fd = Currency.getInstance(currency).getDefaultFractionDigits();
        return fd < 0 ? 0 : fd;
    }

    /**
     * Convert integer minor units (storage/domain {@code long}) to the major-unit
     * {@code BigDecimal} the payment/PG edge expects, with the decimal point placed at
     * the currency's minor-unit scale. {@code toMajorUnits(1099, "USD") → 10.99};
     * {@code toMajorUnits(1000, "KRW") → 1000}. This is the ONLY correct minor→major
     * conversion — never {@code BigDecimal.valueOf(minor)}.
     */
    public static BigDecimal toMajorUnits(long minor, String currency) {
        return BigDecimal.valueOf(minor, fractionDigits(currency));
    }

    /**
     * Convert a major-unit {@code BigDecimal} (e.g. a PG response / human decimal) back to
     * storage minor units. STRICT: throws if the amount carries more precision than the
     * currency allows (mirrors payment's {@code setScale(scale, UNNECESSARY)} discipline —
     * a sub-minor-unit amount at the storage boundary is a caller bug, not a rounding case).
     * {@code toMinorUnits(new BigDecimal("10.99"), "USD") → 1099}.
     *
     * @throws ArithmeticException if {@code major} has a finer scale than the currency's
     *                             minor-unit count (would lose precision)
     */
    public static long toMinorUnits(BigDecimal major, String currency) {
        return major.movePointRight(fractionDigits(currency))
            .setScale(0, RoundingMode.UNNECESSARY)
            .longValueExact();
    }
}
