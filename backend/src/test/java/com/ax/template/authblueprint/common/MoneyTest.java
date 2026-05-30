package com.ax.template.authblueprint.common;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * #39 money-l0 reconcile — the canonical long-minor ↔ BigDecimal-major seam.
 * Tagged COMMON_ADVICE so it rides the verify-completion hard gate (testCommonAdvice).
 */
@Tag("COMMON_ADVICE")
class MoneyTest {

    @Test
    void fractionDigits_perIso4217() {
        assertThat(Money.fractionDigits("USD")).isEqualTo(2);
        assertThat(Money.fractionDigits("EUR")).isEqualTo(2);
        assertThat(Money.fractionDigits("KRW")).isEqualTo(0);
        assertThat(Money.fractionDigits("JPY")).isEqualTo(0);
        assertThat(Money.fractionDigits("BHD")).isEqualTo(3); // 3-decimal currency
    }

    @Test
    void toMajorUnits_placesDecimalAtCurrencyScale() {
        // 2-decimal: 1099 minor cents → $10.99
        assertThat(Money.toMajorUnits(1099, "USD")).isEqualByComparingTo(new BigDecimal("10.99"));
        assertThat(Money.toMajorUnits(1099, "USD").scale()).isEqualTo(2);
        // 0-decimal: 1000 KRW minor == 1000 major (no decimal point)
        assertThat(Money.toMajorUnits(1000, "KRW")).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(Money.toMajorUnits(150, "JPY")).isEqualByComparingTo(new BigDecimal("150"));
        // 3-decimal: 1234 fils → 1.234 BHD
        assertThat(Money.toMajorUnits(1234, "BHD")).isEqualByComparingTo(new BigDecimal("1.234"));
        // zero
        assertThat(Money.toMajorUnits(0, "USD")).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void toMajorUnits_isNotTheRawValueOfBug() {
        // The #39 bug: BigDecimal.valueOf(minorLong) leaves the value in MINOR units while
        // payment interprets a BigDecimal as MAJOR units → 100x over-charge for 2-decimal
        // currencies. The seam helper must DIFFER from the raw conversion for such currencies.
        assertThat(Money.toMajorUnits(1099, "USD"))
            .isNotEqualByComparingTo(BigDecimal.valueOf(1099L)); // 10.99 != 1099
        // …and only coincidentally agrees for 0-decimal currencies (why KRW hid the bug):
        assertThat(Money.toMajorUnits(1000, "KRW"))
            .isEqualByComparingTo(BigDecimal.valueOf(1000L));
    }

    @Test
    void toMinorUnits_roundTripsFromMajor() {
        assertThat(Money.toMinorUnits(new BigDecimal("10.99"), "USD")).isEqualTo(1099L);
        assertThat(Money.toMinorUnits(new BigDecimal("1000"), "KRW")).isEqualTo(1000L);
        assertThat(Money.toMinorUnits(new BigDecimal("1.234"), "BHD")).isEqualTo(1234L);
    }

    @Test
    void roundTrip_minorToMajorToMinor_isIdentity() {
        for (long minor : new long[] {0, 1, 99, 100, 1099, 999999}) {
            for (String ccy : new String[] {"USD", "KRW", "BHD"}) {
                assertThat(Money.toMinorUnits(Money.toMajorUnits(minor, ccy), ccy))
                    .as("round-trip %d %s", minor, ccy)
                    .isEqualTo(minor);
            }
        }
    }

    @Test
    void toMinorUnits_strictlyRejectsSubMinorPrecision() {
        // 10.999 has 3 decimals but USD allows 2 → precision loss is a caller bug, not rounding.
        assertThatThrownBy(() -> Money.toMinorUnits(new BigDecimal("10.999"), "USD"))
            .isInstanceOf(ArithmeticException.class);
        // a 2-decimal amount in USD is fine
        assertThatCode(() -> Money.toMinorUnits(new BigDecimal("10.99"), "USD"))
            .doesNotThrowAnyException();
    }

    @Test
    void invalidCurrency_throws() {
        assertThatThrownBy(() -> Money.fractionDigits("NOTACURRENCY"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
