package com.ax.template.authblueprint.obligation;

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
 * VIOLATION proof for deadline-obligation-l0. Structural assertions a deliberate break cannot
 * pass silently: there is NO auto-expire path (no EXPIRED state, the sweeper writes no terminal),
 * escalation events and derivations are fully append-only with the exactly-once UNIQUE backstop,
 * the terminal/deadline mutators are package-sealed, all write paths use the PESSIMISTIC_WRITE
 * finder, and the migration carries the same backstops — no Spring context.
 */
@Tag("OBLIGATION")
class ObligationViolationProofTest {

    // ── OBL-ACK-001 — NO auto-expire path exists: no EXPIRED state; sweeper writes no terminal ──
    @Test @Tag("OBL-ACK-001")
    void violation_noAutoExpirePathExists() throws Exception {
        assertThat(ObligationStatus.values())
            .as("the lifecycle deliberately has NO expired/cancelled state")
            .containsExactly(ObligationStatus.OPEN, ObligationStatus.ACKNOWLEDGED);

        String sweeper = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "obligation", "ObligationSweeper.java"));
        assertThat(sweeper)
            .as("the sweep must never write the terminal (only a human closes the loop)")
            .doesNotContain("acknowledge(")
            .doesNotContain("ACKNOWLEDGED;");
        // the only terminal writer is the service's ack path
        String service = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "obligation", "ObligationService.java"));
        assertThat(service.split("\\.acknowledge\\(").length - 1)
            .as("exactly one call site writes the terminal — the explicit ack path")
            .isEqualTo(1);
    }

    // ── OBL-LADDER-001 — events append-only + exactly-once UNIQUE backstop ──
    @Test @Tag("OBL-LADDER-001")
    void violation_escalationAppendOnly_uniquePerRung() throws Exception {
        for (Method m : EscalationEvent.class.getMethods()) {
            assertThat(m.getName()).as("EscalationEvent must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "obligationId", "rung", "firedAt", "deadlineAtFiring"}) {
            Column col = EscalationEvent.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("EscalationEvent." + f + " must be immutable").isFalse();
        }
        jakarta.persistence.Table table = EscalationEvent.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()).isNotEmpty();
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("obligation_id", "rung");
        assertThat(EscalationRung.LADDER)
            .as("the ladder is ordered APPROACH → IMMINENT → BREACH")
            .containsExactly(EscalationRung.APPROACH, EscalationRung.IMMINENT, EscalationRung.BREACH);
    }

    // ── OBL-GROUND-001 — derivations fully append-only; controller accepts no raw deadline ──
    @Test @Tag("OBL-GROUND-001")
    void violation_derivationsAppendOnly_noRawDeadlineField() throws Exception {
        for (Method m : DerivationRecord.class.getMethods()) {
            assertThat(m.getName()).as("DerivationRecord must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "obligationId", "axisId", "candidateDeadline", "formula", "derivedAt"}) {
            Column col = DerivationRecord.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("DerivationRecord." + f + " must be immutable").isFalse();
        }
        String controller = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "obligation", "ObligationController.java"));
        assertThat(controller.substring(controller.indexOf("record CreateReq"),
                controller.indexOf("record UsageReq")))
            .as("the create contract must carry NO raw deadline field — deadlines are derived")
            .doesNotContainIgnoringCase("deadline");
    }

    // ── OBL-ACK-001 / OBL-CONCURRENT-001 — mutators package-sealed; write paths use the locked finder ──
    @Test @Tag("OBL-ACK-001") @Tag("OBL-CONCURRENT-001")
    void violation_mutatorsSealed_andLockedFindersUsed() throws Exception {
        for (Method m : Obligation.class.getMethods()) {
            assertThat(m.getName()).as("Obligation must expose no public setter").doesNotStartWith("set");
        }
        for (String hook : new String[]{"reevaluate", "acknowledge"}) {
            Method m = java.util.Arrays.stream(Obligation.class.getDeclaredMethods())
                .filter(x -> x.getName().equals(hook)).findFirst().orElseThrow();
            assertThat(Modifier.isPublic(m.getModifiers()))
                .as("Obligation." + hook + " must be package-private (service is the sole mutator)")
                .isFalse();
        }
        assertThat(Obligation.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();

        Check check = Obligation.class.getAnnotation(Check.class);
        assertThat(check.constraints().replaceAll("\\s+", " "))
            .contains("status <> 'ACKNOWLEDGED' OR (ack_by IS NOT NULL AND ack_at IS NOT NULL)");

        String service = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "obligation", "ObligationService.java"));
        for (String method : new String[]{"public Obligation advanceUsage(", "public Obligation acknowledge("}) {
            int start = service.indexOf(method);
            assertThat(start).as(method + " must exist").isPositive();
            String body = service.substring(start, service.indexOf("\n    }", start));
            assertThat(body).as(method + " must use the PESSIMISTIC_WRITE finder")
                .contains("findByObligationKeyForUpdate");
        }
        String sweeper = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "obligation", "ObligationSweeper.java"));
        assertThat(sweeper).as("the sweep is a concurrent mutator — it locks like the API paths")
            .contains("findByIdForUpdate");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("OBL-LADDER-001") @Tag("OBL-ACK-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V044__create_deadline_obligations.sql")) {
            assertThat(in).as("V044__create_deadline_obligations.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("status <> 'ACKNOWLEDGED' OR (ack_by IS NOT NULL AND ack_at IS NOT NULL)");
            assertThat(sql).contains("UNIQUE INDEX uq_obligation_rung");
            assertThat(sql).doesNotContain("'EXPIRED'");
        }
    }
}
