package com.ax.template.authblueprint.taxapplication;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Violation-proof tests for tax-application-l0.yaml — reflection-only structural negatives plus a
 * direct (no-Spring) invocation of the pure computation. They prove the two invariants are
 * UNREPRESENTABLE to violate:
 * <ul>
 *   <li>IDEMPOTENT-RECOMPUTE: a UNIQUE constraint on {@code order_id} makes a SECOND tax row per
 *       order structurally impossible, identity is immutable, and there is no public setter — the
 *       amount can only move through the sole-mutator service, which converges to one row.</li>
 *   <li>EXEMPT-SKIP: the pure {@code taxableBase}/{@code computeTax} cannot yield a non-zero tax on
 *       an exempt scope — an exempt customer and every exempt line contribute 0.</li>
 * </ul>
 */
@Tag("TAX_APPLICATION")
class TaxApplicationViolationProofTest {

    // ─── IDEMPOTENT-RECOMPUTE: one-record-per-order is structurally enforced ─────────

    /**
     * TAX-IDEMPOTENT-RECOMPUTE-001: the single-record invariant is enforced by a UNIQUE constraint
     * on {@code order_id} — a SECOND tax row for the same order is unrepresentable at the DB layer.
     */
    @Test
    @Tag("TAX-IDEMPOTENT-RECOMPUTE-001")
    void violation_uniqueOrderIdMakesSecondRowUnrepresentable() {
        Table table = TaxAssessment.class.getAnnotation(Table.class);
        assertThat(table).as("TaxAssessment MUST carry an @Table").isNotNull();
        boolean uniqueOnOrderId = Arrays.stream(table.uniqueConstraints())
            .map(UniqueConstraint::columnNames)
            .anyMatch(cols -> Arrays.asList(cols).contains("order_id"));
        assertThat(uniqueOnOrderId)
            .as("TaxAssessment MUST declare a UNIQUE constraint on order_id (one tax row per order)")
            .isTrue();
    }

    /**
     * TAX-IDEMPOTENT-RECOMPUTE-001 / persistence: identity columns are @Column(updatable=false), so
     * the order this tax belongs to can never be re-pointed, and the @Check forbids a negative tax.
     */
    @Test
    @Tag("TAX-IDEMPOTENT-RECOMPUTE-001")
    void violation_assessmentIdentityImmutableAndChecked() throws NoSuchFieldException {
        for (String field : new String[] {"id", "orderId"}) {
            Column col = TaxAssessment.class.getDeclaredField(field).getAnnotation(Column.class);
            assertThat(col).as("TaxAssessment.%s MUST be a @Column", field).isNotNull();
            assertThat(col.updatable())
                .as("TaxAssessment.%s MUST be updatable=false (immutable identity)", field)
                .isFalse();
        }
        Check chk = TaxAssessment.class.getAnnotation(Check.class);
        assertThat(chk).as("TaxAssessment MUST carry an @Check").isNotNull();
        assertThat(chk.constraints())
            .as("the @Check MUST forbid a negative tax amount")
            .contains("tax_amount_minor >= 0");
    }

    /** Optimistic-locking @Version is present on the derived record (concurrent-recompute guard). */
    @Test
    @Tag("TAX-IDEMPOTENT-RECOMPUTE-001")
    void violation_versionFieldsPresent() {
        assertThat(hasVersion(TaxAssessment.class))
            .as("TaxAssessment MUST carry an @Version field").isTrue();
        assertThat(hasVersion(TaxableOrder.class))
            .as("TaxableOrder MUST carry an @Version field").isTrue();
    }

    /**
     * No public setter on either aggregate — the tax amount moves ONLY through the sole-mutator
     * service's recompute, so it can never be set to a client-asserted value out of band.
     */
    @Test
    @Tag("TAX-IDEMPOTENT-RECOMPUTE-001")
    void violation_noPublicSetters() {
        assertThat(publicSetters(TaxAssessment.class))
            .as("TaxAssessment MUST have zero public setters — the amount is recomputed, never set").isZero();
        assertThat(publicSetters(TaxableOrder.class))
            .as("TaxableOrder MUST have zero public setters").isZero();
    }

