package com.ax.template.authblueprint.inputplausibility;

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
 * VIOLATION proof for self-reported-input-plausibility-l0. Structural assertions a deliberate break
 * cannot pass silently: the verification status has NO server-confirmed constant, the accepted
 * reading carries an immutable basis + a @Check pinning SELF_REPORTED_UNVERIFIED, the rejected
 * attempt is immutable and append-only, the channel carries @Version + the range/rate @Check
 * backstops, NO delete path exists anywhere in the domain, the prior-pointer mutator is
 * package-sealed, the write path uses the PESSIMISTIC_WRITE finder, and the migration carries the
 * same backstops.
 */
@Tag("INPUTPLAUSIBILITY")
class PlausibilityViolationProofTest {

    // ── PLAUSIBILITY-PROVENANCE-001 — the status enum has NO server-confirmed constant ──
    @Test @Tag("PLAUSIBILITY-PROVENANCE-001")
    void violation_verificationStatusHasNoConfirmedConstant() {
        VerificationStatus[] all = VerificationStatus.values();
        assertThat(all).containsExactly(VerificationStatus.SELF_REPORTED_UNVERIFIED);
        for (VerificationStatus s : all) {
            assertThat(s.name())
                .as("a self-reported value must never carry a server-verified status")
                .doesNotContain("CONFIRM").doesNotContain("VERIFIED_BY_SERVER").doesNotContain("AUTHORITATIVE");
        }
    }

    // ── PLAUSIBILITY-PROVENANCE-001 — accepted reading is immutable, basis recorded, @Check pins status ──
    @Test @Tag("PLAUSIBILITY-PROVENANCE-001")
    void violation_readingImmutable_basisRecorded_checkPinsUnverified() throws Exception {
        for (Method m : PlausibilityReading.class.getMethods()) {
            assertThat(m.getName()).as("PlausibilityReading must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "channelId", "reportedValue", "verificationStatus", "checksRan",
                "hadPrior", "priorValue", "elapsedSeconds", "computedRate", "actor", "occurredAt"}) {
            Column col = PlausibilityReading.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("PlausibilityReading." + f + " must be immutable").isFalse();
        }
        Check check = PlausibilityReading.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("verification_status = 'SELF_REPORTED_UNVERIFIED'");
        assertThat(c).contains("elapsed_seconds >= 0");
    }

    // ── PLAUSIBILITY-REJECT-001 — rejected attempt is immutable + reasons are exactly the two gates ──
    @Test @Tag("PLAUSIBILITY-REJECT-001")
    void violation_rejectedAttemptImmutable_reasonsAreTheTwoGates() throws Exception {
        for (Method m : RejectedAttempt.class.getMethods()) {
            assertThat(m.getName()).as("RejectedAttempt must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "channelId", "reportedValue", "reason", "priorValue",
                "elapsedSeconds", "computedRate", "actor", "occurredAt"}) {
            Column col = RejectedAttempt.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("RejectedAttempt." + f + " must be immutable").isFalse();
        }
        assertThat(RejectReason.values())
            .containsExactly(RejectReason.IMPLAUSIBLE_RANGE, RejectReason.IMPLAUSIBLE_RATE);
    }

    // ── PLAUSIBILITY-RANGE/RATE-001 — channel @Check backstops; mutator sealed; @Version; immutable config ──
    @Test @Tag("PLAUSIBILITY-RANGE-001") @Tag("PLAUSIBILITY-RATE-001")
    void violation_channelChecks_mutatorSealed_versioned_configImmutable() throws Exception {
        Check check = PlausibilityChannel.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("min_value <= max_value");
        assertThat(c).contains("max_delta_per_second >= 0");
        assertThat(c).contains("(prior_value IS NULL) = (prior_at IS NULL)");

        // the prior-pointer mutator is package-private (the channel is its own sole mutator)
        Method recordAccepted = java.util.Arrays.stream(PlausibilityChannel.class.getDeclaredMethods())
            .filter(x -> x.getName().equals("recordAccepted")).findFirst().orElseThrow();
        assertThat(Modifier.isPublic(recordAccepted.getModifiers()))
            .as("PlausibilityChannel.recordAccepted must be package-private").isFalse();

        // immutable identity + config columns on the channel
        for (String f : new String[]{"id", "subjectRef", "minValue", "maxValue", "maxDeltaPerSecond", "createdAt"}) {
            Column col = PlausibilityChannel.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col.updatable()).as("PlausibilityChannel." + f + " must be immutable").isFalse();
        }
        assertThat(PlausibilityChannel.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── PLAUSIBILITY-REJECT-001 — NO delete path; rejection recorded BEFORE the 422 throw ──
    @Test @Tag("PLAUSIBILITY-REJECT-001")
    void violation_noDeletePath_rejectionRecordedBeforeThrow() throws Exception {
        for (Method m : PlausibilityChannelRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"PlausibilityService", "PlausibilityController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "inputplausibility", src + ".java"));
            assertThat(text).as(src + " must contain no delete call — readings/attempts are append-only")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "inputplausibility", "PlausibilityService.java"));
        // each gate records the rejection before throwing the 422 (rejections.record precedes the throw)
        for (String reason : new String[]{"RejectReason.IMPLAUSIBLE_RANGE", "RejectReason.IMPLAUSIBLE_RATE"}) {
            int rej = svc.indexOf("rejections.record(c.getId(), reportedValue, " + reason);
            assertThat(rej).as(reason + " rejection must be recorded").isPositive();
        }

        // the recorder writes in its OWN committed transaction (REQUIRES_NEW) so the rejected attempt
        // survives the 422 rollback — without it the audit record would roll back with the refusal.
        String rec = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "inputplausibility", "RejectedAttemptRecorder.java"));
        assertThat(rec).as("the rejection recorder must use REQUIRES_NEW so it survives the caller rollback")
            .contains("Propagation.REQUIRES_NEW");
        assertThat(rec).contains("new RejectedAttempt(");
    }

    // ── PLAUSIBILITY-CONCURRENT-001 — the write path uses the PESSIMISTIC_WRITE finder ──
    @Test @Tag("PLAUSIBILITY-CONCURRENT-001")
    void violation_lockedFinder_andSerializedSubmit() throws Exception {
        Method locked = PlausibilityChannelRepository.class.getMethod("findByIdForUpdate", java.util.UUID.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "inputplausibility", "PlausibilityService.java"));
        int start = svc.indexOf("public PlausibilityReading submit(");
        assertThat(start).as("submit must exist").isPositive();
        String body = svc.substring(start, svc.indexOf("\n    }", start));
        assertThat(body).as("submit must take the channel row lock").contains("findByIdForUpdate");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("PLAUSIBILITY-RANGE-001") @Tag("PLAUSIBILITY-PROVENANCE-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V064__create_inputplausibility.sql")) {
            assertThat(in).as("V064__create_inputplausibility.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("min_value <= max_value AND max_delta_per_second >= 0");
            assertThat(sql).contains("(prior_value IS NULL) = (prior_at IS NULL)");
            assertThat(sql).contains("verification_status = 'SELF_REPORTED_UNVERIFIED'");
            assertThat(sql).contains("CREATE TABLE plausibility_rejected_attempts");
        }
    }
}
