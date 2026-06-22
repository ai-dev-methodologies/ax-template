package com.ax.template.authblueprint.settlement;

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
 * VIOLATION proof for settlement-finality-l0. Structural assertions a deliberate break cannot
 * pass silently: partial settlement is unrepresentable (@Check delivery_settled = payment_settled
 * + SETTLED implies both legs + a recorded final instant); the conserved net obligation and the
 * whole novation record are append-only (@Column(updatable=false)); NO delete path exists; status
 * mutators are package-sealed (only the state machine + service drive them through hooks); write
 * paths use the PESSIMISTIC_WRITE finder; and the migration carries the same backstops.
 */
@Tag("SETTLEMENT")
class SettlementViolationProofTest {

    // ── SETTLE-DVP/FINAL-001 — partial unrepresentable; finality @Check-bounded ──
    @Test @Tag("SETTLE-DVP-001") @Tag("SETTLE-FINAL-001")
    void violation_dvpAtomic_finalityBounded() throws Exception {
        Check check = SettlementInstruction.class.getAnnotation(Check.class);
        assertThat(check).as("SettlementInstruction must carry a @Check").isNotNull();
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).as("DvP — both legs or neither").contains("delivery_settled = payment_settled");
        assertThat(c).as("finality implies both legs + recorded instant")
            .contains("status <> 'SETTLED' OR (delivery_settled = TRUE AND final_at IS NOT NULL)");
        assertThat(c).as("a non-final instruction carries no finality instant")
            .contains("status = 'SETTLED' OR final_at IS NULL");
        // net_obligation is immutable so novation cannot drift the conserved amount
        Column netObl = SettlementInstruction.class.getDeclaredField("netObligation").getAnnotation(Column.class);
        assertThat(netObl.updatable()).as("net_obligation must be immutable (conservation)").isFalse();
        // optimistic lock present
        assertThat(SettlementInstruction.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── SETTLE-NOVATE-001 — novation rows append-only; @Check substitution backstop ──
    @Test @Tag("SETTLE-NOVATE-001")
    void violation_novationAppendOnly_substitutionBounded() throws Exception {
        for (Method m : NovationRecord.class.getMethods()) {
            assertThat(m.getName()).as("NovationRecord must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "instructionId", "leg", "releasedParty", "assumingParty",
                "assumedObligation", "novatedBy", "novatedAt"}) {
            Column col = NovationRecord.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("NovationRecord." + f + " must be immutable").isFalse();
        }
        Check check = NovationRecord.class.getAnnotation(Check.class);
        assertThat(check.constraints().replaceAll("\\s+", " "))
            .contains("released_party <> assuming_party AND assumed_obligation >= 0");
    }

    // ── SETTLE-DVP/FINAL/LADDER-001 — NO delete path; status mutators sealed ──
    @Test @Tag("SETTLE-DVP-001") @Tag("SETTLE-LADDER-001")
    void violation_noDeletePath_mutatorsSealed() throws Exception {
        for (Method m : SettlementInstructionRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"SettlementService", "SettlementController", "SettlementFailLadder"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "settlement", src + ".java"));
            assertThat(text).as(src + " must contain no delete call — settlements are never removed")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
        // the instruction's lifecycle hooks must be package-private (sole-mutator discipline)
        for (String hook : new String[]{"settleBothLegs", "moveStatus", "replaceDeliveryParty", "replacePaymentParty"}) {
            Method m = java.util.Arrays.stream(SettlementInstruction.class.getDeclaredMethods())
                .filter(x -> x.getName().equals(hook)).findFirst().orElseThrow();
            assertThat(Modifier.isPublic(m.getModifiers()))
                .as("SettlementInstruction." + hook + " must be package-private").isFalse();
        }
    }

    // ── SETTLE-CONCURRENT-001 — write paths use the locked finder ──
    @Test @Tag("SETTLE-CONCURRENT-001")
    void violation_lockedFinder_onEveryWritePath() throws Exception {
        Method locked = SettlementInstructionRepository.class.getMethod("findByIdForUpdate", java.util.UUID.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "settlement", "SettlementService.java"));
        for (String method : new String[]{"public SettlementInstruction settle(",
                "public SettlementInstruction novate(", "private SettlementInstruction ladderStep("}) {
            int start = svc.indexOf(method);
            assertThat(start).as(method + " must exist").isPositive();
            String body = svc.substring(start, svc.indexOf("\n    }", start));
            assertThat(body).as(method + " must lock the row").contains("findByIdForUpdate");
        }
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("SETTLE-DVP-001") @Tag("SETTLE-NOVATE-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V048__create_settlement.sql")) {
            assertThat(in).as("V048__create_settlement.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("delivery_settled = payment_settled");
            assertThat(sql).contains("status <> 'SETTLED' OR (delivery_settled = TRUE AND final_at IS NOT NULL)");
            assertThat(sql).contains("status = 'SETTLED' OR final_at IS NULL");
            assertThat(sql).contains("released_party <> assuming_party AND assumed_obligation >= 0");
        }
    }
}
