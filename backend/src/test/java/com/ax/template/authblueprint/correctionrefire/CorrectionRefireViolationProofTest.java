package com.ax.template.authblueprint.correctionrefire;

import jakarta.persistence.Column;

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
 * VIOLATION proof for correction-refire-l0. Structural assertions a deliberate break cannot pass
 * silently: a published version is fully immutable (every column @Column(updatable=false), no
 * public setter) with uq(subject_ref, version); NO column anywhere stores a "current version"
 * pointer (CRF-CHAIN-004 — current is always derived); NO delete path exists anywhere in the
 * domain; the ack row's mutator is package-sealed; and the migration carries the same backstops.
 */
@Tag("CORRECTIONREFIRE")
class CorrectionRefireViolationProofTest {

    // ── CRF-SUPERSEDE-001 — the published version is immutable, no setter, uq(subject,version) ──
    @Test @Tag("CRF-SUPERSEDE-001")
    void violation_recordImmutable_noSetter_uniqueVersion() throws Exception {
        for (Method m : CorrectedRecord.class.getMethods()) {
            assertThat(m.getName()).as("CorrectedRecord must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "subjectRef", "version", "content", "contentHash",
                "correctsVersion", "publishedAt"}) {
            Column col = CorrectedRecord.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("CorrectedRecord." + f + " must be immutable").isFalse();
        }
        jakarta.persistence.Table table = CorrectedRecord.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames())
            .as("uq(subject_ref, version) — the exactly-once version backstop")
            .containsExactly("subject_ref", "version");
    }

    // ── CRF-CHAIN-004 — no field anywhere stores a "current version" pointer ──
    @Test @Tag("CRF-CHAIN-004")
    void violation_noStoredCurrentVersionPointer() throws Exception {
        for (var f : CorrectedRecord.class.getDeclaredFields()) {
            String name = f.getName().toLowerCase(java.util.Locale.ROOT);
            assertThat(name).as("CorrectedRecord must have no 'current'-named field — current is derived")
                .doesNotContain("current").doesNotContain("head").doesNotContain("latest");
        }
        String repo = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "correctionrefire", "CorrectedRecordRepository.java"));
        assertThat(repo).as("current is resolved by MAX(version) — a derived read, not a stored column")
            .contains("findTopBySubjectRefOrderByVersionDesc");

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "correctionrefire", "CorrectionRefireService.java"));
        assertThat(svc).as("publish computes nextVersion from the derived current, not a stored pointer")
            .contains("current.map(r -> r.getVersion() + 1)");
    }

    // ── AckRecord is a 1:1 companion row, sole-mutator sealed, uq(record_id) ──
    @Test @Tag("CRF-REFIRE-002")
    void violation_ackRecordSoleMutatorSealed_uniquePerRecord() throws Exception {
        Method close = java.util.Arrays.stream(AckRecord.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("close")).findFirst().orElseThrow();
        assertThat(Modifier.isPublic(close.getModifiers()))
            .as("AckRecord.close must NOT be public — only the service calls it").isFalse();
        for (Method m : AckRecord.class.getMethods()) {
            assertThat(m.getName()).as("AckRecord must have no public setter").doesNotStartWith("set");
        }
        jakarta.persistence.Table table = AckRecord.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames())
            .as("uq(record_id) — exactly one ack row per published version")
            .containsExactly("record_id");

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "correctionrefire", "CorrectionRefireService.java"));
        assertThat(svc).as("every publish creates a NEW pending ack, independent of the prior version's state")
            .contains("AckRecord.pending(");
    }

    // ── NO delete path exists anywhere in the domain ──
    @Test @Tag("CRF-SUPERSEDE-001")
    void violation_noDeletePath() throws Exception {
        for (Method m : CorrectedRecordRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).as("CorrectedRecordRepository declares no delete method")
                .doesNotContain("delete");
        }
        for (String src : new String[]{"CorrectionRefireService", "CorrectionRefireController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "correctionrefire", src + ".java"));
            assertThat(text).as(src + " must contain no delete call")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("CRF-SUPERSEDE-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V110__create_correction_refire.sql")) {
            assertThat(in).as("V110__create_correction_refire.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("UNIQUE INDEX uq_corrected_record_subject_version");
            assertThat(sql).contains("(subject_ref, version)");
            assertThat(sql).contains("UNIQUE INDEX uq_ack_record_id");
            assertThat(sql).contains("(record_id)");
        }
    }
}
