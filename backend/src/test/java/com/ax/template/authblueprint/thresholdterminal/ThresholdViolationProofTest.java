package com.ax.template.authblueprint.thresholdterminal;

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
 * VIOLATION proof for threshold-terminal-derivation-l0. Structural assertions that a deliberate break
 * cannot pass silently: the entity carries the @Check implication, the limit is immutable and the
 * lifecycle has no public setter, the state machine defines no edge OUT of EXPIRED, and the migration
 * carries the same DB backstop — no Spring context.
 */
@Tag("THRESHOLD_TERMINAL")
class ThresholdViolationProofTest {

    // ── TTD-CHECK-001 — the entity declares the implication anchor ≥ limit ⇒ EXPIRED ──
    @Test @Tag("TTD-CHECK-001")
    void violation_entityCarriesTheCheckImplication() {
        Check check = ThresholdRegister.class.getAnnotation(Check.class);
        assertThat(check).as("ThresholdRegister must carry @Check (TTD-CHECK-001)").isNotNull();
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("anchor_value < limit_value OR status = 'EXPIRED'");
        assertThat(c).contains("limit_value > 0");
    }

    // ── TTD-TERMINAL-001 — immutable limit/identity; no public setter; lifecycle is package-sealed ──
    @Test @Tag("TTD-TERMINAL-001")
    void violation_noPublicSetter_limitImmutable_versioned() throws Exception {
        for (Method m : ThresholdRegister.class.getMethods()) {
            assertThat(m.getName())
                .as("ThresholdRegister must expose no public setter (state machine + service are sole mutators)")
                .doesNotStartWith("set");
        }
        // the two mutation hooks must stay package-private — getDeclaredMethods sees them regardless
        // of visibility, so an accidental widening to public FAILS here (getMethods would not see it)
        for (String hook : new String[]{"markExpired", "advanceAnchor"}) {
            Method m = ThresholdRegister.class.getDeclaredMethod(hook,
                hook.equals("advanceAnchor") ? new Class<?>[]{java.math.BigDecimal.class} : new Class<?>[]{});
            assertThat(Modifier.isPublic(m.getModifiers()))
                .as("ThresholdRegister." + hook + " must be package-private (sole-mutator seam)")
                .isFalse();
            assertThat(Modifier.isProtected(m.getModifiers()))
                .as("ThresholdRegister." + hook + " must not be protected (subclass escape)")
                .isFalse();
        }
        for (String f : new String[]{"id", "scopeKey", "limit", "createdAt"}) {
            Column col = ThresholdRegister.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("ThresholdRegister." + f + " must be immutable").isFalse();
        }
        assertThat(ThresholdRegister.class.getDeclaredField("version").isAnnotationPresent(Version.class))
            .as("ThresholdRegister.version must carry @Version").isTrue();
    }

    // ── TTD-TERMINAL-001 — the state machine defines no edge OUT of EXPIRED (zero outgoing edges) ──
    @Test @Tag("TTD-TERMINAL-001")
    void violation_stateMachineHasNoEdgeOutOfTerminal() {
        for (Method m : ThresholdRegisterStateMachine.class.getDeclaredMethods()) {
            if (m.isSynthetic()) {
                continue;
            }
            assertThat(m.getName())
                .as("the ONLY transition is the one-way crossing edge — no un-expire/reset/reactivate")
                .isEqualTo("expire");
        }
        assertThat(ThresholdStatus.EXPIRED.isTerminal()).isTrue();
        assertThat(ThresholdStatus.ACTIVE.isTerminal()).isFalse();
    }

    // ── TTD-DERIVE-001 / TTD-CONCURRENT-001 — both write-paths lock the row (repo declares FOR UPDATE) ──
    @Test @Tag("TTD-DERIVE-001") @Tag("TTD-CONCURRENT-001")
    void violation_bothWritePathsGoThroughTheLockedFinder() throws Exception {
        Method locked = ThresholdRegisterRepository.class.getMethod("findByScopeKeyForUpdate", String.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).as("the serialization point must be PESSIMISTIC_WRITE").isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        // and the SERVICE actually calls it on BOTH write-paths — a refactor that downgrades accrue()
        // or use() to the unlocked finder would pass the declaration check above but fail here
        String src = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "thresholdterminal", "ThresholdRegisterService.java"));
        for (String method : new String[]{"public ThresholdRegister accrue(", "public ThresholdRegister use("}) {
            int start = src.indexOf(method);
            assertThat(start).as(method + " must exist in ThresholdRegisterService").isPositive();
            String body = src.substring(start, src.indexOf("\n    }", start));
            assertThat(body)
                .as(method + " must read the row via the PESSIMISTIC_WRITE finder (TTD-DERIVE/CONCURRENT-001)")
                .contains("findByScopeKeyForUpdate");
        }
    }

    // ── TTD-CHECK-001 — the migration carries the same DB backstop as the entity ──
    @Test @Tag("TTD-CHECK-001") @Tag("TTD-CROSS-001")
    void violation_migrationCarriesTheSameBackstop() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V042__create_threshold_registers.sql")) {
            assertThat(in).as("V042__create_threshold_registers.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("anchor_value < limit_value OR status = 'EXPIRED'");
            assertThat(sql).contains("limit_value > 0");
            assertThat(sql).contains("UNIQUE INDEX uq_threshold_register_scope");
        }
    }
}
