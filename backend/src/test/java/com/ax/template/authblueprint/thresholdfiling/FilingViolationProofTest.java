package com.ax.template.authblueprint.thresholdfiling;

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
 * VIOLATION proof for threshold-filing-obligation-l0. Structural assertions a deliberate break
 * cannot pass silently: TRIGGERED is irreversible (no un-trigger, no EXPIRED state anywhere), the
 * bound filing record is fully immutable except its own ack lifecycle, exactly one call site
 * writes each terminal, both write-paths use the PESSIMISTIC_WRITE finder, and the migration
 * carries the same backstops — no Spring context.
 */
@Tag("THRESHOLD_FILING")
class FilingViolationProofTest {

    // ── TFO-TRIGGER-001 — the register carries the @Check implication; no public setter; sealed ──
    @Test @Tag("TFO-TRIGGER-001")
    void violation_registerCarriesCheckImplication_noPublicSetter_sealed() throws Exception {
        Check check = FilingRegister.class.getAnnotation(Check.class);
        assertThat(check).as("FilingRegister must carry @Check (TFO-TRIGGER-001)").isNotNull();
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("accrued_value < threshold_value OR status = 'TRIGGERED'");
        assertThat(c).contains("threshold_value > 0");

        for (Method m : FilingRegister.class.getMethods()) {
            assertThat(m.getName())
                .as("FilingRegister must expose no public setter (state machine + service are sole mutators)")
                .doesNotStartWith("set");
        }
        for (String hook : new String[]{"markTriggered", "advanceAccrual"}) {
            Method m = java.util.Arrays.stream(FilingRegister.class.getDeclaredMethods())
                .filter(x -> x.getName().equals(hook)).findFirst().orElseThrow();
            assertThat(Modifier.isPublic(m.getModifiers()))
                .as("FilingRegister." + hook + " must be package-private (sole-mutator seam)")
                .isFalse();
        }
        for (String f : new String[]{"id", "subjectKey", "threshold", "createdAt"}) {
            Column col = FilingRegister.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("FilingRegister." + f + " must be immutable").isFalse();
        }
        assertThat(FilingRegister.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── TFO-TRIGGER-001 — TRIGGERED is irreversible: zero outgoing edges, no re-trigger ──
    @Test @Tag("TFO-TRIGGER-001")
    void violation_stateMachineHasNoEdgeOutOfTriggered_noReTriggerPath() throws Exception {
        for (Method m : FilingRegisterStateMachine.class.getDeclaredMethods()) {
            if (m.isSynthetic()) {
                continue;
            }
            assertThat(m.getName())
                .as("the ONLY transition is the one-way crossing edge — no un-trigger/reset")
                .isEqualTo("trigger");
        }
        assertThat(FilingRegisterStatus.values()).containsExactly(FilingRegisterStatus.ACTIVE,
            FilingRegisterStatus.TRIGGERED);

        String service = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "thresholdfiling", "FilingService.java"));
        assertThat(service.split("stateMachine\\.trigger\\(").length - 1)
            .as("exactly one call site drives TRIGGERED — the crossing-accrual path")
            .isEqualTo(1);
    }

    // ── TFO-FILING-RECORD-001 — the bound record is immutable except its own ack lifecycle ──
    @Test @Tag("TFO-FILING-RECORD-001")
    void violation_filingRecordImmutable_exceptAckLifecycle_uniquePerRegister() throws Exception {
        for (Method m : FilingObligation.class.getMethods()) {
            assertThat(m.getName()).as("FilingObligation must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "registerId", "subjectKey", "thresholdSnapshot",
                "triggerInstant", "dueAt"}) {
            Column col = FilingObligation.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("FilingObligation." + f + " must be immutable").isFalse();
        }
        // status/ack_by/ack_at are the ONLY mutable columns — the ack lifecycle
        for (String f : new String[]{"status", "ackBy", "ackAt"}) {
            Column col = FilingObligation.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col.updatable())
                .as("FilingObligation." + f + " is the ack lifecycle — it MUST remain mutable")
                .isTrue();
        }
        Method ack = java.util.Arrays.stream(FilingObligation.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("acknowledge")).findFirst().orElseThrow();
        assertThat(Modifier.isPublic(ack.getModifiers()))
            .as("FilingObligation.acknowledge must be package-private (service is the sole mutator)")
            .isFalse();

        Check check = FilingObligation.class.getAnnotation(Check.class);
        assertThat(check.constraints().replaceAll("\\s+", " "))
            .contains("status <> 'ACKNOWLEDGED' OR (ack_by IS NOT NULL AND ack_at IS NOT NULL)");

        jakarta.persistence.Table table = FilingObligation.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()).isNotEmpty();
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("register_id");

        assertThat(FilingObligationStatus.values())
            .as("the lifecycle deliberately has NO expired/cancelled state — ack is the only terminal")
            .containsExactly(FilingObligationStatus.OPEN, FilingObligationStatus.ACKNOWLEDGED);
    }

    // ── TFO-DEADLINE-001 — exactly one terminal writer; both write-paths use the locked finder ──
    @Test @Tag("TFO-DEADLINE-001")
    void violation_oneAckCallSite_bothWritePathsUseTheLockedFinder() throws Exception {
        String service = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "thresholdfiling", "FilingService.java"));
        assertThat(service.split("\\.acknowledge\\(").length - 1)
            .as("exactly one call site writes the terminal — the explicit ack path")
            .isEqualTo(1);

        Method locked = FilingRegisterRepository.class.getMethod("findBySubjectKeyForUpdate", String.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).as("the serialization point must be PESSIMISTIC_WRITE").isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        for (String method : new String[]{"public FilingRegister accrue(", "public FilingObligation acknowledge("}) {
            int start = service.indexOf(method);
            assertThat(start).as(method + " must exist in FilingService").isPositive();
            String body = service.substring(start, service.indexOf("\n    }", start));
            assertThat(body)
                .as(method + " must read the row via the PESSIMISTIC_WRITE finder")
                .contains("findBySubjectKeyForUpdate");
        }
    }

    // ── the migration carries the same backstops as the entities ──
    @Test @Tag("TFO-TRIGGER-001") @Tag("TFO-DEADLINE-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V086__create_filing_registers.sql")) {
            assertThat(in).as("V086__create_filing_registers.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("accrued_value < threshold_value OR status = 'TRIGGERED'");
            assertThat(sql).contains("status <> 'ACKNOWLEDGED' OR (ack_by IS NOT NULL AND ack_at IS NOT NULL)");
            assertThat(sql).contains("UNIQUE INDEX uq_filing_obligation_register");
            assertThat(sql).doesNotContain("'EXPIRED'");
        }
    }
}
