package com.ax.template.authblueprint.variancegate;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for variance-tolerance-band-l0. Structural assertions a deliberate break cannot
 * pass silently: the variance + standard + actual + band columns are immutable (a hand-entered or
 * rewritten variance is unrepresentable), the appraisal carries @Version + the @Check backstops
 * (variance = actual − standard, non-negative band, disposed ⇒ breach), the disposition is
 * append-only one-per-appraisal (uq(appraisal_id) + @Column(updatable=false)), NO delete path
 * exists anywhere in the domain, mutators are package-sealed, the dispose path uses the
 * PESSIMISTIC_WRITE finder and never rewrites the verdict, and the migration carries the same
 * backstops.
 */
@Tag("VARIANCEGATE")
class VarianceViolationProofTest {

    // ── VG-DERIVE-001 — the variance + standard + actual + band are immutable on the appraisal ──
    @Test @Tag("VG-DERIVE-001")
    void violation_basisAndBandImmutable_noPublicSetter() throws Exception {
        for (Method m : VarianceAppraisal.class.getMethods()) {
            assertThat(m.getName()).as("VarianceAppraisal must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "subject", "standardValue", "actualValue", "variance",
                "lowerTolerance", "upperTolerance", "verdict", "createdAt"}) {
            Column col = VarianceAppraisal.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("VarianceAppraisal." + f + " must be immutable").isFalse();
        }
        // only the disposed flag is mutable (it flips once, never the basis or verdict)
        Column disposed = VarianceAppraisal.class.getDeclaredField("disposed").getAnnotation(Column.class);
        assertThat(disposed.updatable()).as("only the disposed flag is mutable").isTrue();
        assertThat(VarianceAppraisal.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── VG-DISPOSE-001 — dispositions append-only, one per appraisal ──
    @Test @Tag("VG-DISPOSE-001")
    void violation_dispositionsAppendOnly_uniquePerAppraisal() throws Exception {
        for (Method m : VarianceDisposition.class.getMethods()) {
            assertThat(m.getName()).as("VarianceDisposition must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "appraisalId", "decision", "actor", "reason", "decidedAt"}) {
            Column col = VarianceDisposition.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("VarianceDisposition." + f + " must be immutable").isFalse();
        }
        jakarta.persistence.Table table = VarianceDisposition.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("appraisal_id");
    }

    // ── VG-GATE-001 — the asymmetric gate is two independent bounds; @Check backstops present ──
    @Test @Tag("VG-GATE-001")
    void violation_checkBackstops_andMutatorsSealed() throws Exception {
        Check check = VarianceAppraisal.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("lower_tolerance >= 0 AND upper_tolerance >= 0");
        assertThat(c).contains("variance = actual_value - standard_value");
        assertThat(c).contains("disposed = FALSE OR verdict = 'OUT_OF_TOLERANCE'");

        // the sole-mutator hook is package-private (the disposed flag flips only through it)
        Method hook = java.util.Arrays.stream(VarianceAppraisal.class.getDeclaredMethods())
            .filter(x -> x.getName().equals("markDisposed")).findFirst().orElseThrow();
        assertThat(Modifier.isPublic(hook.getModifiers()))
            .as("VarianceAppraisal.markDisposed must be package-private").isFalse();
    }

    // ── VG-BLOCK/DISPOSE-001 — NO delete path; the dispose path never rewrites the verdict ──
    @Test @Tag("VG-BLOCK-001") @Tag("VG-DISPOSE-001")
    void violation_noDeletePath_verdictNeverRewritten() throws Exception {
        for (Method m : VarianceAppraisalRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"VarianceService", "VarianceController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "variancegate", src + ".java"));
            assertThat(text).as(src + " must contain no delete call — appraisals are records, never removed")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
        // the verdict column is updatable=false — a disposition cannot rewrite OUT_OF_TOLERANCE away
        Column verdict = VarianceAppraisal.class.getDeclaredField("verdict").getAnnotation(Column.class);
        assertThat(verdict.updatable()).as("the verdict is immutable — a disposition never erases the breach").isFalse();
    }

    // ── VG-CONCURRENT-001 — the dispose path uses the PESSIMISTIC_WRITE finder ──
    @Test @Tag("VG-CONCURRENT-001")
    void violation_lockedFinder_andSerializedDispose() throws Exception {
        Method locked = VarianceAppraisalRepository.class.getMethod("findByIdForUpdate", java.util.UUID.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "variancegate", "VarianceService.java"));
        int start = svc.indexOf("public VarianceAppraisal dispose(");
        assertThat(start).as("dispose() must exist").isPositive();
        String body = svc.substring(start, svc.indexOf("\n    }", start));
        assertThat(body).as("dispose() must take the appraisal row lock").contains("findByIdForUpdate");
        assertThat(body).as("dispose() persists the disposition through the member writer").contains("members.persistAndFlush");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("VG-DERIVE-001") @Tag("VG-DISPOSE-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V061__create_variancegate.sql")) {
            assertThat(in).as("V061__create_variancegate.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("lower_tolerance >= 0 AND upper_tolerance >= 0");
            assertThat(sql).contains("variance = actual_value - standard_value");
            assertThat(sql).contains("disposed = FALSE OR verdict = 'OUT_OF_TOLERANCE'");
            assertThat(sql).contains("UNIQUE INDEX uq_variance_appraisal");
            assertThat(sql).contains("(appraisal_id)");
        }
    }
}
