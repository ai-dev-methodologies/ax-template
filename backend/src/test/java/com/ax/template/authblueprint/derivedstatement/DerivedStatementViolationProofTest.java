package com.ax.template.authblueprint.derivedstatement;

import jakarta.persistence.Column;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for derived-statement-l0. Structural assertions a deliberate break cannot
 * pass silently: every DerivedStatement column is immutable ({@code updatable=false}), the
 * entity carries no public setter, and the DB unique constraint backing STMT-DERIVE-001's
 * identity is present in the migration.
 */
@Tag("DERIVEDSTATEMENT")
class DerivedStatementViolationProofTest {

    // ── entity — every column immutable, no public setters ──
    @Test @Tag("STMT-IMMUTABLE-003")
    void violation_entityImmutable_noSetters() throws Exception {
        for (Method m : DerivedStatement.class.getMethods()) {
            assertThat(m.getName()).as("DerivedStatement must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "subject", "period", "versionNo", "basisHash", "basisJson",
                                     "totalAmount", "generatedAt"}) {
            Field field = DerivedStatement.class.getDeclaredField(f);
            Column col = field.getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("DerivedStatement." + f + " must be immutable").isFalse();
        }
    }

    // ── the entity has no update path at all — only the append-only constructor ──
    @Test @Tag("STMT-DERIVE-001")
    void violation_entityHasOnlyTheAppendConstructor() {
        assertThat(DerivedStatement.class.getDeclaredConstructors()).hasSize(2);
        DerivedStatement statement = new DerivedStatement(UUID.randomUUID(), "subj", "2026-01", 1,
            "hash", "[]", BigDecimal.TEN, Instant.now());
        assertThat(statement.getVersionNo()).isEqualTo(1);
    }

    // ── the migration carries the same identity backstop ──
    @Test @Tag("STMT-DERIVE-001")
    void violation_migrationCarriesTheIdentityBackstop() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V102__create_derived_statements.sql")) {
            assertThat(in).as("V102__create_derived_statements.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("CREATE TABLE derived_statements");
            assertThat(sql).contains("uq_statement_basis");
            assertThat(sql).contains("basis_hash");
        }
    }

    // ── P1-65 — save()→saveAndFlush inside a REQUIRES_NEW boundary; the replay catch is dead code
    //    without it, and a raw saveAndFlush would poison the outer requery on PostgreSQL (25P02) ──
    @Test @Tag("STMT-RETRY-002")
    void violation_racyInsertIsolatedInRequiresNewBoundary() throws Exception {
        Method insert = com.ax.template.authblueprint.common.IdempotentInsert.class
            .getMethod("insert", java.util.function.Supplier.class);
        org.springframework.transaction.annotation.Transactional tx =
            insert.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(tx).as("IdempotentInsert.insert must be @Transactional").isNotNull();
        assertThat(tx.propagation())
            .as("the racy insert must run in a REQUIRES_NEW inner tx (25P02 poisoned-tx seal)")
            .isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);
        assertThat(java.util.Arrays.stream(DerivedStatementService.class.getDeclaredFields())
                .anyMatch(f -> f.getType() == com.ax.template.authblueprint.common.IdempotentInsert.class))
            .as("DerivedStatementService must delegate its racy insert through IdempotentInsert (revert-proof)")
            .isTrue();
    }
}
