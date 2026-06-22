package com.ax.template.authblueprint.dunning;

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
 * VIOLATION proof for dunning-collections-l0. Structural assertions a deliberate break cannot
 * pass silently: the stage transitions are append-only one-per-(case,stage,kind), the aging
 * bucket carries its own immutable basis, the case carries @Version + the @Check backstops, NO
 * delete path exists anywhere in the domain, mutators are package-sealed, the write path uses
 * the PESSIMISTIC_WRITE finder, and the migration carries the same backstops.
 */
@Tag("DUNNING")
class DunningViolationProofTest {

    // ── DUNNING-LADDER-001 — transitions append-only, one per (case, stage, kind) ──
    @Test @Tag("DUNNING-LADDER-001")
    void violation_transitionsAppendOnly_uniquePerStageKind() throws Exception {
        for (Method m : DunningStageTransition.class.getMethods()) {
            assertThat(m.getName()).as("DunningStageTransition must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "caseId", "stage", "kind", "daysOverdue", "actor", "occurredAt"}) {
            Column col = DunningStageTransition.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("DunningStageTransition." + f + " must be immutable").isFalse();
        }
        jakarta.persistence.Table table = DunningStageTransition.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("case_id", "stage", "kind");
    }

    // ── DUNNING-LADDER-001 — the one-way ladder never skips or reverses ──
    @Test @Tag("DUNNING-LADDER-001")
    void violation_ladderIsOneWay_terminalHasNoNext() {
        assertThat(DunningStage.REMINDER.next()).isEqualTo(DunningStage.NOTICE);
        assertThat(DunningStage.NOTICE.next()).isEqualTo(DunningStage.FINAL_NOTICE);
        assertThat(DunningStage.FINAL_NOTICE.next()).isEqualTo(DunningStage.SUSPENDED);
        assertThat(DunningStage.SUSPENDED.next()).as("SUSPENDED is terminal — one-way ladder").isNull();
    }

    // ── DUNNING-AGING-001 — deterministic cut-points; the basis columns exist ──
    @Test @Tag("DUNNING-AGING-001")
    void violation_agingDeterministic_basisRecorded() throws Exception {
        assertThat(AgingBucket.of(0)).isEqualTo(AgingBucket.CURRENT);
        assertThat(AgingBucket.of(-5)).isEqualTo(AgingBucket.CURRENT);
        assertThat(AgingBucket.of(1)).isEqualTo(AgingBucket.B1_30);
        assertThat(AgingBucket.of(30)).isEqualTo(AgingBucket.B1_30);
        assertThat(AgingBucket.of(31)).isEqualTo(AgingBucket.B2_60);
        assertThat(AgingBucket.of(60)).isEqualTo(AgingBucket.B2_60);
        assertThat(AgingBucket.of(61)).isEqualTo(AgingBucket.B3_90_PLUS);
        assertThat(AgingBucket.of(365)).isEqualTo(AgingBucket.B3_90_PLUS);
        // the recorded basis lives on the case row
        for (String f : new String[]{"agingBucket", "agingAsOf", "daysOverdue"}) {
            assertThat(DunningCase.class.getDeclaredField(f).getAnnotation(Column.class))
                .as(f + " must carry @Column").isNotNull();
        }
    }

    // ── DUNNING-LADDER/CURE-001 — NO delete path; @Check backstops; mutators sealed; @Version ──
    @Test @Tag("DUNNING-LADDER-001") @Tag("DUNNING-CURE-001")
    void violation_noDeletePath_checkBackstops_mutatorsSealed() throws Exception {
        for (Method m : DunningCaseRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"DunningService", "DunningController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "dunning", src + ".java"));
            assertThat(text).as(src + " must contain no delete call — cases are closed, never removed")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
        Check check = DunningCase.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("overdue_amount >= 0 AND paid_amount >= 0");
        assertThat(c).contains("cure_deadline IS NULL OR cure_window_opened_at IS NOT NULL");
        assertThat(c).contains("ladder_halted = FALSE OR aging_bucket = 'CURRENT'");

        for (String hook : new String[]{"advanceTo", "reage", "openCureWindow", "addPayment", "cure", "releaseHalt"}) {
            Method m = java.util.Arrays.stream(DunningCase.class.getDeclaredMethods())
                .filter(x -> x.getName().equals(hook)).findFirst().orElseThrow();
            assertThat(Modifier.isPublic(m.getModifiers()))
                .as("DunningCase." + hook + " must be package-private").isFalse();
        }
        // immutable identity columns on the case
        for (String f : new String[]{"id", "receivableRef", "overdueAmount", "createdAt"}) {
            Column col = DunningCase.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col.updatable()).as("DunningCase." + f + " must be immutable").isFalse();
        }
        assertThat(DunningCase.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── DUNNING-CONCURRENT-001 — the write path uses the PESSIMISTIC_WRITE finder ──
    @Test @Tag("DUNNING-CONCURRENT-001")
    void violation_lockedFinder_andSerializedAdvance() throws Exception {
        Method locked = DunningCaseRepository.class.getMethod("findByIdForUpdate", java.util.UUID.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "dunning", "DunningService.java"));
        for (String method : new String[]{"public DunningCase advance(", "public DunningCase cure("}) {
            int start = svc.indexOf(method);
            assertThat(start).as(method + " must exist").isPositive();
            String body = svc.substring(start, svc.indexOf("\n    }", start));
            assertThat(body).as(method + " must take the case row lock").contains("findByIdForUpdate");
        }
        assertThat(svc).as("the advance precondition gates on the observed stage")
            .contains("c.getStage() != fromStage");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("DUNNING-LADDER-001") @Tag("DUNNING-AGING-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V047__create_dunning.sql")) {
            assertThat(in).as("V047__create_dunning.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("overdue_amount >= 0 AND paid_amount >= 0");
            assertThat(sql).contains("ladder_halted = FALSE OR aging_bucket = 'CURRENT'");
            assertThat(sql).contains("UNIQUE INDEX uq_dunning_case_stage");
            assertThat(sql).contains("(case_id, stage, kind)");
        }
    }
}
