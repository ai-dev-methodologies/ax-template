package com.ax.template.authblueprint.provisionalattestation;

import jakarta.persistence.Column;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for provisional-attestation-l0. Structural assertions a deliberate break cannot
 * pass silently: the identity/authored-by columns are immutable, there is no public setter, the
 * state machine is the sole caller of the entity's status mutator, the DB @Check backstops
 * PATT-DISTINCT-002 even against a direct write, NO delete path exists anywhere in the domain,
 * and the migration carries the same @Check.
 */
@Tag("PROVISIONALATTESTATION")
class ProvisionalAttestationViolationProofTest {

    // ── PATT-LIFECYCLE-001 — no public setter; the entity's mutators are package-private ──
    @Test @Tag("PATT-LIFECYCLE-001")
    void violation_noPublicSetter_mutatorsPackagePrivate() throws Exception {
        for (Method m : ProvisionalRecord.class.getMethods()) {
            assertThat(m.getName()).as("ProvisionalRecord must have no public setter").doesNotStartWith("set");
        }
        for (String mutator : new String[]{"editContent", "markAttested"}) {
            Method m = java.util.Arrays.stream(ProvisionalRecord.class.getDeclaredMethods())
                .filter(x -> x.getName().equals(mutator)).findFirst().orElseThrow();
            assertThat(Modifier.isPublic(m.getModifiers()))
                .as("ProvisionalRecord." + mutator + " must NOT be public").isFalse();
        }
    }

    // ── PATT-LIFECYCLE-001 — identity columns immutable ──
    @Test @Tag("PATT-LIFECYCLE-001")
    void violation_identityColumnsImmutable() throws Exception {
        for (String f : new String[]{"id", "authoredBy", "createdAt"}) {
            Column col = ProvisionalRecord.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("ProvisionalRecord." + f + " must be immutable").isFalse();
        }
    }

    // ── PATT-DISTINCT-002 — the DB @Check backstops self-attestation even against a direct write ──
    @Test @Tag("PATT-DISTINCT-002")
    void violation_dbCheckBackstopsSelfAttestation() {
        Check check = ProvisionalRecord.class.getAnnotation(Check.class);
        assertThat(check).as("ProvisionalRecord must carry a @Check constraint").isNotNull();
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).as("the DB backstop ties attested_by to differ from authored_by")
            .contains("attested_by").contains("authored_by").contains("<>");
    }

    // ── PATT-LIFECYCLE-001 — the state machine is the SOLE caller of markAttested ──
    @Test @Tag("PATT-LIFECYCLE-001")
    void violation_stateMachineIsSoleMutatorCaller() throws Exception {
        String stateMachine = java.nio.file.Files.readString(java.nio.file.Path.of(
            System.getProperty("user.dir"), "src", "main", "java", "com", "ax", "template",
            "authblueprint", "provisionalattestation", "ProvisionalRecordStateMachine.java"));
        assertThat(stateMachine).as("the state machine calls markAttested").contains("record.markAttested(");

        String service = java.nio.file.Files.readString(java.nio.file.Path.of(
            System.getProperty("user.dir"), "src", "main", "java", "com", "ax", "template",
            "authblueprint", "provisionalattestation", "ProvisionalRecordService.java"));
        assertThat(service).as("the service NEVER calls markAttested directly — only via the state machine")
            .doesNotContain(".markAttested(");
        assertThat(service).as("PATT-DISTINCT-002 is checked BEFORE the state machine is invoked")
            .contains("attestorMustDifferFromAuthor");
    }

    // ── NO delete path exists anywhere in the domain ──
    @Test @Tag("PATT-LIFECYCLE-001")
    void violation_noDeletePath() throws Exception {
        for (Method m : ProvisionalRecordRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).as("ProvisionalRecordRepository declares no delete method")
                .doesNotContain("delete");
        }
        for (String src : new String[]{"ProvisionalRecordService", "ProvisionalRecordController"}) {
            String text = java.nio.file.Files.readString(java.nio.file.Path.of(
                System.getProperty("user.dir"), "src", "main", "java", "com", "ax", "template",
                "authblueprint", "provisionalattestation", src + ".java"));
            assertThat(text).as(src + " must contain no delete call")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
    }

    // ── the migration carries the same @Check backstop ──
    @Test @Tag("PATT-DISTINCT-002")
    void violation_migrationCarriesTheSameCheck() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(
                "/db/migration/V109__create_provisional_attestation.sql")) {
            assertThat(in).as("V109__create_provisional_attestation.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("attested_by <> authored_by");
        }
    }
}
