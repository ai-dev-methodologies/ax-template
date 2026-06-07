package com.ax.template.authblueprint.netting;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for collection-conservation-l0. Append-only inputs + immutable computed positions +
 * no public setters + migration set-wide-zero / one-position-per-member backstops — no Spring context.
 */
@Tag("NETTING")
class NettingViolationProofTest {

    // ── NET-ONCE-001 — run has no public setter; key/currency immutable; versioned ──
    @Test @Tag("NET-ONCE-001")
    void violation_runNoPublicSetter_immutableKeyCurrency_versioned() throws Exception {
        for (Method m : NettingRun.class.getMethods()) {
            assertThat(m.getName())
                .as("NettingRun must expose no public setter (status/netTotal move only via the service)")
                .doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "runKey", "currency", "createdAt"}) {
            Column col = NettingRun.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("NettingRun." + f + " must be immutable").isFalse();
        }
        assertThat(NettingRun.class.getDeclaredField("version").isAnnotationPresent(Version.class))
            .as("NettingRun.version must carry @Version").isTrue();
        // NETTED is the terminal reduction state
        assertThat(NettingStatus.values()).containsExactly(NettingStatus.OPEN, NettingStatus.NETTED);
    }

    // ── NET-INPUTS-IMMUTABLE-001 — gross obligations + net positions fully append-only/immutable ──
    @Test @Tag("NET-INPUTS-IMMUTABLE-001")
    void violation_obligationsAndPositionsImmutable() throws Exception {
        for (Method m : GrossObligation.class.getMethods()) {
            assertThat(m.getName()).as("GrossObligation must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "runId", "fromMember", "toMember", "amount", "currency", "createdAt"}) {
            assertThat(GrossObligation.class.getDeclaredField(f).getAnnotation(Column.class).updatable())
                .as("GrossObligation." + f + " immutable").isFalse();
        }
        for (Method m : NetPosition.class.getMethods()) {
            assertThat(m.getName()).as("NetPosition must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "runId", "member", "netAmount"}) {
            assertThat(NetPosition.class.getDeclaredField(f).getAnnotation(Column.class).updatable())
                .as("NetPosition." + f + " immutable").isFalse();
        }
    }

    // ── NET-SETWIDE-ZERO-001 — migration declares set-wide-zero + one-position-per-member backstops ──
    @Test @Tag("NET-SETWIDE-ZERO-001")
    void violation_migrationDeclaresSetWideZeroAndUniqueness() throws Exception {
        String sql;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V040__create_netting.sql")) {
            assertThat(in).as("V040 migration must be on the classpath").isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(sql).contains("chk_netting_setwide_zero");
        assertThat(sql).contains("net_total = 0");
        assertThat(sql).contains("uq_net_position_run_member");
        assertThat(sql).contains("from_member <> to_member");
    }
}