    /** TaxableOrder identity columns (id, createdAt) are immutable. */
    @Test
    @Tag("TAX-IDEMPOTENT-RECOMPUTE-001")
    void violation_orderIdentityImmutable() throws NoSuchFieldException {
        for (String field : new String[] {"id", "createdAt"}) {
            Column col = TaxableOrder.class.getDeclaredField(field).getAnnotation(Column.class);
            assertThat(col).as("TaxableOrder.%s MUST be a @Column", field).isNotNull();
            assertThat(col.updatable())
                .as("TaxableOrder.%s MUST be updatable=false (immutable identity)", field)
                .isFalse();
        }
    }

    // ─── EXEMPT-SKIP: the exempt path cannot yield a non-zero tax ─────────────────────

    /**
     * TAX-EXEMPT-SKIP-001 (keystone): the pure computation skips every declared-exempt scope. An
     * exempt customer yields taxable base 0; an exempt line contributes 0; a zero base yields tax 0.
     * Invoked directly (no Spring) — the functions read no injected collaborator.
     */
    @Test
    @Tag("TAX-EXEMPT-SKIP-001")
    void violation_exemptScopeCannotYieldNonZeroTax() {
        // Exempt customer → taxable base 0 → tax 0, regardless of line amounts.
        TaxableOrder exemptCustomer = new TaxableOrder(UUID.randomUUID(), true,
            List.of(new TaxLine(50_000L, false)), Instant.EPOCH);
        long exemptBase = TaxApplicationService.taxableBase(exemptCustomer);
        assertThat(exemptBase).as("exempt customer MUST have taxable base 0").isZero();
        assertThat(TaxApplicationService.computeTax(exemptBase, 1000L))
            .as("exempt customer MUST yield 0 tax").isZero();

        // Exempt line contributes 0 — only the non-exempt base is taxable.
        TaxableOrder mixed = new TaxableOrder(UUID.randomUUID(), false,
            List.of(new TaxLine(10_000L, false), new TaxLine(99_999L, true)), Instant.EPOCH);
        assertThat(TaxApplicationService.taxableBase(mixed))
            .as("an exempt line MUST contribute 0 — only the non-exempt 10000 is taxable").isEqualTo(10_000L);

        // A zero base yields exactly 0 tax (the exempt path can never conjure tax).
        assertThat(TaxApplicationService.computeTax(0L, 9999L))
            .as("a zero taxable base MUST yield 0 tax").isZero();
    }

    /**
     * TAX-EXEMPT-SKIP-001: tax = round(base × rate) is half-up in integer minor units — the non-
     * exempt base alone is taxed, with deterministic rounding (no floating-point drift).
     */
    @Test
    @Tag("TAX-EXEMPT-SKIP-001")
    void violation_taxIsRoundHalfUpOfNonExemptBase() {
        // 10000 × 1000 bp (10%) = exactly 1000.
        assertThat(TaxApplicationService.computeTax(10_000L, 1000L)).isEqualTo(1000L);
        // 12345 × 1000 bp = 1234.5 → 1235 (half-up).
        assertThat(TaxApplicationService.computeTax(12_345L, 1000L)).isEqualTo(1235L);
        // 12344 × 1000 bp = 1234.4 → 1234 (rounds down).
        assertThat(TaxApplicationService.computeTax(12_344L, 1000L)).isEqualTo(1234L);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────────────

    private static boolean hasVersion(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields()).anyMatch(f -> f.isAnnotationPresent(Version.class));
    }

    private static long publicSetters(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .filter(m -> m.getName().startsWith("set") && Modifier.isPublic(m.getModifiers()))
            .count();
    }
}
