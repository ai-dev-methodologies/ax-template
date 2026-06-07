package com.ax.template.authblueprint.copresence;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for negative-copresence-gate-l0. The gate is the sole activation path (no public
 * setter on members), KB rows immutable, graded severities present, migration backstops declared.
 */
@Tag("COPRESENCE")
class CopresenceViolationProofTest {

    // ── GATE-SET-EVAL-001 — a member is activated only via the gate (no public setter / bypass write) ──
    @Test @Tag("GATE-SET-EVAL-001")
    void violation_memberNoPublicSetter_immutableCoreFields_versioned() throws Exception {
        for (Method m : SubjectMember.class.getMethods()) {
            assertThat(m.getName())
                .as("SubjectMember must expose no public setter (activation only via the gate)")
                .doesNotStartWith("set");
        }
        for (String f : new String[]{"subjectId", "concept", "label", "createdAt",
                "overrideReason", "overriddenFindings"}) {
            Column col = SubjectMember.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("SubjectMember." + f + " must be immutable").isFalse();
        }
        assertThat(SubjectMember.class.getDeclaredField("version").isAnnotationPresent(Version.class))
            .as("SubjectMember.version must carry @Version").isTrue();
        for (Method m : Subject.class.getMethods()) {
            assertThat(m.getName()).as("Subject must have no public setter").doesNotStartWith("set");
        }
        for (Method m : ConflictRule.class.getMethods()) {
            assertThat(m.getName()).as("ConflictRule (KB) must be immutable — no public setter").doesNotStartWith("set");
        }
    }

    // ── GATE-GRADED-001 — exactly the two graded severities; member lifecycle is ACTIVE/REMOVED ──
    @Test @Tag("GATE-GRADED-001")
    void violation_gradedSeverities() {
        assertThat(ConflictSeverity.values()).containsExactly(ConflictSeverity.ABSOLUTE, ConflictSeverity.RELATIVE);
        assertThat(MemberStatus.values()).containsExactly(MemberStatus.ACTIVE, MemberStatus.REMOVED);
    }

    // ── GATE-FAILCLOSED-001 — migration declares the KB vocabulary + conflict-pair backstops ──
    @Test @Tag("GATE-FAILCLOSED-001")
    void violation_migrationDeclaresKbBackstops() throws Exception {
        String sql;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V041__create_copresence.sql")) {
            assertThat(in).as("V041 migration must be on the classpath").isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(sql).contains("copresence_known_concepts");
        assertThat(sql).contains("uq_copresence_known_concept");
        assertThat(sql).contains("chk_copresence_conflict_distinct");
        assertThat(sql).contains("concept_a <> concept_b");
    }
}
