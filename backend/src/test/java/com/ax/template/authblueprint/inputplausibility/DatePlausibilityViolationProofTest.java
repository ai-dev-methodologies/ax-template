package com.ax.template.authblueprint.inputplausibility;

import jakarta.persistence.Column;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for PLAUSIBILITY-DATE-RANGE/FUTURE-001 (BACKLOG P3-16). A deliberate break
 * cannot pass silently: the reading/rejected-attempt rows are fully immutable, the channel's
 * @Check backstops a non-negative window, NO delete path exists, the reason enum carries only
 * the one date-range verdict, and the submit path uses the injected Clock — never wall-clock —
 * for the reference instant.
 */
@Tag("INPUTPLAUSIBILITY")
class DatePlausibilityViolationProofTest {

    // ── PLAUSIBILITY-DATE-RANGE-001 — accepted reading is immutable + @Check pins unverified ──
    @Test @Tag("PLAUSIBILITY-DATE-RANGE-001")
    void violation_readingImmutable_checkPinsUnverified() throws Exception {
        for (Method m : DatePlausibilityReading.class.getMethods()) {
            assertThat(m.getName()).as("DatePlausibilityReading must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "channelId", "assertedAt", "referenceAt", "verificationStatus", "actor", "occurredAt"}) {
            Column col = DatePlausibilityReading.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("DatePlausibilityReading." + f + " must be immutable").isFalse();
        }
        Check check = DatePlausibilityReading.class.getAnnotation(Check.class);
        assertThat(check.constraints()).contains("verification_status = 'SELF_REPORTED_UNVERIFIED'");
    }

    // ── PLAUSIBILITY-DATE-RANGE-001 — rejected attempt is immutable; reason has exactly one verdict ──
    @Test @Tag("PLAUSIBILITY-DATE-RANGE-001")
    void violation_rejectedAttemptImmutable_singleReason() throws Exception {
        for (Method m : DateRejectedAttempt.class.getMethods()) {
            assertThat(m.getName()).as("DateRejectedAttempt must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "channelId", "assertedAt", "referenceAt", "reason", "actor", "occurredAt"}) {
            Column col = DateRejectedAttempt.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col.updatable()).as("DateRejectedAttempt." + f + " must be immutable").isFalse();
        }
        assertThat(DateRejectReason.values()).containsExactly(DateRejectReason.IMPLAUSIBLE_DATE_RANGE);
    }

    // ── PLAUSIBILITY-DATE-FUTURE-001 — channel @Check backstops a non-negative window; config immutable ──
    @Test @Tag("PLAUSIBILITY-DATE-FUTURE-001")
    void violation_channelWindowChecked_configImmutable() throws Exception {
        Check check = DatePlausibilityChannel.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("max_lookback_seconds >= 0");
        assertThat(c).contains("max_lookahead_seconds >= 0");
        for (String f : new String[]{"id", "subjectRef", "maxLookbackSeconds", "maxLookaheadSeconds", "createdAt"}) {
            Column col = DatePlausibilityChannel.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col.updatable()).as("DatePlausibilityChannel." + f + " must be immutable").isFalse();
        }
    }

    // ── PLAUSIBILITY-DATE-FUTURE-001 — the gate uses the injected Clock, never wall-clock; NO delete path ──
    @Test @Tag("PLAUSIBILITY-DATE-FUTURE-001")
    void violation_gateUsesInjectedClock_noWallClock_noDeletePath() throws Exception {
        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "inputplausibility", "DatePlausibilityService.java"));
        assertThat(svc).as("DatePlausibilityService must take java.time.Clock").contains("Clock clock");
        assertThat(svc).as("the reference instant must come from the injected Clock")
            .contains("Instant.now(clock)");
        assertThat(svc).as("the gate must never read wall-clock time directly")
            .doesNotContain("Instant.now()").doesNotContain("System.currentTimeMillis()");
        assertThat(svc).doesNotContain(".delete(").doesNotContain("deleteBy");

        // the rejection is recorded BEFORE the 422 throw, in an independently-committed transaction
        int rej = svc.indexOf("rejections.record(c.getId(), assertedAt, referenceAt, DateRejectReason.IMPLAUSIBLE_DATE_RANGE");
        assertThat(rej).as("the date-range rejection must be recorded before the throw").isPositive();

        String rec = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "inputplausibility", "DateRejectedAttemptRecorder.java"));
        assertThat(rec).as("the recorder must use REQUIRES_NEW so it survives the caller rollback")
            .contains("Propagation.REQUIRES_NEW");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("PLAUSIBILITY-DATE-RANGE-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V113__create_date_plausibility.sql")) {
            assertThat(in).as("V113__create_date_plausibility.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("max_lookback_seconds >= 0 AND max_lookahead_seconds >= 0");
            assertThat(sql).contains("verification_status = 'SELF_REPORTED_UNVERIFIED'");
            assertThat(sql).contains("CREATE TABLE date_plausibility_rejected_attempts");
        }
    }
}
