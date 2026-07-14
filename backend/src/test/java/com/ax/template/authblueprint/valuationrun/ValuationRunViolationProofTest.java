package com.ax.template.authblueprint.valuationrun;

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
 * VIOLATION proof for valuation-run-projection-l0. Structural assertions a deliberate break cannot
 * pass silently: a run is immutable (every column @Column(updatable=false), no public setter,
 * @Version present) with the fan-out conservation @Check (output_sum = total_value) + the
 * uq(subject_id, run_version) backstop; ValuationOutput is append-only immutable; NO delete path
 * exists anywhere in the domain; the subject mutator is package-sealed; the write path uses the
 * PESSIMISTIC_WRITE finder; and the migration carries the same backstops.
 */
@Tag("VALUATIONRUN")
class ValuationRunViolationProofTest {

    // ── VALRUN-IMMUTABLE-001 — the run is immutable: all columns updatable=false, no setter, @Version ──
    @Test @Tag("VALRUN-IMMUTABLE-001")
    void violation_runImmutable_noSetter_versionPresent() throws Exception {
        for (Method m : ValuationRun.class.getMethods()) {
            assertThat(m.getName()).as("ValuationRun must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "subjectId", "runVersion", "asOf", "basis", "totalValue",
                "outputSum", "rebasedFromRunVersion", "sourceRef", "createdAt"}) {
            Column col = ValuationRun.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("ValuationRun." + f + " must be immutable").isFalse();
        }
        // a versioned run carries no @Version on ITSELF (it IS the version) — the lockable head
        // ValuationSubject carries @Version for the optimistic guard.
        assertThat(ValuationSubject.class.getDeclaredField("version").isAnnotationPresent(Version.class))
            .as("ValuationSubject must carry @Version").isTrue();
    }

    // ── VALRUN-FANOUT-001 — the run @Check ties output_sum to total_value; uq(subject,version) ──
    @Test @Tag("VALRUN-FANOUT-001") @Tag("VALRUN-CONCURRENT-001")
    void violation_fanOutCheck_andVersionUnique() {
        Check check = ValuationRun.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).as("the fan-out conservation backstop ties output_sum to total_value")
            .contains("output_sum = total_value");
        assertThat(c).contains("run_version >= 1");

        jakarta.persistence.Table table = ValuationRun.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames())
            .as("uq(subject_id, run_version) — the exactly-once version backstop")
            .containsExactly("subject_id", "run_version");
    }

    // ── VALRUN-FANOUT-001 — ValuationOutput is append-only immutable; uq(run_id, position_ref) ──
    @Test @Tag("VALRUN-FANOUT-001")
    void violation_outputAppendOnly_uniquePerPosition() throws Exception {
        for (Method m : ValuationOutput.class.getMethods()) {
            assertThat(m.getName()).as("ValuationOutput must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "runId", "positionRef", "positionValue"}) {
            Column col = ValuationOutput.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("ValuationOutput." + f + " must be immutable").isFalse();
        }
        jakarta.persistence.Table table = ValuationOutput.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("run_id", "position_ref");
    }

    // ── VALRUN-IMMUTABLE-001 — NO delete path; the subject mutator is package-sealed ──
    @Test @Tag("VALRUN-IMMUTABLE-001")
    void violation_noDeletePath_mutatorSealed() throws Exception {
        for (Class<?> repo : new Class<?>[]{ValuationRunRepository.class, ValuationSubjectRepository.class}) {
            for (Method m : repo.getDeclaredMethods()) {
                assertThat(m.getName()).as(repo.getSimpleName() + " declares no delete method")
                    .doesNotContain("delete");
            }
        }
        for (String src : new String[]{"ValuationRunService", "ValuationRunController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "valuationrun", src + ".java"));
            assertThat(text).as(src + " must contain no delete call — runs are immutable, never removed")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
        // the head pointer mutator is package-private (the service is its sole caller)
        Method advanceHead = java.util.Arrays.stream(ValuationSubject.class.getDeclaredMethods())
            .filter(x -> x.getName().equals("advanceHead")).findFirst().orElseThrow();
        assertThat(Modifier.isPublic(advanceHead.getModifiers()))
            .as("ValuationSubject.advanceHead must be package-private").isFalse();
        // immutable identity columns on the subject
        for (String f : new String[]{"id", "subjectRef", "createdAt"}) {
            Column col = ValuationSubject.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col.updatable()).as("ValuationSubject." + f + " must be immutable").isFalse();
        }
    }

    // ── VALRUN-CONCURRENT-001 — the write path uses the PESSIMISTIC_WRITE finder + observed-head gate ──
    @Test @Tag("VALRUN-CONCURRENT-001")
    void violation_lockedFinder_andSerializedWrite() throws Exception {
        Method locked = ValuationSubjectRepository.class.getMethod("findByIdForUpdate", java.util.UUID.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "valuationrun", "ValuationRunService.java"));
        // recompute(...) is a thin delegate to recomputeForSource(...) (VALRUN-FALLBACK-001) — the
        // lock-taking write core lives in recomputeForSource, which is what must be checked here.
        for (String method : new String[]{"public ValuationRun recomputeForSource(", "public ValuationRun rebase("}) {
            int start = svc.indexOf(method);
            assertThat(start).as(method + " must exist").isPositive();
            String body = svc.substring(start, svc.indexOf("\n    }", start));
            assertThat(body).as(method + " must take the subject row lock").contains("findByIdForUpdate");
        }
        // recompute gates on the observed head version (via recomputeForSource); rebase gates too
        assertThat(svc).as("recompute gates on the observed head version (exactly-one-wins)")
            .contains("getHeadRunVersion() != expectedHeadVersion");
        assertThat(svc).as("rebase gates on the observed head version (linear chain)")
            .contains("getHeadRunVersion() != fromRunVersion");
        // the independent conservation cross-check, NOT a by-construction tautology
        assertThat(svc).as("conservation is derived a second, independent way")
            .contains("runs.sumOutputValues(run.getId())");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("VALRUN-FANOUT-001") @Tag("VALRUN-CONCURRENT-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V052__create_valuation_run.sql")) {
            assertThat(in).as("V052__create_valuation_run.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("output_sum = total_value");
            assertThat(sql).contains("run_version >= 1");
            assertThat(sql).contains("UNIQUE INDEX uq_valuation_subject_version");
            assertThat(sql).contains("(subject_id, run_version)");
            assertThat(sql).contains("UNIQUE INDEX uq_valuation_output_position");
            assertThat(sql).contains("(run_id, position_ref)");
        }
    }

    // ── VALRUN-FALLBACK-001 — sourceRef is immutable; the read tries sources in order, fail-closed ──
    @Test @Tag("VALRUN-FALLBACK-001")
    void violation_sourceRefImmutable_andFallbackTriesInOrderFailClosed() throws Exception {
        Column col = ValuationRun.class.getDeclaredField("sourceRef").getAnnotation(Column.class);
        assertThat(col).as("sourceRef must carry @Column").isNotNull();
        assertThat(col.updatable()).as("ValuationRun.sourceRef must be immutable").isFalse();

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "valuationrun", "ValuationRunService.java"));
        assertThat(svc).as("the fallback read iterates the caller's priority order, not most-recent-across-sources")
            .contains("for (String source : sourcePriority)");
        assertThat(svc).as("no qualifying source in ANY source is fail-closed, never a silent default")
            .contains("noQualifyingSource()");

        try (InputStream in = getClass().getResourceAsStream("/db/migration/V111__extend_valuation_run_source.sql")) {
            assertThat(in).as("V111__extend_valuation_run_source.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(sql).contains("source_ref");
        }
    }
}
