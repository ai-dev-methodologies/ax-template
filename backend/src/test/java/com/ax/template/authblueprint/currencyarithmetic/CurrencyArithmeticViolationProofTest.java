package com.ax.template.authblueprint.currencyarithmetic;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Violation-proof tests for currency-arithmetic-l0.yaml — direct (no-Spring) invocation of the pure
 * value object plus reflection-based structural negatives. They prove the invariant is
 * UNREPRESENTABLE to violate:
 * <ul>
 *   <li>FAIL-CLOSED: {@link CurrencyMoney#plus}/{@link CurrencyMoney#minus} across DIFFERENT
 *       currencies throw — there is no silent-coercion path. The guard is symmetric across add and
 *       subtract.</li>
 *   <li>SAME-CURRENCY OK: same-currency arithmetic yields the exact integer result in that same
 *       currency.</li>
 *   <li>EXPLICIT CONVERT: {@link CurrencyMoney#convertedVia} is the only cross-currency seam and it
 *       rejects a from-currency mismatch.</li>
 *   <li>The persisted {@link CurrencyLedger} carries an immutable currency tag (so the guard cannot
 *       be evaded by mutating it), {@code @Version}, immutable identity, and no public setters.</li>
 * </ul>
 */
@Tag("CURRENCY_ARITHMETIC")
class CurrencyArithmeticViolationProofTest {

    // ─── FAIL-CLOSED: cross-currency arithmetic cannot silently succeed ───────────────

    /**
     * CCY-FAILCLOSED-ADD (keystone): adding two amounts of different currencies throws
     * CURRENCY_MISMATCH — invoked directly, the value object reads no injected collaborator, so there
     * is no path that silently coerces a foreign-currency addend.
     */
    @Test
    @Tag("CCY-FAILCLOSED-ADD")
    void violation_crossCurrencyAddThrows() {
        CurrencyMoney usd = new CurrencyMoney(1099L, "USD");
        CurrencyMoney krw = new CurrencyMoney(1000L, "KRW");

        assertThatThrownBy(() -> usd.plus(krw))
            .as("adding KRW to USD MUST fail closed")
            .isInstanceOf(CurrencyArithmeticException.class)
            .satisfies(e -> assertThat(((CurrencyArithmeticException) e).code()).isEqualTo("CURRENCY_MISMATCH"));
    }

    /**
     * CCY-FAILCLOSED-SUBTRACT: subtraction is symmetric — a differing-currency subtrahend throws too,
     * so subtraction is never a back-door around the currency-tag check.
     */
    @Test
    @Tag("CCY-FAILCLOSED-SUBTRACT")
    void violation_crossCurrencySubtractThrows() {
        CurrencyMoney usd = new CurrencyMoney(5000L, "USD");
        CurrencyMoney eur = new CurrencyMoney(100L, "EUR");

        assertThatThrownBy(() -> usd.minus(eur))
            .as("subtracting EUR from USD MUST fail closed")
            .isInstanceOf(CurrencyArithmeticException.class)
            .satisfies(e -> assertThat(((CurrencyArithmeticException) e).code()).isEqualTo("CURRENCY_MISMATCH"));
    }

    /** An invalid (non-ISO-4217) currency code is rejected at construction (CURRENCY_INVALID). */
    @Test
    @Tag("CCY-FAILCLOSED-ADD")
    void violation_nonIsoCurrencyRejected() {
        assertThatThrownBy(() -> new CurrencyMoney(100L, "XYZ"))
            .isInstanceOf(CurrencyArithmeticException.class)
            .satisfies(e -> assertThat(((CurrencyArithmeticException) e).code()).isEqualTo("CURRENCY_INVALID"));
    }

    // ─── SAME-CURRENCY OK: the guard never blocks legitimate arithmetic ───────────────

    /**
     * CCY-SAMECCY-OK: same-currency add/subtract yields the EXACT integer result in the same
     * currency — no rounding, no float, no currency change.
     */
    @Test
    @Tag("CCY-SAMECCY-OK")
    void violation_sameCurrencyArithmeticIsExact() {
        CurrencyMoney base = new CurrencyMoney(1099L, "USD");

        CurrencyMoney sum = base.plus(new CurrencyMoney(250L, "USD"));
        assertThat(sum.minorUnits()).isEqualTo(1349L);
        assertThat(sum.currency()).isEqualTo("USD");

        CurrencyMoney diff = base.minus(new CurrencyMoney(99L, "USD"));
        assertThat(diff.minorUnits()).isEqualTo(1000L);
        assertThat(diff.currency()).isEqualTo("USD");
    }

    // ─── EXPLICIT CONVERT: the only cross-currency seam, and it is guarded ─────────────

    /**
     * CCY-EXPLICIT-CONVERT: an explicit recorded conversion re-tags an amount into another currency,
     * after which a same-currency add succeeds — but a conversion whose fromCurrency does not match
     * the amount is rejected (CURRENCY_CONVERSION_MISMATCH). The converted amount is supplied (no
     * rate is computed).
     */
    @Test
    @Tag("CCY-EXPLICIT-CONVERT")
    void violation_conversionIsTheOnlySeam_andIsGuarded() {
        CurrencyMoney krw = new CurrencyMoney(130000L, "KRW");

        // A correct conversion re-tags KRW → USD, then a same-currency add succeeds.
        CurrencyMoney converted = krw.convertedVia(new CurrencyConversion("KRW", "USD", 1000L));
        assertThat(converted.currency()).isEqualTo("USD");
        assertThat(converted.minorUnits()).isEqualTo(1000L);
        assertThat(new CurrencyMoney(1000L, "USD").plus(converted).minorUnits()).isEqualTo(2000L);

        // A conversion whose fromCurrency mismatches the amount is rejected — no silent re-tag.
        assertThatThrownBy(() -> krw.convertedVia(new CurrencyConversion("USD", "EUR", 900L)))
            .isInstanceOf(CurrencyArithmeticException.class)
            .satisfies(e -> assertThat(((CurrencyArithmeticException) e).code())
                .isEqualTo("CURRENCY_CONVERSION_MISMATCH"));
    }

    // ─── persistence: the ledger's currency tag is immutable + no public setter ───────

    /**
     * The ledger's currency tag is {@code @Column(updatable=false)} — so a cross-currency add can
     * never be retroactively legitimized by re-pointing the tag — along with the identity columns.
     */
    @Test
    @Tag("CCY-FAILCLOSED-ADD")
    void violation_ledgerCurrencyTagAndIdentityImmutable() throws NoSuchFieldException {
        for (String field : new String[] {"id", "currencyCode", "createdAt"}) {
            Column col = CurrencyLedger.class.getDeclaredField(field).getAnnotation(Column.class);
            assertThat(col).as("CurrencyLedger.%s MUST be a @Column", field).isNotNull();
            assertThat(col.updatable())
                .as("CurrencyLedger.%s MUST be updatable=false (immutable)", field)
                .isFalse();
        }
    }

    /** Optimistic-locking @Version is present on the persisted ledger (concurrent-mutation guard). */
    @Test
    @Tag("CCY-FAILCLOSED-ADD")
    void violation_versionFieldPresent() {
        boolean hasVersion = Arrays.stream(CurrencyLedger.class.getDeclaredFields())
            .anyMatch(f -> f.isAnnotationPresent(Version.class));
        assertThat(hasVersion).as("CurrencyLedger MUST carry an @Version field").isTrue();
    }

    /**
     * No public setter on the ledger — the balance moves ONLY through the sole-mutator service's
     * fail-closed arithmetic, never set to a coerced cross-currency value out of band.
     */
    @Test
    @Tag("CCY-FAILCLOSED-ADD")
    void violation_noPublicSetters() {
        long setters = Arrays.stream(CurrencyLedger.class.getDeclaredMethods())
            .filter(m -> m.getName().startsWith("set") && Modifier.isPublic(m.getModifiers()))
            .count();
        assertThat(setters)
            .as("CurrencyLedger MUST have zero public setters — the balance is computed, never set")
            .isZero();
    }
}
