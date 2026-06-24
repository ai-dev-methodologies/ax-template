package com.ax.template.authblueprint.quorumresolution;

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
 * VIOLATION proof for quorum-resolution-l0. Structural assertions a deliberate break
 * cannot pass silently — no Spring context required.
 *
 * <p>Spec item coverage:
 * QR-BALLOT-001 — immutable ballot rows (no public setters, all @Column(updatable=false))
 * QR-ELIG-002 — voter weight immutable
 * QR-POLICY-001 — policy columns frozen at open
 * QR-RESOLVE-005 — idempotent (UNIQUE on resolution entity)
 * QR-RESOLVE-006 — concurrent serialization via PESSIMISTIC_WRITE (source-level check)
 * QR-AUTHZ-001/002 — covered implicitly via SecurityConfig (not structural)
 */
@Tag("QUORUM")
class QuorumViolationProofTest {

    // ── QR-BALLOT-001 — ballot row fully append-only (no public setters, all @Column(updatable=false)) ──

    @Test @Tag("QR-BALLOT-001")
    void violation_ballotRowFullyAppendOnly() throws Exception {
        for (Method m : Ballot.class.getMethods()) {
            assertThat(m.getName()).as("Ballot must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "motionId", "voterId", "choice", "weightAtCast", "castAt"}) {
            Column col = Ballot.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("Ballot." + f + " must be immutable").isFalse();
        }
    }

    // ── QR-BALLOT-001 — resolution row fully append-only ──

    @Test @Tag("QR-BALLOT-001") @Tag("QR-RESOLVE-005")
    void violation_resolutionRowFullyAppendOnly() throws Exception {
        for (Method m : Resolution.class.getMethods()) {
            assertThat(m.getName()).as("Resolution must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "motionId", "outcome", "yesWeight", "noWeight",
                "abstainWeight", "castEligibleWeight", "totalEligibleWeight", "resolvedAt"}) {
            Column col = Resolution.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("Resolution." + f + " must be immutable").isFalse();
        }
    }

    // ── QR-ELIG-002 — eligible voter weight immutable ──

    @Test @Tag("QR-ELIG-002")
    void violation_eligibleVoterRowFullyAppendOnly() throws Exception {
        for (Method m : EligibleVoter.class.getMethods()) {
            assertThat(m.getName()).as("EligibleVoter must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "motionId", "voterId", "weight"}) {
            Column col = EligibleVoter.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("EligibleVoter." + f + " must be immutable").isFalse();
        }
    }

    // ── QR-BALLOT-002 / QR-TALLY-001 — double-vote UNIQUE declared on Ballot entity ──

    @Test @Tag("QR-BALLOT-002") @Tag("QR-TALLY-001")
    void violation_ballotUniqueMotionVoterDeclaredOnEntity() {
        jakarta.persistence.Table table = Ballot.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table).isNotNull();
        assertThat(table.uniqueConstraints()).isNotEmpty();
        boolean found = false;
        for (jakarta.persistence.UniqueConstraint uc : table.uniqueConstraints()) {
            if (java.util.Arrays.asList(uc.columnNames()).containsAll(
                    java.util.List.of("motion_id", "voter_id"))) {
                found = true;
            }
        }
        assertThat(found).as("Ballot must have UNIQUE(motion_id, voter_id)").isTrue();
    }

    // ── QR-RESOLVE-005 — resolution UNIQUE(motion_id) → idempotent ──

    @Test @Tag("QR-RESOLVE-005")
    void violation_resolutionUniqueMotionIdDeclaredOnEntity() {
        jakarta.persistence.Table table = Resolution.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table).isNotNull();
        assertThat(table.uniqueConstraints()).isNotEmpty();
        boolean found = false;
        for (jakarta.persistence.UniqueConstraint uc : table.uniqueConstraints()) {
            if (java.util.Arrays.asList(uc.columnNames()).contains("motion_id")) {
                found = true;
            }
        }
        assertThat(found).as("Resolution must have UNIQUE(motion_id)").isTrue();
    }

    // ── QR-POLICY-001 — motion state machine methods not public + @Version present ──

    @Test @Tag("QR-POLICY-001")
    void violation_motionMarkMethodsNotPublic_versionAnnotated() throws Exception {
        Method markTallying = Motion.class.getDeclaredMethod("markTallying");
        assertThat(Modifier.isPublic(markTallying.getModifiers()))
            .as("Motion.markTallying must not be public — state machine is the sole mutator")
            .isFalse();

        Method markResolved = Motion.class.getDeclaredMethod("markResolved");
        assertThat(Modifier.isPublic(markResolved.getModifiers()))
            .as("Motion.markResolved must not be public — state machine is the sole mutator")
            .isFalse();

        assertThat(Motion.class.getDeclaredField("version").isAnnotationPresent(Version.class))
            .as("Motion.version must carry @Version").isTrue();
    }

