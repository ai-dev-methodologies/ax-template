package com.ax.template.authblueprint.netmetering;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for signed-dual-register-l0. Structural immutability (the meter exposes no public setter
 * — neither a cumulative nor the net is settable; readings + period snapshots are fully append-only) +
 * derived-net invariant (advance always re-derives net = import − export) + migration monotone/non-negative
 * backstops — no Spring context.
 */
@Tag("NETMETERING")
class NetMeterViolationProofTest {

    // ── NETM-NET-001 — the meter has NO public setter; the net is DERIVED, never settable ──
    @Test @Tag("NETM-NET-001")
    void violation_meterNoPublicSetter_netNotSettable_immutableIdentity_versioned() throws Exception {
        for (Method m : NetMeter.class.getMethods()) {
            assertThat(m.getName())
                .as("NetMeter must expose no public setter (cumulatives + net move only via the service); "
                    + "the net is DERIVED, never settable (a setNet would let it drift from the registers)")
                .doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "meterKey", "createdAt", "rateImport", "rateExport"}) {
            Column col = NetMeter.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("NetMeter." + f + " must be immutable").isFalse();
        }
        assertThat(NetMeter.class.getDeclaredField("version").isAnnotationPresent(Version.class))
            .as("NetMeter.version must carry @Version").isTrue();
    }

    // ── NETM-NET-001 — advance ALWAYS re-derives net = cumulativeImport − cumulativeExport ──
    @Test @Tag("NETM-NET-001")
    void violation_advanceAlwaysReDerivesNet() {
        NetMeter m = new NetMeter(UUID.randomUUID(), "k-" + UUID.randomUUID(),
            new BigDecimal("100.0000"), new BigDecimal("0.0000"), BigDecimal.ONE, BigDecimal.ONE,
            Instant.MIN, Instant.now());
        assertThat(m.getNet()).as("net derived at construction = 100 − 0").isEqualByComparingTo("100");

        m.advance(MeterDirection.EXPORT, new BigDecimal("160.0000"));     // export now dominates → net negative
        assertThat(m.getNet()).as("net re-derived = 100 − 160").isEqualByComparingTo("-60");
        assertThat(m.getNet()).isEqualByComparingTo(m.getCumulativeImport().subtract(m.getCumulativeExport()));

        m.advance(MeterDirection.IMPORT, new BigDecimal("250.0000"));     // import dominates again → net positive
        assertThat(m.getNet()).as("net re-derived = 250 − 160").isEqualByComparingTo("90");
        assertThat(m.getNet()).isEqualByComparingTo(m.getCumulativeImport().subtract(m.getCumulativeExport()));
    }

    // ── NETM-DIRECTION-001 — readings are fully append-only (every column updatable=false, no public setter) ──
    @Test @Tag("NETM-DIRECTION-001")
    void violation_readingFullyAppendOnly() throws Exception {
        for (Method m : NetMeterReading.class.getMethods()) {
            assertThat(m.getName()).as("NetMeterReading must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "meterId", "direction", "readingValue", "priorCumulative",
                "delta", "netAfter", "importAfter", "exportAfter", "sequenceNo", "effectiveAt", "recordedAt"}) {
            Column col = NetMeterReading.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("NetMeterReading." + f + " must be immutable").isFalse();
        }
    }

    // ── NETM-PERIOD-001 — closed-period snapshots are fully immutable (no setter; every column updatable=false) ──
    @Test @Tag("NETM-PERIOD-001")
    void violation_periodSnapshotFullyImmutable() throws Exception {
        for (Method m : NetMeterPeriod.class.getMethods()) {
            assertThat(m.getName()).as("NetMeterPeriod must have no public setter (a closed period is frozen)")
                .doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "meterId", "boundaryAt", "importCumulative", "exportCumulative",
                "netStart", "netEnd", "periodNetDelta", "importDelta", "exportDelta", "rateImport", "rateExport",
                "billedAmount", "sequenceNo", "closedAt"}) {
            Column col = NetMeterPeriod.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("NetMeterPeriod." + f + " must be immutable").isFalse();
        }
    }

    // ── NETM-DIRECTION-001 — migration declares the per-direction non-negative + non-negative-delta CHECK backstops ──
    @Test @Tag("NETM-DIRECTION-001")
    void violation_migrationDeclaresCumulativeAndDeltaChecks() throws Exception {
        String sql;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V054__create_netmetering.sql")) {
            assertThat(in).as("V054 migration must be on the classpath").isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(sql).contains("chk_net_meter_cumulatives");
        assertThat(sql).contains("cumulative_import >= 0 AND cumulative_export >= 0");
        assertThat(sql).contains("chk_net_meter_reading_delta");
        assertThat(sql).contains("delta >= 0 AND reading_value >= 0");
    }

    // ── NETM-RATE-001 — the entity's @Check folds in strict rate positivity; the extension migration
    //    carries the same backstop ──
    @Test @Tag("NETM-RATE-001")
    void violation_rateIsStrictlyPositive_backstoppedByCheckAndMigration() throws Exception {
        org.hibernate.annotations.Check check = NetMeter.class.getAnnotation(org.hibernate.annotations.Check.class);
        assertThat(check).as("NetMeter must carry @Check (NETM-RATE-001)").isNotNull();
        assertThat(check.constraints().replaceAll("\\s+", " ")).contains("rate_import > 0 AND rate_export > 0");

        String sql;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V087__extend_netmetering_rate.sql")) {
            assertThat(in).as("V087 migration must be on the classpath").isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(sql).contains("rate_import > 0 AND rate_export > 0");
    }
}
