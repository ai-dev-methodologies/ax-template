package com.ax.template.authblueprint.duplicatesubmission;

import jakarta.persistence.Column;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for duplicate-submission-key-l0. Structural assertions a deliberate break
 * cannot pass silently: the identity/derivation columns are immutable, the release hook is
 * package-sealed (submission is its own sole mutator), the state machine has no edge back out of
 * WITHDRAWN/REJECTED, NO delete path exists anywhere in the domain, and the migration carries the
 * same UNIQUE(channel_id, active_key) backstop.
 */
@Tag("DUPLICATESUBMISSION")
class DuplicateSubmissionViolationProofTest {

    // ── DUPKEY-NATURAL-001 — identity/derivation columns are immutable; natural key is deterministic ──
    @Test @Tag("DUPKEY-NATURAL-001")
    void violation_submissionImmutableColumns_naturalKeyDeterministic() throws Exception {
        for (String f : new String[]{"id", "channelId", "subjectRef", "lossDate", "lossType", "naturalKey",
                "flaggedForReview", "suspectSubmissionId", "createdAt"}) {
            Column col = Submission.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("Submission." + f + " must be immutable").isFalse();
        }
        // activeKey is the ONLY mutable column (release() clears it) — asserted separately, not immutable.
        Column activeKey = Submission.class.getDeclaredField("activeKey").getAnnotation(Column.class);
        assertThat(activeKey.updatable()).as("activeKey must be mutable — release() clears it").isTrue();

        LocalDate d = LocalDate.of(2026, 1, 1);
        assertThat(Submission.deriveNaturalKey("s1", d, "TYPE")).isEqualTo("s1|2026-01-01|TYPE");
    }

    // ── DUPKEY-WITHDRAWN-003 — release() is package-private; submission is its own sole mutator ──
    @Test @Tag("DUPKEY-WITHDRAWN-003")
    void violation_releaseHookSealed_noDeletePath() throws Exception {
        Method release = Submission.class.getDeclaredMethod("release", SubmissionStatus.class);
        assertThat(Modifier.isPublic(release.getModifiers()))
            .as("Submission.release must be package-private").isFalse();

        for (Method m : SubmissionRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (Method m : DuplicateKeyChannelRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"DuplicateSubmissionService", "DuplicateSubmissionController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "duplicatesubmission", src + ".java"));
            assertThat(text).as(src + " must contain no delete call").doesNotContain(".delete(").doesNotContain("deleteBy");
        }
    }

    // ── DUPKEY-WITHDRAWN-003 — the state machine has NO edge back out of WITHDRAWN/REJECTED ──
    @Test @Tag("DUPKEY-WITHDRAWN-003")
    void violation_stateMachineHasNoReverseEdge() {
        SubmissionStateMachine sm = new SubmissionStateMachine();
        Submission withdrawn = new Submission(UUID.randomUUID(), UUID.randomUUID(), "s", LocalDate.now(),
            "T", false, null, Instant.now());
        sm.withdraw(withdrawn);
        assertThat(withdrawn.getStatus()).isEqualTo(SubmissionStatus.WITHDRAWN);
        assertThat(withdrawn.getActiveKey()).as("withdraw must release the key").isNull();

        assertThatIllegalTransition(() -> sm.withdraw(withdrawn));
        assertThatIllegalTransition(() -> sm.reject(withdrawn));

        Submission rejected = new Submission(UUID.randomUUID(), UUID.randomUUID(), "s2", LocalDate.now(),
            "T", false, null, Instant.now());
        sm.reject(rejected);
        assertThat(rejected.getStatus()).isEqualTo(SubmissionStatus.REJECTED);
        assertThat(rejected.getActiveKey()).isNull();
        assertThatIllegalTransition(() -> sm.withdraw(rejected));
    }

    private static void assertThatIllegalTransition(Runnable r) {
        try {
            r.run();
            org.junit.jupiter.api.Assertions.fail("expected DuplicateSubmissionException for an illegal transition");
        } catch (DuplicateSubmissionException expected) {
            assertThat(expected.code()).isEqualTo("SUBMISSION_ILLEGAL_TRANSITION");
        }
    }

    // ── DUPKEY-NATURAL-001 — channel config is immutable; @Check backstops the fuzzy window ──
    @Test @Tag("DUPKEY-NATURAL-001")
    void violation_channelConfigImmutable_checked() throws Exception {
        for (String f : new String[]{"id", "scopeLabel", "fuzzyWindowDays", "createdAt"}) {
            Column col = DuplicateKeyChannel.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col.updatable()).as("DuplicateKeyChannel." + f + " must be immutable").isFalse();
        }
        Check check = DuplicateKeyChannel.class.getAnnotation(Check.class);
        assertThat(check.constraints()).contains("fuzzy_window_days >= 0");
    }

    // ── the migration carries the same UNIQUE(channel_id, active_key) backstop ──
    @Test @Tag("DUPKEY-NATURAL-001")
    void violation_migrationCarriesTheUniqueBackstop() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V114__create_duplicate_submission.sql")) {
            assertThat(in).as("V114__create_duplicate_submission.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("UNIQUE (channel_id, active_key)");
            assertThat(sql).contains("(status = 'ACTIVE') = (active_key IS NOT NULL)");
        }
    }
}