    // ── QR-POLICY-001 / QR-ELIG-002 — policy columns updatable=false on Motion ──

    @Test @Tag("QR-POLICY-001") @Tag("QR-ELIG-002")
    void violation_motionPolicyColumnsImmutable() throws Exception {
        for (String f : new String[]{"convenerId", "totalEligibleWeight", "ruleType",
                "thresholdNumerator", "thresholdDenominator", "quorumNumerator", "quorumDenominator",
                "abstentionMode", "tieBreakMode", "tieBreakVoterId", "createdAt"}) {
            Column col = Motion.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column on Motion").isNotNull();
            assertThat(col.updatable()).as("Motion." + f + " must be updatable=false").isFalse();
        }
    }

    // ── QR-RESOLVE-001 / QR-TALLY-003 — Outcome has NO_DECISION as a SEPARATE constant ──

    @Test @Tag("QR-RESOLVE-001") @Tag("QR-TALLY-003")
    void violation_outcomeHasNoDecisionSeparateFromRejected() {
        Outcome[] values = Outcome.values();
        assertThat(values).contains(Outcome.NO_DECISION, Outcome.REJECTED, Outcome.PASSED);
        assertThat(Outcome.NO_DECISION).isNotEqualTo(Outcome.REJECTED);
        // Resolution @Check allows all three
        Check check = Resolution.class.getAnnotation(Check.class);
        assertThat(check).isNotNull();
        assertThat(check.constraints()).contains("NO_DECISION");
        assertThat(check.constraints()).contains("REJECTED");
        assertThat(check.constraints()).contains("PASSED");
    }

    // ── QR-RESOLVE-004 — state machine has no RESOLVED → OPEN edge ──

    @Test @Tag("QR-RESOLVE-004")
    void violation_stateMachineHasNoResolvedToOpenEdge() {
        Motion motion = new Motion(java.util.UUID.randomUUID(), "convener", 10L,
            RuleType.MAJORITY, 1L, 2L, 1L, 2L,
            AbstentionMode.EXCLUDE_FROM_BASE, TieBreakMode.TIE_FAILS, null,
            java.time.Instant.now());
        motion.setStatus(MotionStatus.RESOLVED);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> MotionStateMachine.transition(motion, MotionStatus.OPEN))
            .as("RESOLVED → OPEN must be an illegal transition")
            .isInstanceOf(QuorumException.class);
    }

    // ── QR-RESOLVE-006 — resolve() and castBallot() go through PESSIMISTIC_WRITE lock ──

    @Test @Tag("QR-RESOLVE-006")
    void violation_resolveAndCastBallotUseTheLockedFinder() throws Exception {
        Method locked = MotionRepository.class.getMethod("findByIdForUpdate", java.util.UUID.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String src = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "quorumresolution", "QuorumService.java"));
        for (String method : new String[]{"public Ballot castBallot(", "public Resolution resolve("}) {
            int start = src.indexOf(method);
            assertThat(start).as(method + " must exist in QuorumService").isPositive();
            String body = src.substring(start, src.indexOf("\n    }", start));
            assertThat(body)
                .as(method + " must use findByIdForUpdate (PESSIMISTIC_WRITE lock)")
                .contains("findByIdForUpdate");
        }
    }

    // ── QR-POLICY-002 — V069 migration carries the same backstops as entity declarations ──

    @Test @Tag("QR-POLICY-002")
    void violation_migrationCarriesTheBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V069__create_quorum_resolution.sql")) {
            assertThat(in).as("V069__create_quorum_resolution.sql must exist on the classpath").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            // Double-vote unique constraint
            assertThat(sql).contains("uq_quorum_ballot_voter");
            // Resolution unique(motion_id)
            assertThat(sql).contains("uq_quorum_resolution_motion");
            // Outcome values
            assertThat(sql).contains("NO_DECISION");
            assertThat(sql).contains("REJECTED");
            assertThat(sql).contains("PASSED");
            // Quorum denominator > 0 check
            assertThat(sql).contains("quorum_denominator > 0");
            // Chair casting requires voter id
            assertThat(sql).contains("tie_break_voter_id IS NOT NULL");
        }
    }
}
