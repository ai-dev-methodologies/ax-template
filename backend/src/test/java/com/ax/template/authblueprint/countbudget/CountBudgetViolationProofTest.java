package com.ax.template.authblueprint.countbudget;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for periodic-count-budget-l0. Structural assertions that a deliberate break cannot pass
 * silently: the policy has no public setter (cap moves only via the package-private sole-mutator hook);
 * period/consumption rows are fully immutable; the row-lock finder is PESSIMISTIC_WRITE; the migration
 * carries the same CHECK backstops — no Spring context.
 */
@Tag("COUNT_BUDGET")
class CountBudgetViolationProofTest {

    // ── PCB-CAP-001 — the policy has no public setter; cap moves ONLY via the package-private hook ──
    @Test @Tag("PCB-CAP-001")
    void violation_policyNoPublicSetter_capMovesOnlyViaPackagePrivateHook() throws Exception {
        for (Method m : CountBudgetPolicy.class.getMethods()) {
            assertThat(m.getName())
                .as("CountBudgetPolicy must expose no public setter (the service is the sole mutator)")
                .doesNotStartWith("set");
        }
        Method hook = CountBudgetPolicy.class.getDeclaredMethod("updateCap", int.class);
        assertThat(Modifier.isPublic(hook.getModifiers()))
            .as("CountBudgetPolicy.updateCap must be package-private (sole-mutator seam)").isFalse();

        for (String f : new String[]{"id", "subjectKey", "cadence", "createdAt"}) {
            Column col = CountBudgetPolicy.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("CountBudgetPolicy." + f + " must be immutable").isFalse();
        }
        assertThat(CountBudgetPolicy.class.getDeclaredField("version").isAnnotationPresent(Version.class))
            .as("CountBudgetPolicy.version must carry @Version").isTrue();
    }

    // ── PCB-RESET-001 / PCB-CAP-001 — a period row is fully immutable once first-touched ──
    @Test @Tag("PCB-RESET-001") @Tag("PCB-CAP-001")
    void violation_periodFullyImmutable() throws Exception {
        for (Method m : CountBudgetPeriod.class.getMethods()) {
            assertThat(m.getName()).as("CountBudgetPeriod must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "policyId", "periodKey", "capAtPeriodStart", "firstTouchedAt"}) {
            Column col = CountBudgetPeriod.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("CountBudgetPeriod." + f + " must be immutable").isFalse();
        }
        Check check = CountBudgetPeriod.class.getAnnotation(Check.class);
        assertThat(check).as("CountBudgetPeriod must carry @Check").isNotNull();
        assertThat(check.constraints()).contains("cap_at_period_start > 0");
    }

    // ── PCB-AUDIT-001 — the consumption ledger is fully append-only ──
    @Test @Tag("PCB-AUDIT-001")
    void violation_consumptionFullyAppendOnly() throws Exception {
        for (Method m : CountBudgetConsumption.class.getMethods()) {
            assertThat(m.getName()).as("CountBudgetConsumption must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "periodId", "sequenceNo", "consumedAt"}) {
            Column col = CountBudgetConsumption.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("CountBudgetConsumption." + f + " must be immutable").isFalse();
        }
    }

    // ── PCB-CONSUME-001 — the row-lock finder is PESSIMISTIC_WRITE, and the SERVICE actually calls it ──
    @Test @Tag("PCB-CONSUME-001")
    void violation_consumeGoesThroughTheLockedFinder() throws Exception {
        Method locked = CountBudgetPolicyRepository.class.getMethod("findBySubjectKeyForUpdate", String.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).as("the serialization point must be PESSIMISTIC_WRITE").isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String src = java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("user.dir"), "src",
            "main", "java", "com", "ax", "template", "authblueprint", "countbudget", "CountBudgetService.java"));
        int start = src.indexOf("public ConsumeResult consume(");
        assertThat(start).as("consume(...) must exist in CountBudgetService").isPositive();
        String body = src.substring(start, src.indexOf("\n    }", start));
        assertThat(body)
            .as("consume(...) must read the policy via the PESSIMISTIC_WRITE finder (PCB-CONSUME-001)")
            .contains("findBySubjectKeyForUpdate");
    }

    // ── PCB-RESET-001 — periodKeyFor is a PURE function of (cadence, instant) — deterministic, TZ-fixed (UTC) ──
    @Test @Tag("PCB-RESET-001")
    void violation_periodKeyForIsPureAndUtcFixed() throws Exception {
        Method m = CountBudgetCadence.class.getDeclaredMethod("periodKeyFor", Instant.class);
        m.setAccessible(true);
        Instant midnight = Instant.parse("2026-03-01T00:00:00Z");
        Instant almostNextDay = Instant.parse("2026-03-01T23:59:59Z");
        assertThat(m.invoke(CountBudgetCadence.DAILY, midnight)).isEqualTo("2026-03-01");
        assertThat(m.invoke(CountBudgetCadence.DAILY, almostNextDay)).isEqualTo("2026-03-01");
        assertThat(m.invoke(CountBudgetCadence.DAILY, Instant.parse("2026-03-02T00:00:00Z"))).isEqualTo("2026-03-02");
        assertThat(m.invoke(CountBudgetCadence.MONTHLY, midnight)).isEqualTo("2026-03");
    }

    // ── the migration carries the same CHECK backstops as the entities ──
    @Test @Tag("COUNT_BUDGET")
    void violation_migrationCarriesTheSameChecks() throws Exception {
        String sql;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V089__create_count_budget.sql")) {
            assertThat(in).as("V089 migration must be on the classpath").isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
        }
        assertThat(sql).contains("chk_count_budget_policy_cap");
        assertThat(sql).contains("chk_count_budget_period_cap");
        assertThat(sql).contains("uq_count_budget_period_key");
        assertThat(sql).contains("uq_count_budget_consumption_seq");
    }
}
