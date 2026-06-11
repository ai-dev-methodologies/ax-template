package com.ax.template.authblueprint.trueup;

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
 * VIOLATION proof for remeasurement-trueup-l0. Structural assertions a deliberate break cannot
 * pass silently: a reading's value/source/method are immutable and @Check-paired, a run row has
 * no update path and versions uniquely per period, a posting is fully append-only with one-per-run
 * uniqueness, the period FSM mutators are package-sealed with the closed⇒run-of-record @Check,
 * NO delete path exists anywhere in the domain, write paths use the PESSIMISTIC_WRITE period
 * finder in ascending-id order, and the migration carries the same backstops.
 */
@Tag("TRUEUP")
class TrueUpViolationProofTest {

    // ── TUP-SUPERSEDE-001 — the reading's truth is immutable and source-method paired ──
    @Test @Tag("TUP-SUPERSEDE-001")
    void violation_readingImmutable_andCheckBackstopped() throws Exception {
        for (String f : new String[]{"periodId", "slotIndex", "slotVersion", "readingValue",
                "source", "estimationMethod"}) {
            Column col = MeterReading.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("MeterReading." + f + " must be immutable").isFalse();
        }
        Check check = MeterReading.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("status <> 'SUPERSEDED' OR superseded_by_id IS NOT NULL");
        assertThat(c).contains("source = 'ESTIMATED' AND estimation_method IS NOT NULL");
        assertThat(c).contains("source = 'ACTUAL' AND estimation_method IS NULL");
        assertThat(c).contains("slot_version >= 1");
        Method supersededBy = java.util.Arrays.stream(MeterReading.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("supersededBy")).findFirst().orElseThrow();
        assertThat(Modifier.isPublic(supersededBy.getModifiers()))
            .as("MeterReading.supersededBy must be package-private").isFalse();
        assertThat(MeterReading.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
        jakarta.persistence.Table table = MeterReading.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames())
            .containsExactly("period_id", "slot_index", "slot_version");
    }

    // ── TUP-RUNVERSION-001 — a run row has NO update path; versions are unique per period ──
    @Test @Tag("TUP-RUNVERSION-001")
    void violation_runRowFullyImmutable_uniqueVersionPerPeriod() throws Exception {
        for (Method m : SettlementRun.class.getMethods()) {
            assertThat(m.getName()).as("SettlementRun must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"periodId", "runVersion", "basisJson", "basisHash",
                "totalValue", "computedAt"}) {
            Column col = SettlementRun.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("SettlementRun." + f + " must be immutable").isFalse();
        }
        jakarta.persistence.Table table = SettlementRun.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("period_id", "run_version");
    }

    // ── TUP-DELTA-001 — postings append-only, exactly one per run ──
    @Test @Tag("TUP-DELTA-001")
    void violation_postingAppendOnly_onePerRun() throws Exception {
        for (Method m : TrueUpPosting.class.getMethods()) {
            assertThat(m.getName()).as("TrueUpPosting must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"runId", "sourcePeriodId", "targetPeriodId", "fromRunVersion",
                "toRunVersion", "amount", "postedAt"}) {
            Column col = TrueUpPosting.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("TrueUpPosting." + f + " must be immutable").isFalse();
        }
        jakarta.persistence.Table table = TrueUpPosting.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("run_id");
    }

    // ── TUP-SEALED-001 — FSM mutators sealed; closed/sealed implies run-of-record ──
    @Test @Tag("TUP-SEALED-001")
    void violation_periodMutatorsSealed_checkBackstopped() throws Exception {
        for (String hook : new String[]{"close", "seal"}) {
            Method m = java.util.Arrays.stream(SettlementPeriod.class.getDeclaredMethods())
                .filter(x -> x.getName().equals(hook)).findFirst().orElseThrow();
            assertThat(Modifier.isPublic(m.getModifiers()))
                .as("SettlementPeriod." + hook + " must be package-private").isFalse();
        }
        Check check = SettlementPeriod.class.getAnnotation(Check.class);
        assertThat(check.constraints().replaceAll("\\s+", " "))
            .contains("status = 'OPEN' OR run_of_record_id IS NOT NULL");
        assertThat(SettlementPeriod.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── no delete path anywhere — corrections post forward, history is never erased ──
    @Test @Tag("TUP-SUPERSEDE-001") @Tag("TUP-DELTA-001")
    void violation_noDeletePath() throws Exception {
        for (Class<?> repo : new Class<?>[]{SettlementPeriodRepository.class,
                MeterReadingRepository.class, SettlementRunRepository.class}) {
            for (Method m : repo.getDeclaredMethods()) {
                assertThat(m.getName()).doesNotContain("delete");
            }
        }
        for (String src : new String[]{"TrueUpService", "TrueUpController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "trueup", src + ".java"));
            assertThat(text).as(src + " must contain no delete call — history is never erased")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
    }

    // ── TUP-CONCURRENT-001 — write paths use the locked period finder, ascending lock order ──
    @Test @Tag("TUP-CONCURRENT-001")
    void violation_lockedPeriodFinder_andAscendingLockOrder() throws Exception {
        Method locked = SettlementPeriodRepository.class.getMethod("findByIdForUpdate", java.util.UUID.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "trueup", "TrueUpService.java"));
        for (String method : new String[]{"public MeterReading recordReading(",
                "public List<MeterReading> estimateMissing(", "public SettlementPeriod close(",
                "public SettlementPeriod seal("}) {
            int start = svc.indexOf(method);
            assertThat(start).as(method + " must exist").isPositive();
            String body = svc.substring(start, svc.indexOf("\n    }", start));
            assertThat(body).contains("findByIdForUpdate");
        }
        assertThat(svc).as("recompute locks source+target through the lock helper")
            .contains("lockPeriods(periodId, targetPeriodId)");
        assertThat(svc).as("ascending-id lock order is the deadlock guard")
            .contains("periodId.compareTo(targetPeriodId) < 0");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("TUP-RUNVERSION-001") @Tag("TUP-SEALED-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V046__create_remeasurement_trueup.sql")) {
            assertThat(in).as("V046__create_remeasurement_trueup.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("status = 'OPEN' OR run_of_record_id IS NOT NULL");
            assertThat(sql).contains("status <> 'SUPERSEDED' OR superseded_by_id IS NOT NULL");
            assertThat(sql).contains("source = 'ESTIMATED' AND estimation_method IS NOT NULL");
            assertThat(sql).contains("source = 'ACTUAL' AND estimation_method IS NULL");
            assertThat(sql).contains("slot_version >= 1");
            assertThat(sql).contains("grid_slots >= 1");
            assertThat(sql).contains("UNIQUE INDEX uq_reading_slot_version");
            assertThat(sql).contains("UNIQUE INDEX uq_run_version");
            assertThat(sql).contains("UNIQUE INDEX uq_trueup_run");
        }
    }
}
