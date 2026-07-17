package com.ax.template.authblueprint.withholdingsplit;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for withholding-split-l0. Structural assertions that a deliberate break cannot
 * pass silently: every posting/leg/remittance column is immutable, no public setter exists anywhere,
 * no delete path exists anywhere, and the migration carries the same backstops as the entities — no
 * Spring context.
 */
@Tag("WITHHOLDING_SPLIT")
class WithholdingSplitViolationProofTest {

    // ── WHT-IMMUTABLE-004 — no public setter anywhere in the domain ──
    @Test @Tag("WHT-IMMUTABLE-004")
    void violation_noPublicSetterAnywhere() {
        for (Class<?> entity : new Class<?>[]{WithholdingPosting.class, WithholdingLeg.class, RemittanceRun.class}) {
            for (Method m : entity.getMethods()) {
                assertThat(m.getName())
                    .as(entity.getSimpleName() + " must expose no public setter")
                    .doesNotStartWith("set");
            }
        }
    }

    // ── WHT-IMMUTABLE-004 — every column on every entity is immutable ──
    @Test @Tag("WHT-IMMUTABLE-004")
    void violation_everyColumnImmutable() throws Exception {
        assertColumnsImmutable(WithholdingPosting.class,
            "id", "grossAmount", "rate", "period", "correctionOfPostingId", "createdAt");
        assertColumnsImmutable(WithholdingLeg.class, "id", "postingId", "legType", "amount", "createdAt");
        assertColumnsImmutable(RemittanceRun.class, "id", "period", "totalWithheld", "postingCount", "collectedAt");
    }

    private void assertColumnsImmutable(Class<?> entity, String... fields) throws Exception {
        for (String f : fields) {
            Column col = entity.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(entity.getSimpleName() + "." + f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as(entity.getSimpleName() + "." + f + " must be immutable").isFalse();
        }
    }

    // ── WHT-SPLIT-001 — the posting carries the gross<>0 / rate-bounds @Check ──
    @Test @Tag("WHT-SPLIT-001")
    void violation_postingCarriesTheCheckImplication() {
        Check check = WithholdingPosting.class.getAnnotation(Check.class);
        assertThat(check).as("WithholdingPosting must carry @Check").isNotNull();
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("gross_amount <> 0");
        assertThat(c).contains("rate >= 0 AND rate < 1");
    }

    // ── WHT-REMIT-003 — uq(period) declared on the entity ──
    @Test @Tag("WHT-REMIT-003")
    void violation_remittanceRunCarriesUniquePeriodConstraint() {
        Table table = RemittanceRun.class.getAnnotation(Table.class);
        assertThat(table).isNotNull();
        assertThat(Arrays.stream(table.uniqueConstraints()).map(UniqueConstraint::name))
            .contains("uq_remittance_period");
    }

    // ── WHT-IMMUTABLE-004 — no delete method anywhere in the two repositories ──
    @Test @Tag("WHT-IMMUTABLE-004")
    void violation_noDeleteMethodDeclaredOnEitherRepository() {
        for (Class<?> repo : new Class<?>[]{WithholdingPostingRepository.class, RemittanceRunRepository.class}) {
            for (Method m : repo.getDeclaredMethods()) {
                assertThat(m.getName().toLowerCase())
                    .as(repo.getSimpleName() + " must declare no delete method")
                    .doesNotContain("delete");
            }
        }
    }

    // ── the migration carries the same backstops as the entities ──
    @Test @Tag("WHT-SPLIT-001") @Tag("WHT-REMIT-003")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V105__create_withholding_split.sql")) {
            assertThat(in).as("V105__create_withholding_split.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("gross_amount <> 0 AND rate >= 0 AND rate < 1");
            assertThat(sql).contains("UNIQUE INDEX uq_remittance_period");
        }
    }

    // ── P1-64 — the racy remittance insert crosses a REQUIRES_NEW boundary (poisoned-tx seal) ──
    @Test @Tag("WHT-REMIT-003")
    void violation_racyInsertIsolatedInRequiresNewBoundary() throws Exception {
        Method insert = com.ax.template.authblueprint.common.IdempotentInsert.class
            .getMethod("insert", java.util.function.Supplier.class);
        org.springframework.transaction.annotation.Transactional tx =
            insert.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(tx).as("IdempotentInsert.insert must be @Transactional").isNotNull();
        assertThat(tx.propagation())
            .as("the racy insert must run in a REQUIRES_NEW inner tx (25P02 poisoned-tx seal)")
            .isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);
        assertThat(Arrays.stream(RemittanceService.class.getDeclaredFields())
                .anyMatch(f -> f.getType() == com.ax.template.authblueprint.common.IdempotentInsert.class))
            .as("RemittanceService must delegate its racy insert through IdempotentInsert (revert-proof)")
            .isTrue();
    }
}
