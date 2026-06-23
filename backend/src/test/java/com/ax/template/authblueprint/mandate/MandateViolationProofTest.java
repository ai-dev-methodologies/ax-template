package com.ax.template.authblueprint.mandate;

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
 * VIOLATION proof for mandate-fanout-l0. Structural assertions a deliberate break cannot pass
 * silently: completion is a DERIVED recall (a state <> PENDING count query, never a stored flag on
 * the root); the fan-out children are unique per (mandate, task_seq) and the battery checks per
 * (mandate, check_key); a terminal task carries its resolver/reason/instant (@Check); the root
 * carries @Version + the issuedCount/SATISFIED @Check backstops; NO delete path exists anywhere in
 * the domain; mutators are package-sealed; the explicit-complete + deemed worker both use the
 * task's PESSIMISTIC_WRITE finder; the deemed sweep drives the worker cross-bean (not a same-bean
 * self-invocation); and the migration carries the same backstops.
 */
@Tag("MANDATE")
class MandateViolationProofTest {

    // ── MANDATE-FANOUT-001 — the root has NO stored completion flag; completion is a derived query ──
    @Test @Tag("MANDATE-FANOUT-001")
    void violation_completionIsDerived_notAStoredFlag() throws Exception {
        // the Mandate root carries no boolean completion column
        for (java.lang.reflect.Field f : Mandate.class.getDeclaredFields()) {
            assertThat(f.getName().toLowerCase())
                .as("Mandate must not store a completion flag — completion is a derived recall")
                .doesNotContain("complete").doesNotContain("done");
        }
        // the derived recall is a COUNT over non-PENDING (terminal) children
        String repo = Files.readString(srcPath("MandateRepository"));
        assertThat(repo).as("the completion recall counts terminal children (state <> PENDING)")
            .contains("countTerminalTasks")
            .contains("t.state <> com.ax.template.authblueprint.mandate.MandateTaskState.PENDING");
        // the service compares the count to issuedCount (Σ terminal == issuedCount), never a flag
        String svc = Files.readString(srcPath("MandateService"));
        assertThat(svc).contains("terminal == m.getIssuedCount()");
        // issuedCount is immutable on the root
        assertThat(Mandate.class.getDeclaredField("issuedCount").getAnnotation(Column.class).updatable())
            .as("Mandate.issuedCount must be immutable").isFalse();
    }

