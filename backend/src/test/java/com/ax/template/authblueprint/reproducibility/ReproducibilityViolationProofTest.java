package com.ax.template.authblueprint.reproducibility;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for reproducible-procedure-l0. Structural assertions a deliberate break cannot
 * pass silently: the recorded basis columns (seed/algorithm/input_hash/selected_ids/classifier_
 * version/resolved_class) are immutable; the raw subject is @JsonIgnore and exposed by no public
 * getter; the procedure carries @Version + the @Check + uq backstops; the seeded draw is
 * deterministic (same args → byte-identical selection); NO delete path exists; the migration
 * carries the same backstops.
 */
@Tag("REPRODUCIBILITY")
class ReproducibilityViolationProofTest {

    // ── PROC-DRAW/CLASS-001 — recorded basis columns are immutable; no public setter ──
    @Test @Tag("PROC-DRAW-001") @Tag("PROC-CLASS-001")
    void violation_recordedBasisImmutable_noPublicSetter() throws Exception {
        for (Method m : Procedure.class.getMethods()) {
            assertThat(m.getName()).as("Procedure must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "kind", "inputSetRef", "inputHash", "seed", "algorithm",
                "drawK", "candidates", "selectedIds", "classifierVersion", "resolvedClass",
                "rawSubject", "actor", "createdAt"}) {
            Column col = Procedure.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("Procedure." + f + " must be immutable (recorded basis)").isFalse();
        }
        assertThat(Procedure.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── PROC-BLIND-001 — the raw subject is @JsonIgnore and no public getter leaks it ──
    @Test @Tag("PROC-BLIND-001")
    void violation_rawSubjectIsBlinded_noPublicLeak() throws Exception {
        assertThat(Procedure.class.getDeclaredField("rawSubject").isAnnotationPresent(JsonIgnore.class))
            .as("the raw subject field must be @JsonIgnore").isTrue();
        // there is no PUBLIC getter named getRawSubject/getSubject that would serialize the raw value
        for (Method m : Procedure.class.getMethods()) {
            assertThat(m.getName()).as("no public raw-subject getter may leak the blinded value")
                .isNotEqualTo("getRawSubject").isNotEqualTo("getSubject");
        }
        // the masked projection is deterministic and never reveals length
        assertThat(FieldBlinder.mask("subject-9911")).isEqualTo("s***1");
        assertThat(FieldBlinder.mask("subject-9911")).as("deterministic — same raw → same mask")
            .isEqualTo(FieldBlinder.mask("subject-9911"));
        assertThat(FieldBlinder.mask("ab")).as("a short value is fully starred").isEqualTo("***");
        assertThat(FieldBlinder.mask(null)).isNull();
    }

    // ── PROC-REPLAY-001 — the seeded draw is deterministic: same args → byte-identical selection ──
    @Test @Tag("PROC-REPLAY-001")
    void violation_seededDrawIsDeterministic() {
        List<String> pool = List.of("a", "b", "c", "d", "e", "f", "g", "h");
        long seed = 123456789L;
        assertThat(SeededDraw.select(pool, 3, seed))
            .as("the seeded draw reproduces byte-identically from the recorded seed")
            .isEqualTo(SeededDraw.select(pool, 3, seed));
        // a different seed (overwhelmingly) yields a different selection — the seed is load-bearing
        assertThat(SeededDraw.select(pool, 3, seed))
            .isNotEqualTo(SeededDraw.select(pool, 3, seed + 1));
        // k is clamped to the candidate count
        assertThat(SeededDraw.select(pool, 99, seed)).hasSize(pool.size());
    }

    // ── PROC-CLASS-001 — no delete path; the @Check + uq backstops are present ──
    @Test @Tag("PROC-CLASS-001")
    void violation_noDeletePath_checkAndUniqueBackstops() throws Exception {
        for (Method m : ProcedureRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).as("no delete method on the repository").doesNotContain("delete");
        }
        Check check = Procedure.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("kind <> 'DRAW' OR (seed IS NOT NULL AND algorithm IS NOT NULL AND selected_ids IS NOT NULL)");
        assertThat(c).contains("kind <> 'CLASSIFICATION' OR (classifier_version IS NOT NULL AND resolved_class IS NOT NULL)");

        jakarta.persistence.Table table = Procedure.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames())
            .containsExactly("input_hash", "classifier_version", "kind");
    }

    // ── PROC-CLASS-001 — the classify path takes the PESSIMISTIC_WRITE finder (idempotent under race) ──
    @Test @Tag("PROC-CLASS-001")
    void violation_lockedClassificationFinder() throws Exception {
        Method locked = ProcedureRepository.class.getMethod("findClassificationForUpdate",
            String.class, String.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("PROC-DRAW-001") @Tag("PROC-CLASS-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V050__create_reproducibility.sql")) {
            assertThat(in).as("V050__create_reproducibility.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("kind <> 'DRAW' OR (seed IS NOT NULL AND algorithm IS NOT NULL AND selected_ids IS NOT NULL)");
            assertThat(sql).contains("kind <> 'CLASSIFICATION' OR (classifier_version IS NOT NULL AND resolved_class IS NOT NULL)");
            assertThat(sql).contains("UNIQUE INDEX uq_procedure_class");
            assertThat(sql).contains("(input_hash, classifier_version, kind)");
        }
    }
}
