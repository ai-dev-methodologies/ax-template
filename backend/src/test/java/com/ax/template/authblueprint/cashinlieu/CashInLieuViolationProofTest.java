package com.ax.template.authblueprint.cashinlieu;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for cash-in-lieu-l0. Structural assertions that a deliberate break cannot pass
 * silently: every column is immutable, no public setter exists, no delete path exists, the entity
 * carries the @Check implication, and the migration carries the same backstops — no Spring context.
 */
@Tag("CASH_IN_LIEU")
class CashInLieuViolationProofTest {

    // ── CIL-IDEMPOTENT-003 — no public setter; every column immutable ──
    @Test @Tag("CIL-IDEMPOTENT-003")
    void violation_noPublicSetter_everyColumnImmutable() throws Exception {
        for (Method m : CashInLieuAllocation.class.getMethods()) {
            assertThat(m.getName()).as("CashInLieuAllocation must expose no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "subjectRef", "eventRef", "rawEntitlement", "unitsInKind",
                "fractionalRemainder", "cashRate", "cashValue", "allocatedAt"}) {
            Column col = CashInLieuAllocation.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("CashInLieuAllocation." + f + " must be immutable").isFalse();
        }
    }

    // ── CIL-FRACTION-001 — the entity carries the units/remainder/rate/cash @Check ──
    @Test @Tag("CIL-FRACTION-001")
    void violation_entityCarriesTheCheckImplication() {
        Check check = CashInLieuAllocation.class.getAnnotation(Check.class);
        assertThat(check).as("CashInLieuAllocation must carry @Check").isNotNull();
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("fractional_remainder >= 0 AND fractional_remainder < 1");
        assertThat(c).contains("cash_rate > 0");
    }

    // ── CIL-IDEMPOTENT-003 — uq(subject_ref, event_ref) declared on the entity ──
    @Test @Tag("CIL-IDEMPOTENT-003")
    void violation_uniqueSubjectEventConstraintDeclared() {
        Table table = CashInLieuAllocation.class.getAnnotation(Table.class);
        assertThat(table).isNotNull();
        assertThat(Arrays.stream(table.uniqueConstraints()).map(UniqueConstraint::name))
            .contains("uq_cil_subject_event");
    }

    // ── P1-64 — the racy insert crosses a REQUIRES_NEW boundary (poisoned-tx seal) ──
    @Test @Tag("CIL-IDEMPOTENT-003")
    void violation_racyInsertIsolatedInRequiresNewBoundary() throws Exception {
        java.lang.reflect.Method insert = com.ax.template.authblueprint.common.IdempotentInsert.class
            .getMethod("insert", java.util.function.Supplier.class);
        org.springframework.transaction.annotation.Transactional tx =
            insert.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(tx).as("IdempotentInsert.insert must be @Transactional").isNotNull();
        assertThat(tx.propagation())
            .as("the racy insert must run in a REQUIRES_NEW inner tx so a uq violation cannot poison "
                + "the outer requery on PostgreSQL (25P02)")
            .isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);
        assertThat(Arrays.stream(CashInLieuService.class.getDeclaredFields())
                .anyMatch(f -> f.getType() == com.ax.template.authblueprint.common.IdempotentInsert.class))
            .as("CashInLieuService must delegate its racy insert through IdempotentInsert (revert-proof)")
            .isTrue();
    }

    // ── no delete method anywhere in the repository ──
    @Test @Tag("CIL-IDEMPOTENT-003")
    void violation_noDeleteMethodDeclared() {
        for (Method m : CashInLieuAllocationRepository.class.getDeclaredMethods()) {
            assertThat(m.getName().toLowerCase())
                .as("CashInLieuAllocationRepository must declare no delete method")
                .doesNotContain("delete");
        }
    }

    // ── the migration carries the same backstops as the entity ──
    @Test @Tag("CIL-FRACTION-001") @Tag("CIL-IDEMPOTENT-003")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V106__create_cash_in_lieu.sql")) {
            assertThat(in).as("V106__create_cash_in_lieu.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("fractional_remainder >= 0 AND fractional_remainder < 1");
            assertThat(sql).contains("UNIQUE INDEX uq_cil_subject_event");
        }
    }
}
