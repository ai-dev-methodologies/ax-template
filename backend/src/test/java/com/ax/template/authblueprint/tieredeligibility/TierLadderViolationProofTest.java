package com.ax.template.authblueprint.tieredeligibility;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for tiered-eligibility-l0. Structural assertions that a deliberate break cannot pass
 * silently: the ladder has no public setter (count/tier move only via the package-private sole-mutator
 * hook, called ONLY by the state machine); the state machine defines EXACTLY two entry points (degrade,
 * restore) — no third silent path; deriveTierIndex is a pure, deterministic function; accrual/restore
 * ledgers are fully immutable; the migration carries the same backstops — no Spring context.
 */
@Tag("TIERED_ELIGIBILITY")
class TierLadderViolationProofTest {

    // ── TIER-DERIVE-001 — the ladder has no public setter; count/tier move ONLY via the package-private hook ──
    @Test @Tag("TIER-DERIVE-001")
    void violation_ladderNoPublicSetter_applyCountIsPackagePrivate() throws Exception {
        for (Method m : TierLadder.class.getMethods()) {
            assertThat(m.getName())
                .as("TierLadder must expose no public setter (the state machine is the sole mutator)")
                .doesNotStartWith("set");
        }
        Method hook = TierLadder.class.getDeclaredMethod("applyCount", int.class, int.class);
        assertThat(Modifier.isPublic(hook.getModifiers()))
            .as("TierLadder.applyCount must be package-private (sole-mutator seam)").isFalse();

        for (String f : new String[]{"id", "ladderKey", "createdAt"}) {
            Column col = TierLadder.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("TierLadder." + f + " must be immutable").isFalse();
        }
        assertThat(TierLadder.class.getDeclaredField("version").isAnnotationPresent(Version.class))
            .as("TierLadder.version must carry @Version").isTrue();

        Check check = TierLadder.class.getAnnotation(Check.class);
        assertThat(check).as("TierLadder must carry @Check").isNotNull();
        assertThat(check.constraints()).contains("ladder_count >= 0").contains("current_tier_index >= 0");
    }

    // ── TIER-MONOTONE-001 — the state machine defines EXACTLY the two audited entry points ──
    @Test @Tag("TIER-MONOTONE-001")
    void violation_stateMachineDefinesExactlyDegradeAndRestore() {
        long nonSynthetic = 0;
        for (Method m : TierLadderStateMachine.class.getDeclaredMethods()) {
            if (m.isSynthetic()) {
                continue;
            }
            nonSynthetic++;
            assertThat(m.getName())
                .as("the ladder must move ONLY via degrade() or restore() — no third silent path")
                .isIn("degrade", "restore");
        }
        assertThat(nonSynthetic).isEqualTo(2);
    }

    // ── TIER-LADDER-001 / TIER-DERIVE-001 — deriveTierIndex is a pure, deterministic function of count ──
    @Test @Tag("TIER-LADDER-001") @Tag("TIER-DERIVE-001")
    void violation_deriveTierIndexIsPureAndTotal() {
        TierLadder l = new TierLadder(UUID.randomUUID(), "ladder-" + UUID.randomUUID(),
            List.of(new TierDefinition("FULL", 0), new TierDefinition("WARN", 20),
                new TierDefinition("REDUCED", 50), new TierDefinition("SUSPENDED", 100)),
            0, Instant.now());

        assertThat(l.deriveTierIndex(0)).isEqualTo(0);
        assertThat(l.deriveTierIndex(19)).isEqualTo(0);
        assertThat(l.deriveTierIndex(20)).as("threshold is inclusive at entry").isEqualTo(1);
        assertThat(l.deriveTierIndex(49)).isEqualTo(1);
        assertThat(l.deriveTierIndex(50)).isEqualTo(2);
        assertThat(l.deriveTierIndex(99)).isEqualTo(2);
        assertThat(l.deriveTierIndex(100)).isEqualTo(3);
        assertThat(l.deriveTierIndex(100_000)).as("overshoot still resolves to the worst tier").isEqualTo(3);
    }

    // ── the accrual and restore ledgers are fully append-only ──
    @Test @Tag("TIER-LADDER-001")
    void violation_accrualLedgerFullyAppendOnly() throws Exception {
        for (Method m : TierAccrual.class.getMethods()) {
            assertThat(m.getName()).as("TierAccrual must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "ladderId", "delta", "countAfter", "tierIndexAfter", "sequenceNo",
                "recordedAt"}) {
            Column col = TierAccrual.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("TierAccrual." + f + " must be immutable").isFalse();
        }
    }

    @Test @Tag("TIER-MONOTONE-001")
    void violation_restoreLedgerFullyAppendOnly_carriesReasonCheck() throws Exception {
        for (Method m : TierRestoreEvent.class.getMethods()) {
            assertThat(m.getName()).as("TierRestoreEvent must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "ladderId", "countAfter", "tierIndexAfter", "reason", "sequenceNo",
                "recordedAt"}) {
            Column col = TierRestoreEvent.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("TierRestoreEvent." + f + " must be immutable").isFalse();
        }
        Check check = TierRestoreEvent.class.getAnnotation(Check.class);
        assertThat(check).as("TierRestoreEvent must carry @Check").isNotNull();
    }

    // ── the migration carries the same backstops as the entities ──
    @Test @Tag("TIERED_ELIGIBILITY")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        String sql;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V090__create_tiered_eligibility.sql")) {
            assertThat(in).as("V090 migration must be on the classpath").isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
        }
        assertThat(sql).contains("ladder_count >= 0 AND current_tier_index >= 0");
        assertThat(sql).contains("chk_tier_accrual_delta");
        assertThat(sql).contains("chk_tier_restore_reason");
        assertThat(sql).contains("uq_tier_ladder_key");
    }
}
