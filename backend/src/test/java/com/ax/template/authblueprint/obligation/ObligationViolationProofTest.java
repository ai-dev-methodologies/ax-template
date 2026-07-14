package com.ax.template.authblueprint.obligation;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

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

    // ── OBL-CONSEQUENCE-001 — exactly-once (UNIQUE) + fully immutable, no public setter ──
    @Test @Tag("OBL-CONSEQUENCE-001")
    void violation_consequenceImmutable_uniquePerObligation_noStoredAccruedColumn() throws Exception {
        for (Method m : BreachConsequence.class.getMethods()) {
            assertThat(m.getName()).as("BreachConsequence must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "obligationId", "recordedAt", "basisAmount", "deadlineAtRecording"}) {
            Column col = BreachConsequence.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("BreachConsequence." + f + " must be immutable").isFalse();
        }
        assertThat(java.util.Arrays.stream(BreachConsequence.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName))
            .as("OBL-INTEREST-ACCRUE-001 — no stored accrued-interest field: it is derive-on-read ONLY")
            .doesNotContain("accruedInterest", "accrued");
        jakarta.persistence.Table table = BreachConsequence.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()).isNotEmpty();
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("obligation_id");

        String sweeper = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "obligation", "ObligationSweeper.java"));
        assertThat(sweeper)
            .as("the sweep must check for an existing consequence before binding a new one (exactly-once)")
            .contains("findConsequence");
    }

    // ── OBL-INTEREST-ACCRUE-001 — exact value, wall-clock-independent (fixed deadline + fixed
    //    "now", both literal Instants — no Instant.now() anywhere in this test). A self-consistency
    //    check (accrued > 0, two reads agree) can pass even with an off-by-one day-count, a wrong
    //    scale, or a wrong divisor; only an EXACT match against an independently hand-computed
    //    BigDecimal (literal rate/divisor, not referencing the entity's own constants) catches those. ──
    @Test @Tag("OBL-INTEREST-ACCRUE-001")
    void violation_accruedInterest_exactValue_noWallClockSensitivity() {
        Instant deadline = Instant.parse("2020-01-01T00:00:00Z");
        Instant now = deadline.plus(java.time.Duration.ofDays(100));   // exactly 100 whole days overdue
        BigDecimal basis = new BigDecimal("10000.0000");

        BreachConsequence c = new BreachConsequence(UUID.randomUUID(), UUID.randomUUID(),
            deadline, basis, deadline);

        // independently computed — literal 8%/365-day divisor, scale 4 HALF_UP, mirroring the
        // documented formula in specs/deadline-obligation-l0.yaml but NOT referencing
        // BreachConsequence.STATUTORY_ANNUAL_RATE, so a drifted constant would be caught, not echoed
        BigDecimal expected = basis.multiply(new BigDecimal("0.08"))
            .multiply(new BigDecimal("100"))
            .divide(new BigDecimal("365"), 4, RoundingMode.HALF_UP);
        assertThat(expected).as("sanity: the hand-derived expected value").isEqualByComparingTo("219.1781");

        assertThat(c.accruedInterest(now))
            .as("OBL-INTEREST-ACCRUE-001 — exact deterministic value: 100 whole days overdue on a "
                + "10000 basis at the documented 8%/yr statutory rate; catches off-by-one day-count, "
                + "wrong scale, and wrong divisor bugs that a mere >0/self-consistency check would miss")
            .isEqualByComparingTo(expected);
    }

    // ── OBL-WAIVER-001/002 — waiver + revocation are fully immutable; 4-eyes enforced in the service ──
    @Test @Tag("OBL-WAIVER-001") @Tag("OBL-WAIVER-002")
    void violation_waiverAndRevocationImmutable_fourEyesEnforced_sweepConsultsValidity() throws Exception {
        for (Method m : ObligationWaiver.class.getMethods()) {
            assertThat(m.getName()).as("ObligationWaiver must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "obligationId", "grantedBy", "obligationOwner", "reason",
                "grantedAt", "grantedAtCycle", "expiresAt", "expiresAfterCycles"}) {
            Column col = ObligationWaiver.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("ObligationWaiver." + f + " must be immutable").isFalse();
        }
        for (Method m : WaiverRevocation.class.getMethods()) {
            assertThat(m.getName()).as("WaiverRevocation must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "waiverId", "obligationId", "revokedBy", "revokedAt"}) {
            Column col = WaiverRevocation.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("WaiverRevocation." + f + " must be immutable").isFalse();
        }
        jakarta.persistence.Table revocationTable = WaiverRevocation.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(revocationTable.uniqueConstraints()[0].columnNames()).containsExactly("waiver_id");

        String service = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "obligation", "ObligationService.java"));
        assertThat(service)
            .as("granting a waiver must reject grantor == declared owner (4-eyes, OBL-WAIVER-002)")
            .contains("waiverSelfGrant");
        assertThat(service)
            .as("revoking must APPEND a WaiverRevocation, never mutate ObligationWaiver")
            .contains("new WaiverRevocation(");

        String sweeper = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "obligation", "ObligationSweeper.java"));
        assertThat(sweeper)
            .as("the sweep must consult waiver validity before binding a consequence (OBL-WAIVER-001)")
            .contains("isValidAt");
    }

    // ── the P3-20/P3-40 migrations carry the same backstops as the entities ──
    @Test @Tag("OBL-CONSEQUENCE-001") @Tag("OBL-WAIVER-001")
    void violation_extensionMigrationsCarryTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(
                "/db/migration/V084__create_obligation_breach_consequences.sql")) {
            assertThat(in).as("V084 migration must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("UNIQUE INDEX uq_obligation_consequence");
            assertThat(sql)
                .as("no accrued-amount COLUMN — interest is derive-on-read only (an explanatory comment "
                    + "mentioning the word is fine; a persisted column named for it is not)")
                .doesNotContainIgnoringCase("accrued_amount")
                .doesNotContainIgnoringCase("accrued_interest");
        }
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V085__create_obligation_waivers.sql")) {
            assertThat(in).as("V085 migration must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("UNIQUE INDEX uq_waiver_revocation");
        }
    }
}
