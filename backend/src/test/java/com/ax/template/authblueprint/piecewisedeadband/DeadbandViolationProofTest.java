package com.ax.template.authblueprint.piecewisedeadband;

import jakarta.persistence.Column;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for piecewise-deadband-l0. Structural assertions that a deliberate break cannot pass
 * silently: config/segment/evaluation expose no public setter and are fully immutable; the segment @Check
 * backstops start &lt; end; the migration carries the same idempotency/tiling backstops — no Spring context.
 */
@Tag("PIECEWISE_DEADBAND")
class DeadbandViolationProofTest {

    // ── config is fully immutable — no setter, every column updatable=false ──
    @Test @Tag("PIECEWISE_DEADBAND") @Tag("PWDB-SEGMENT-001")
    void violation_configNoPublicSetter_fullyImmutable() throws Exception {
        for (Method m : DeadbandConfig.class.getMethods()) {
            assertThat(m.getName()).as("DeadbandConfig must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "configKey", "domainStart", "domainEnd", "createdAt"}) {
            Column col = DeadbandConfig.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("DeadbandConfig." + f + " must be immutable").isFalse();
        }
    }

    // ── segment is fully immutable and carries the start<end / non-negative-width @Check ──
    @Test @Tag("PIECEWISE_DEADBAND") @Tag("PWDB-SEGMENT-001")
    void violation_segmentNoPublicSetter_fullyImmutable_carriesCheck() throws Exception {
        for (Method m : DeadbandSegment.class.getMethods()) {
            assertThat(m.getName()).as("DeadbandSegment must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "configId", "ordinal", "start", "end", "obligationTarget",
                "deadbandWidth"}) {
            Column col = DeadbandSegment.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("DeadbandSegment." + f + " must be immutable").isFalse();
        }
        Check check = DeadbandSegment.class.getAnnotation(Check.class);
        assertThat(check).as("DeadbandSegment must carry @Check").isNotNull();
        assertThat(check.constraints().replaceAll("\\s+", " ")).contains("segment_start < segment_end");

        // covers() is the tiling primitive — package-private, not a public API surface
        Method covers = DeadbandSegment.class.getDeclaredMethod("covers", BigDecimal.class);
        assertThat(java.lang.reflect.Modifier.isPublic(covers.getModifiers()))
            .as("DeadbandSegment.covers must stay package-private").isFalse();
    }

    // ── evaluation is fully append-only ──
    @Test @Tag("PIECEWISE_DEADBAND") @Tag("PWDB-IMMUTABLE-001")
    void violation_evaluationFullyAppendOnly() throws Exception {
        for (Method m : DeadbandEvaluation.class.getMethods()) {
            assertThat(m.getName()).as("DeadbandEvaluation must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "configId", "segmentId", "pointX", "actualValue", "obligationTarget",
                "deadbandWidth", "deviation", "compliant", "idempotencyKey", "sequenceNo", "evaluatedAt"}) {
            Column col = DeadbandEvaluation.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("DeadbandEvaluation." + f + " must be immutable").isFalse();
        }
    }

    // ── the covers() primitive correctly implements the half-open [start,end) tiling boundary ──
    @Test @Tag("PIECEWISE_DEADBAND") @Tag("PWDB-EVAL-001")
    void violation_coversIsHalfOpen_boundaryExactlyAtEndBelongsToNextSegment() throws Exception {
        DeadbandSegment s = new DeadbandSegment(UUID.randomUUID(), UUID.randomUUID(), 0,
            new BigDecimal("0.0000"), new BigDecimal("50.0000"), new BigDecimal("10.0000"),
            new BigDecimal("2.0000"));
        Method covers = DeadbandSegment.class.getDeclaredMethod("covers", BigDecimal.class);
        covers.setAccessible(true);
        assertThat((Boolean) covers.invoke(s, new BigDecimal("0.0000"))).as("start is inclusive").isTrue();
        assertThat((Boolean) covers.invoke(s, new BigDecimal("49.9999"))).isTrue();
        assertThat((Boolean) covers.invoke(s, new BigDecimal("50.0000")))
            .as("end is EXCLUSIVE — belongs to the NEXT segment, never both").isFalse();
    }

    // ── the migration carries the same tiling + idempotency backstops as the entities ──
    @Test @Tag("PIECEWISE_DEADBAND") @Tag("PWDB-SEGMENT-001") @Tag("PWDB-IMMUTABLE-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        String sql;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V088__create_piecewise_deadband.sql")) {
            assertThat(in).as("V088 migration must be on the classpath").isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
        }
        assertThat(sql).contains("segment_start < segment_end AND deadband_width >= 0");
        assertThat(sql).contains("uq_deadband_segment_ordinal");
        assertThat(sql).contains("uq_deadband_evaluation_idempotency");
    }

    // ── P1-64 — the racy evaluation insert crosses a REQUIRES_NEW boundary (poisoned-tx seal) ──
    @Test @Tag("PWDB-IMMUTABLE-001")
    void violation_racyInsertIsolatedInRequiresNewBoundary() throws Exception {
        Method insert = com.ax.template.authblueprint.common.IdempotentInsert.class
            .getMethod("insert", java.util.function.Supplier.class);
        org.springframework.transaction.annotation.Transactional tx =
            insert.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(tx).as("IdempotentInsert.insert must be @Transactional").isNotNull();
        assertThat(tx.propagation())
            .as("the racy insert must run in a REQUIRES_NEW inner tx (25P02 poisoned-tx seal)")
            .isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);
        assertThat(java.util.Arrays.stream(DeadbandService.class.getDeclaredFields())
                .anyMatch(f -> f.getType() == com.ax.template.authblueprint.common.IdempotentInsert.class))
            .as("DeadbandService must delegate its racy insert through IdempotentInsert (revert-proof)")
            .isTrue();
    }
}
