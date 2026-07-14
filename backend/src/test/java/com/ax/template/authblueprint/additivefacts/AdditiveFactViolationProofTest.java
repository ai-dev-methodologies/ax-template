package com.ax.template.authblueprint.additivefacts;

import jakarta.persistence.Column;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for additive-fact-ledger-l0. Structural assertions a deliberate break cannot
 * pass silently: facts and postings are fully immutable and append-only, the frozen aggregate
 * column has no update path and the closed-implies-frozen @Check makes an inconsistent row
 * unrepresentable, the row-lock finder is PESSIMISTIC_WRITE, the fact dedup key is a real DB
 * unique constraint, NO delete path exists anywhere in the domain, and the migration carries
 * the same backstops.
 */
class AdditiveFactViolationProofTest {

    // ── FACT-ADDITIVE-ACCUM-001 — facts are fully immutable ──
    @Test @Tag("ADDITIVEFACTS") @Tag("FACT-ADDITIVE-ACCUM-001")
    void violation_factsFullyImmutable() throws Exception {
        for (Method m : Fact.class.getMethods()) {
            assertThat(m.getName()).as("Fact must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"periodId", "source", "externalFactId", "amount", "createdAt"}) {
            Column col = Fact.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("Fact." + f + " must be immutable").isFalse();
        }
        jakarta.persistence.Table table = Fact.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames())
            .containsExactly("source", "external_fact_id");
    }

    // ── FACT-LATE-DELTA-POST-002 — postings are fully immutable and append-only ──
    @Test @Tag("ADDITIVEFACTS") @Tag("FACT-LATE-DELTA-POST-002")
    void violation_postingsFullyImmutable() throws Exception {
        for (Method m : LateDeltaPosting.class.getMethods()) {
            assertThat(m.getName()).as("LateDeltaPosting must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"currentPeriodId", "originPeriodId", "factId", "amount", "postedAt"}) {
            Column col = LateDeltaPosting.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("LateDeltaPosting." + f + " must be immutable").isFalse();
        }
    }

    // ── FACT-CLOSED-PERIOD-ADD-003 — the frozen aggregate has no rewrite path ──
    @Test @Tag("ADDITIVEFACTS") @Tag("FACT-CLOSED-PERIOD-ADD-003")
    void violation_frozenAggregateHasNoRewritePath_checkBackstopped() throws Exception {
        // NOT @Column(updatable=false) — see FactPeriod.frozenAggregate javadoc: that column
        // IS legitimately written once, by close(). Immutability-after-close is enforced by
        // close() being package-private + callable only while status==OPEN (checked in
        // AdditiveFactService.close), backstopped by the @Check below.
        Method close = Arrays.stream(FactPeriod.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("close")).findFirst().orElseThrow();
        assertThat(Modifier.isPublic(close.getModifiers()))
            .as("FactPeriod.close must be package-private (sole mutator)").isFalse();
        Check check = FactPeriod.class.getAnnotation(Check.class);
        assertThat(check.constraints().replaceAll("\\s+", " "))
            .contains("status = 'OPEN' OR frozen_aggregate IS NOT NULL");
        assertThat(FactPeriod.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── FACT-IDEMPOTENT-004 — the row-lock finder is PESSIMISTIC_WRITE ──
    @Test @Tag("ADDITIVEFACTS") @Tag("FACT-IDEMPOTENT-004")
    void violation_lockedFinderIsPessimisticWrite() throws Exception {
        Method locked = FactPeriodRepository.class.getMethod("findByIdForUpdate", java.util.UUID.class);
        Lock lock = locked.getAnnotation(Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    // ── no delete path anywhere — facts and postings are never erased ──
    @Test @Tag("ADDITIVEFACTS") @Tag("FACT-ADDITIVE-ACCUM-001")
    void violation_noDeletePath() throws Exception {
        for (Method m : FactPeriodRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"AdditiveFactService", "AdditiveFactController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "additivefacts", src + ".java"));
            assertThat(text).as(src + " must contain no delete call")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("ADDITIVEFACTS") @Tag("FACT-CLOSED-PERIOD-ADD-003")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(
                "/db/migration/V100__create_additive_fact_ledger.sql")) {
            assertThat(in).as("V100__create_additive_fact_ledger.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("status = 'OPEN' OR frozen_aggregate IS NOT NULL");
            assertThat(sql).contains("UNIQUE INDEX uq_fact_source_external_id");
        }
    }
}
