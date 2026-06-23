package com.ax.template.authblueprint.recurringinterval;

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
 * VIOLATION proof for completion-reset-recurring-interval-l0. Structural assertions a deliberate
 * break cannot pass silently: occurrences are fully append-only with the exactly-once UNIQUE
 * backstop; the window/flag mutators are package-sealed and the entity exposes no public setter;
 * the create contract carries NO windowStart/due/nextDueAt INPUT field (due-ness is recomputed,
 * never accepted); the sweeper records only the non-authoritative flag, injects @Lazy self, and
 * NEVER completes/advances; all write paths use the PESSIMISTIC_WRITE finder; the migration carries
 * the same backstops — no Spring context.
 */
@Tag("RECURRINGINTERVAL")
class RecurringIntervalViolationProofTest {

    private static final Path SRC = Path.of(System.getProperty("user.dir"), "src", "main", "java",
        "com", "ax", "template", "authblueprint", "recurringinterval");

    private static String src(String file) throws Exception {
        return Files.readString(SRC.resolve(file));
    }

    // ── CRI-ONCE-001 — occurrences append-only + exactly-once UNIQUE backstop ──
    @Test @Tag("CRI-ONCE-001")
    void violation_occurrenceAppendOnly_uniquePerWindow() throws Exception {
        for (Method m : Occurrence.class.getMethods()) {
            assertThat(m.getName()).as("Occurrence must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "obligationId", "closedWindowStart", "completedBy", "completedAt"}) {
            Column col = Occurrence.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("Occurrence." + f + " must be immutable").isFalse();
        }
        jakarta.persistence.Table table = Occurrence.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()).isNotEmpty();
        assertThat(table.uniqueConstraints()[0].columnNames())
            .as("exactly-once is keyed on (obligation_id, closed_window_start)")
            .containsExactly("obligation_id", "closed_window_start");
    }

    // ── CRI-DUE-001 — no raw windowStart/due/nextDueAt INPUT; due-ness is recomputed ──
    @Test @Tag("CRI-DUE-001")
    void violation_createContractAcceptsNoWindowOrDueField() throws Exception {
        String controller = src("RecurringIntervalController.java");
        String createReq = controller.substring(controller.indexOf("record CreateReq"),
            controller.indexOf("record RecurringObligationDto"));
        assertThat(createReq)
            .as("the create contract must carry NO windowStart input — windows are derived")
            .doesNotContainIgnoringCase("windowstart");
        assertThat(createReq)
            .as("the create contract must carry NO due/nextDueAt/overdue input — due-ness is recomputed")
            .doesNotContainIgnoringCase("duedat")
            .doesNotContainIgnoringCase("nextdue")
            .doesNotContainIgnoringCase("overdue");

        // the authoritative overdue is recomputed in the service from the clock + windowStart
        String service = src("RecurringObligationService.java");
        assertThat(service)
            .as("isOverdue must be a recomputed predicate over nextDueAt, not a stored boolean read")
            .contains("isBefore(o.nextDueAt())");
    }

    // ── CRI-SWEEP-001 — the sweep records only the flag, injects @Lazy self, never completes ──
    @Test @Tag("CRI-SWEEP-001")
    void violation_sweepFlagsOnly_lazySelf_neverCompletesOrAdvances() throws Exception {
        String sweeper = src("RecurringIntervalSweeper.java");
        assertThat(sweeper)
            .as("the sweep must never complete or advance — only record the non-authoritative flag")
            .doesNotContain("completeAndAdvance(")
            .doesNotContain(".complete(");
        assertThat(sweeper)
            .as("the @Scheduled tick must call its worker through an injected @Lazy self proxy")
            .contains("@Lazy")
            .contains("self.sweepOne(");
        assertThat(sweeper)
            .as("the sweep is a concurrent mutator — it locks the row like the complete path")
            .contains("findByIdForUpdate");
        // the swept flag column is non-authoritative — the entity records it via a package-private hook
        Method recordHook = java.util.Arrays.stream(RecurringObligation.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("recordSweptOverdue")).findFirst().orElseThrow();
        assertThat(Modifier.isPublic(recordHook.getModifiers()))
            .as("recordSweptOverdue must be package-private (the sweeper is its only caller)").isFalse();
    }

    // ── CRI-RESET-001 / CRI-CONCURRENT-001 — mutators package-sealed; write paths use the locked finder ──
    @Test @Tag("CRI-RESET-001") @Tag("CRI-CONCURRENT-001")
    void violation_mutatorsSealed_andLockedFinderUsed() throws Exception {
        for (Method m : RecurringObligation.class.getMethods()) {
            assertThat(m.getName()).as("RecurringObligation must expose no public setter").doesNotStartWith("set");
        }
        for (String hook : new String[]{"completeAndAdvance", "recordSweptOverdue"}) {
            Method m = java.util.Arrays.stream(RecurringObligation.class.getDeclaredMethods())
                .filter(x -> x.getName().equals(hook)).findFirst().orElseThrow();
            assertThat(Modifier.isPublic(m.getModifiers()))
                .as("RecurringObligation." + hook + " must be package-private (service is the sole mutator)")
                .isFalse();
        }
        // intervalSeconds is immutable; version is the optimistic guard
        assertThat(RecurringObligation.class.getDeclaredField("intervalSeconds")
            .getAnnotation(Column.class).updatable())
            .as("intervalSeconds must be immutable").isFalse();
        assertThat(RecurringObligation.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();

        Check check = RecurringObligation.class.getAnnotation(Check.class);
        assertThat(check.constraints().replaceAll("\\s+", " ")).contains("interval_seconds > 0");

        String service = src("RecurringObligationService.java");
        int start = service.indexOf("public RecurringObligation complete(");
        assertThat(start).as("complete(...) must exist").isPositive();
        String body = service.substring(start, service.indexOf("\n    }", start));
        assertThat(body).as("complete must use the PESSIMISTIC_WRITE finder")
            .contains("findByObligationKeyForUpdate");
        assertThat(body).as("complete advances the window to the completion instant (reset, not grid)")
            .contains("completeAndAdvance(");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("CRI-ONCE-001") @Tag("CRI-RESET-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V063__create_recurringinterval.sql")) {
            assertThat(in).as("V063__create_recurringinterval.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("interval_seconds > 0");
            assertThat(sql).contains("UNIQUE INDEX uq_recurring_window");
            assertThat(sql).contains("(obligation_id, closed_window_start)");
        }
    }
}
