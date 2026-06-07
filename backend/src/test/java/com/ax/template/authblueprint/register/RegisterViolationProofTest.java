package com.ax.template.authblueprint.register;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for monotone-register-l0. Structural immutability (register has no public setter;
 * reads are fully append-only) + governed-exception enum shape + migration monotone/non-negative-delta
 * backstops — no Spring context.
 */
@Tag("REGISTER")
class RegisterViolationProofTest {

    // ── REG-MONOTONE-001 — register has no public setter; funding/identity columns immutable ──
    @Test @Tag("REG-MONOTONE-001")
    void violation_registerNoPublicSetter_immutableIdentity_versioned() throws Exception {
        for (Method m : Register.class.getMethods()) {
            assertThat(m.getName())
                .as("Register must expose no public setter (anchor moves only via the service)")
                .doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "scopeKey", "modulus", "createdAt"}) {
            Column col = Register.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("Register." + f + " must be immutable").isFalse();
        }
        assertThat(Register.class.getDeclaredField("version").isAnnotationPresent(Version.class))
            .as("Register.version must carry @Version").isTrue();
    }

    // ── REG-DELTA-001 — reads are fully append-only (every column updatable=false, no public setter) ──
    @Test @Tag("REG-DELTA-001")
    void violation_readingFullyAppendOnly() throws Exception {
        for (Method m : RegisterReading.class.getMethods()) {
            assertThat(m.getName()).as("RegisterReading must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "registerId", "kind", "readingValue", "priorAnchor",
                "delta", "sequenceNo", "reason", "recordedAt"}) {
            Column col = RegisterReading.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("RegisterReading." + f + " must be immutable").isFalse();
        }
    }

    // ── REG-ROLLOVER-001 / REG-EXCHANGE-001 — only the governed kinds may reset below the anchor ──
    @Test @Tag("REG-ROLLOVER-001") @Tag("REG-EXCHANGE-001")
    void violation_onlyGovernedKindsAreExceptions() {
        assertThat(ReadingKind.NORMAL.isGovernedException()).as("NORMAL is the monotone path").isFalse();
        assertThat(ReadingKind.ROLLOVER.isGovernedException()).isTrue();
        assertThat(ReadingKind.EXCHANGE.isGovernedException()).isTrue();
    }

    // ── REG-MONOTONE-001 — migration declares the monotone-anchor + non-negative-delta CHECK backstops ──
    @Test @Tag("REG-MONOTONE-001")
    void violation_migrationDeclaresAnchorAndDeltaChecks() throws Exception {
        String sql;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V039__create_registers.sql")) {
            assertThat(in).as("V039 migration must be on the classpath").isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(sql).contains("chk_register_anchor");
        assertThat(sql).contains("anchor_value < modulus");
        assertThat(sql).contains("chk_register_reading_delta");
        assertThat(sql).contains("delta >= 0");
    }
}