    // ── MANDATE-FANOUT-001 — fan-out children unique per (mandate, task_seq); terminal carries who/why/when ──
    @Test @Tag("MANDATE-FANOUT-001")
    void violation_taskUniquePerSeq_terminalRecorded_noPublicSetter() throws Exception {
        for (Method m : MandateTask.class.getMethods()) {
            assertThat(m.getName()).as("MandateTask must have no public setter").doesNotStartWith("set");
        }
        jakarta.persistence.Table table = MandateTask.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("mandate_id", "task_seq");
        // immutable identity columns
        for (String f : new String[]{"id", "mandateId", "taskSeq", "deemedDeadline", "createdAt"}) {
            assertThat(MandateTask.class.getDeclaredField(f).getAnnotation(Column.class).updatable())
                .as("MandateTask." + f + " must be immutable").isFalse();
        }
        // a terminal task records resolver/reason/instant (the @Check)
        Check check = MandateTask.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("state = 'PENDING'");
        assertThat(c).contains("resolved_by IS NOT NULL AND resolved_at IS NOT NULL AND resolve_reason IS NOT NULL");
        // @Version present so the explicit-complete + deemed paths share the locked row
        assertThat(MandateTask.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── MANDATE-BATTERY-001 — checks unique per (mandate, check_key); only the verdict is mutable ──
    @Test @Tag("MANDATE-BATTERY-001")
    void violation_checkUniquePerKey_keyImmutable_noPublicSetter() throws Exception {
        for (Method m : MandateCheck.class.getMethods()) {
            assertThat(m.getName()).as("MandateCheck must have no public setter").doesNotStartWith("set");
        }
        jakarta.persistence.Table table = MandateCheck.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("mandate_id", "check_key");
        for (String f : new String[]{"id", "mandateId", "checkKey", "declaredAt"}) {
            assertThat(MandateCheck.class.getDeclaredField(f).getAnnotation(Column.class).updatable())
                .as("MandateCheck." + f + " must be immutable").isFalse();
        }
        // the gate reads per-check verdicts (allMatch isPassed), not a bare aggregate
        String svc = Files.readString(srcPath("MandateService"));
        assertThat(svc).as("satisfy reads per-check verdicts, pass-all")
            .contains("battery.stream().allMatch(MandateCheck::isPassed)")
            .contains("!battery.isEmpty()");
    }

    // ── MANDATE-* — @Check backstops on the root; mutators sealed; @Version; NO delete path ──
    @Test @Tag("MANDATE-FANOUT-001") @Tag("MANDATE-BATTERY-001")
    void violation_rootChecks_mutatorsSealed_noDeletePath() throws Exception {
        Check check = Mandate.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("issued_count > 0");
        assertThat(c).contains("status <> 'SATISFIED' OR (satisfied_by IS NOT NULL AND satisfied_at IS NOT NULL)");
        assertThat(Mandate.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();

        // sealed mutators (package-private hooks only)
        for (String hook : new String[]{"markSatisfied"}) {
            Method m = java.util.Arrays.stream(Mandate.class.getDeclaredMethods())
                .filter(x -> x.getName().equals(hook)).findFirst().orElseThrow();
            assertThat(Modifier.isPublic(m.getModifiers())).as("Mandate." + hook + " must be package-private").isFalse();
        }
        for (String hook : new String[]{"resolve"}) {
            Method m = java.util.Arrays.stream(MandateTask.class.getDeclaredMethods())
                .filter(x -> x.getName().equals(hook)).findFirst().orElseThrow();
            assertThat(Modifier.isPublic(m.getModifiers())).as("MandateTask." + hook + " must be package-private").isFalse();
        }
        for (String hook : new String[]{"record"}) {
            Method m = java.util.Arrays.stream(MandateCheck.class.getDeclaredMethods())
                .filter(x -> x.getName().equals(hook)).findFirst().orElseThrow();
            assertThat(Modifier.isPublic(m.getModifiers())).as("MandateCheck." + hook + " must be package-private").isFalse();
        }

        // NO delete path anywhere — mandates + children are recorded forever
        for (Method m : MandateRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"MandateService", "MandateController", "MandateDeemedSweeper"}) {
            String text = Files.readString(srcPath(src));
            assertThat(text).as(src + " must contain no delete call")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
    }

    // ── MANDATE-CONCURRENT-001 — task row PESSIMISTIC_WRITE finder; resolve gates on PENDING ──
    @Test @Tag("MANDATE-CONCURRENT-001")
    void violation_lockedTaskFinder_resolveGatesOnPending() throws Exception {
        Method locked = MandateRepository.class.getMethod("findTaskByIdForUpdate", java.util.UUID.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String svc = Files.readString(srcPath("MandateService"));
        for (String method : new String[]{"public MandateTask completeTask(", "public boolean resolveDeemed("}) {
            int start = svc.indexOf(method);
            assertThat(start).as(method + " must exist").isPositive();
            String body = svc.substring(start, svc.indexOf("\n    }", start));
            assertThat(body).as(method + " must take the task row lock").contains("findTaskByIdForUpdate");
        }
        assertThat(svc).as("completeTask resolves only a PENDING task").contains("!t.isPending()");
        assertThat(svc).as("resolveDeemed resolves only a PENDING task").contains("!t.isPending()");
    }

    // ── MANDATE-DEEMED-001 — the sweep drives the worker cross-bean, never same-bean self-invocation ──
    @Test @Tag("MANDATE-DEEMED-001")
    void violation_deemedSweep_drivesWorkerCrossBean_notSelfInvocation() throws Exception {
        // the worker is @Transactional on the service (the sole orchestrator)
        Method worker = MandateService.class.getMethod("resolveDeemed", java.util.UUID.class);
        assertThat(worker.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class))
            .as("resolveDeemed must be @Transactional (the proxied worker)").isTrue();
        // the sweeper calls the SERVICE (cross-bean), not a same-bean this.resolveDeemed(...)
        String sweeper = Files.readString(srcPath("MandateDeemedSweeper"));
        assertThat(sweeper).as("the @Scheduled poller drives the worker cross-bean")
            .contains("@Scheduled")
            .contains("service.resolveDeemed(")
            .doesNotContain("this.resolveDeemed(");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("MANDATE-FANOUT-001") @Tag("MANDATE-BATTERY-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V051__create_mandate.sql")) {
            assertThat(in).as("V051__create_mandate.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("issued_count > 0");
            assertThat(sql).contains("status <> 'SATISFIED' OR (satisfied_by IS NOT NULL AND satisfied_at IS NOT NULL)");
            assertThat(sql).contains("UNIQUE INDEX uq_mandate_task_seq");
            assertThat(sql).contains("(mandate_id, task_seq)");
            assertThat(sql).contains("UNIQUE INDEX uq_mandate_check_key");
            assertThat(sql).contains("(mandate_id, check_key)");
            assertThat(sql).contains("resolved_by IS NOT NULL AND resolved_at IS NOT NULL AND resolve_reason IS NOT NULL");
        }
    }

    private static Path srcPath(String simpleName) {
        return Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "mandate", simpleName + ".java");
    }
}
