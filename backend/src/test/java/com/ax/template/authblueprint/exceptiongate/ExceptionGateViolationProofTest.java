package com.ax.template.authblueprint.exceptiongate;

import jakarta.persistence.Column;
import jakarta.persistence.LockModeType;

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
 * VIOLATION proof for orthogonal-exception-gate-l0. Structural assertions a deliberate break
 * cannot pass silently: the raise/lift/advancePrimary mutators are three DISTINCT
 * package-private hooks (none calling the others — the compile-time proof of independence),
 * identity columns are immutable, the audit ledger is fully append-only, the row-lock read is
 * PESSIMISTIC_WRITE, and NO delete path exists anywhere in the domain.
 */
class ExceptionGateViolationProofTest {

    // ── EXC-DIM-INDEPENDENT-001 — three distinct mutators; none calls another ──
    @Test @Tag("EXCEPTIONGATE") @Tag("EXC-DIM-INDEPENDENT-001")
    void violation_threeDistinctMutators_noCrossCalls() throws Exception {
        for (String hook : new String[]{"raise", "lift", "advancePrimary"}) {
            Method m = Arrays.stream(ExceptionGate.class.getDeclaredMethods())
                .filter(x -> x.getName().equals(hook)).findFirst().orElseThrow();
            assertThat(Modifier.isPublic(m.getModifiers()))
                .as("ExceptionGate." + hook + " must be package-private").isFalse();
        }
        String src = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "exceptiongate", "ExceptionGate.java"));
        String raiseBody = methodBody(src, "void raise(");
        String liftBody = methodBody(src, "void lift(");
        String advanceBody = methodBody(src, "void advancePrimary(");
        assertThat(raiseBody).as("raise must not touch primaryState").doesNotContain("primaryState");
        assertThat(liftBody).as("lift must not touch primaryState").doesNotContain("primaryState");
        assertThat(advanceBody).as("advancePrimary must not touch raised/reason")
            .doesNotContain("this.raised").doesNotContain("this.reason");

        for (String f : new String[]{"id", "subjectType", "subjectId", "createdAt"}) {
            Column col = ExceptionGate.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("ExceptionGate." + f + " must be immutable").isFalse();
        }
        assertThat(ExceptionGate.class.getDeclaredField("version")
            .isAnnotationPresent(jakarta.persistence.Version.class)).isTrue();
    }

    private static String methodBody(String src, String signaturePrefix) {
        int start = src.indexOf(signaturePrefix);
        assertThat(start).as(signaturePrefix + " must exist").isPositive();
        int end = src.indexOf("\n    }", start);
        return src.substring(start, end);
    }

    // ── EXC-DIM-LIFT-001 — the audit ledger is fully append-only ──
    @Test @Tag("EXCEPTIONGATE") @Tag("EXC-DIM-LIFT-001")
    void violation_auditLedgerFullyImmutable() throws Exception {
        for (Method m : ExceptionAuditEntry.class.getMethods()) {
            assertThat(m.getName()).as("ExceptionAuditEntry must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"gateId", "action", "reason", "actor", "occurredAt"}) {
            Column col = ExceptionAuditEntry.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("ExceptionAuditEntry." + f + " must be immutable").isFalse();
        }
    }

    // ── EXC-DIM-BLOCK-001 — the row-lock finder is PESSIMISTIC_WRITE ──
    @Test @Tag("EXCEPTIONGATE") @Tag("EXC-DIM-BLOCK-001")
    void violation_lockedFinderIsPessimisticWrite() throws Exception {
        Method locked = ExceptionGateRepository.class.getMethod("findBySubjectTypeAndSubjectIdForUpdate",
            String.class, String.class);
        Lock lock = locked.getAnnotation(Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    // ── no delete path anywhere — the gate and its audit trail are never erased ──
    @Test @Tag("EXCEPTIONGATE") @Tag("EXC-DIM-LIFT-001")
    void violation_noDeletePath() throws Exception {
        for (Method m : ExceptionGateRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"ExceptionGateService", "ExceptionGateController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "exceptiongate", src + ".java"));
            assertThat(text).as(src + " must contain no delete call")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
    }

    // ── the migration carries the same unique backstop ──
    @Test @Tag("EXCEPTIONGATE") @Tag("EXC-DIM-INDEPENDENT-001")
    void violation_migrationCarriesTheSameBackstop() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V099__create_exception_gate.sql")) {
            assertThat(in).as("V099__create_exception_gate.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("UNIQUE INDEX uq_exception_gate_subject");
        }
    }
}
