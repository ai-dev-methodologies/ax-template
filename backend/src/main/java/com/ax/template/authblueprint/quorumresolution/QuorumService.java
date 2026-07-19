package com.ax.template.authblueprint.quorumresolution;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * quorum-resolution-l0 sole orchestrator/mutator. Every use case that mutates
 * state acquires the Motion row under PESSIMISTIC_WRITE (QR-CONCURRENT-001).
 *
 * <p>CORRECTNESS TRAPS explicitly addressed here:
 * <ol>
 *   <li>Quorum is checked against TOTAL ELIGIBLE weight, not cast weight
 *       (QR-QUORUM-001). Using cast weight would always satisfy quorum — that is
 *       the #1 correctness bug.</li>
 *   <li>All threshold/quorum comparisons use INTEGER cross-multiplication —
 *       NEVER floating point (QR-INTEGER-001).</li>
 *   <li>Tie-break is deterministic from the frozen policy: scalars yes/no are
 *       compared; for CHAIR_CASTING the chair's ballot choice is the decider
 *       — never map iteration order (QR-TIEBREAK-001).</li>
 *   <li>ABSTAIN counts toward quorum participation (it IS a cast act) but its
 *       treatment in the threshold base depends on abstentionMode (QR-ABSTAIN-001).
 *       A non-vote (eligible but no ballot) counts toward neither.</li>
 *   <li>Policy + roster + totalEligibleWeight are FROZEN at open (all
 *       updatable=false). resolve() reads only the frozen columns — never
 *       recomputes totalEligibleWeight from a live roster (QR-FREEZE-001).</li>
 *   <li>resolve() is PURE and IDEMPOTENT: UNIQUE(motion_id) on Resolution prevents
 *       a second insert; the existing row is returned byte-identical (QR-IDEMPOTENT-001).</li>
 * </ol>
 */
@Service
public class QuorumService {

    static final int MAX_PAGE_SIZE = 200;

    private final MotionRepository motions;
    private final MemberWriter members;
    private final QuorumMetrics metrics;
    private final Clock clock;

    public QuorumService(MotionRepository motions, MemberWriter members,
                         QuorumMetrics metrics, Clock clock) {
        this.motions = motions;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    /**
     * Open a new motion — validate policy self-consistency, snapshot policy + roster +
     * totalEligibleWeight onto rows, status = OPEN.
     *
     * @param convenerId  the opening convener
     * @param policy      the frozen policy (validated here, stored immutably)
     * @param roster      the eligible voters with weights
     * @return the persisted Motion
     */
    @Transactional
    public Motion openMotion(String convenerId, PolicySnapshot policy, List<VoterEntry> roster) {
        validatePolicy(policy);

        long totalEligibleWeight = roster.stream().mapToLong(VoterEntry::weight).sum();

        Motion motion = motions.saveAndFlush(new Motion(
            UUID.randomUUID(), convenerId, totalEligibleWeight,
            policy.ruleType(), policy.thresholdNumerator(), policy.thresholdDenominator(),
            policy.quorumNumerator(), policy.quorumDenominator(),
            policy.abstentionMode(), policy.tieBreakMode(), policy.tieBreakVoterId(),
            Instant.now(clock)));

        for (VoterEntry entry : roster) {
            members.persist(new EligibleVoter(UUID.randomUUID(), motion.getId(),
                entry.voterId(), entry.weight()));
        }

        return motion;
    }

    /**
     * Cast a ballot. The motion row is locked PESSIMISTIC_WRITE for the duration.
     * Rejects: status != OPEN (409), caller not eligible (403), double-vote caught
     * by UNIQUE constraint (409). weight_at_cast is copied from the frozen voter weight.
     */
    @Transactional
    public Ballot castBallot(UUID motionId, String callerId, Choice choice) {
        Motion motion = motions.findByIdForUpdate(motionId)
            .orElseThrow(QuorumException::notFound);

        if (motion.getStatus() != MotionStatus.OPEN) {
            metrics.recordBallot("rejected");
            throw QuorumException.motionClosed();
        }

        EligibleVoter voter = motions.findEligibleVoter(motionId, callerId)
            .orElseThrow(() -> {
                metrics.recordBallot("rejected");
                return QuorumException.notEligible();
            });

        try {
            Ballot ballot = members.persistAndFlush(new Ballot(
                UUID.randomUUID(), motionId, callerId, choice,
                voter.getWeight(), Instant.now(clock)));
            metrics.recordBallot("ok");
            return ballot;
        } catch (DataIntegrityViolationException e) {
            metrics.recordBallot("rejected");
            throw QuorumException.doubleVote();
        }
    }

    /**
     * Close and tally. Only the convener may call this. PESSIMISTIC_WRITE on the motion row.
     * Idempotent: if already RESOLVED, returns the existing Resolution byte-identical
     * (UNIQUE(motion_id) prevents a second insert — QR-IDEMPOTENT-001).
     *
     * <p>Correctness: quorum is measured against TOTAL ELIGIBLE weight (not cast weight).
     * All comparisons are integer cross-multiplication. Tie-break uses scalar totals only,
     * never map iteration order.
     */
    @Transactional
    public Resolution resolve(UUID motionId, String callerId) {
        Motion motion = motions.findByIdForUpdate(motionId)
            .orElseThrow(QuorumException::notFound);

        if (!motion.getConvenerId().equals(callerId)) {
            throw QuorumException.notConvener();
        }

        // Idempotent: if already resolved, return the existing record
        if (motion.getStatus() == MotionStatus.RESOLVED) {
            return motions.findResolution(motionId)
                .orElseThrow(QuorumException::notFound);
        }

        // Advance state: OPEN → TALLYING → RESOLVED
        motion.markTallying();

        // Read all immutable ballots
        List<Ballot> ballotList = motions.findBallots(motionId);

        // Tally into scalars — NEVER a map (would introduce iteration-order non-determinism)
        long yesWeight = 0L;
        long noWeight = 0L;
        long abstainWeight = 0L;
        long castEligibleWeight = 0L;

        for (Ballot b : ballotList) {
            // Read ONLY the frozen weight_at_cast column (QR-FREEZE-001)
            long w = b.getWeightAtCast();
            castEligibleWeight += w;
            switch (b.getChoice()) {
                case YES -> yesWeight += w;
                case NO -> noWeight += w;
                case ABSTAIN -> abstainWeight += w;
            }
        }

        // ── Quorum check: measured against TOTAL ELIGIBLE weight, not cast weight ──
        // Integer cross-multiplication: castEligibleWeight * quorumDenominator >= quorumNumerator * totalEligibleWeight
        long totalEligibleWeight = motion.getTotalEligibleWeight();
        long quorumNum = motion.getQuorumNumerator();
        long quorumDen = motion.getQuorumDenominator();

        // Math.multiplyExact: exact integer arithmetic that FAILS CLOSED on overflow rather
        // than silently wrapping to a wrong decision (spec stack: "integer/BigDecimal exact").
        boolean quorumMet = Math.multiplyExact(castEligibleWeight, quorumDen)
            >= Math.multiplyExact(quorumNum, totalEligibleWeight);

        Outcome outcome;
        if (!quorumMet) {
            outcome = Outcome.NO_DECISION;
        } else {
            // ── Threshold check (exact integer cross-multiplication — NO floating point) ──
            // base depends on the FROZEN abstentionMode.
            long base;
            if (motion.getAbstentionMode() == AbstentionMode.COUNT_AS_NO) {
                base = yesWeight + noWeight + abstainWeight;   // abstentions fold into the base as effective NO
            } else {
                base = yesWeight + noWeight;                    // EXCLUDE_FROM_BASE
            }

            long threshNum = motion.getThresholdNumerator();
            long threshDen = motion.getThresholdDenominator();

            // The DEADLOCK that warrants a tie-break is threshold EQUALITY — yes is exactly AT the
            // bar — NOT yes==no (which only coincides with the bar for a 1/2 majority and would
            // spuriously flip a 1/3 or 2/3 motion that cleared its threshold). A motion PASSES iff
            // it strictly EXCEEDS the threshold; exactly meeting it is resolved by the frozen
            // tie_break_mode (QR-RESOLVE-003).
            long lhs = Math.multiplyExact(yesWeight, threshDen);
            long rhs = Math.multiplyExact(threshNum, base);

            if (lhs > rhs) {
                outcome = Outcome.PASSED;
            } else if (lhs < rhs) {
                outcome = Outcome.REJECTED;
            } else {
                // exactly at the threshold — a deadlock resolved by the frozen policy
                outcome = resolveTie(motion, motionId);
            }
        }

        motion.markResolved();

        Resolution resolution = members.persistAndFlush(new Resolution(
            UUID.randomUUID(), motionId, outcome,
            yesWeight, noWeight, abstainWeight,
            castEligibleWeight, totalEligibleWeight, Instant.now(clock)));

        metrics.recordResolution(outcome.name().toLowerCase(Locale.ROOT));
        return resolution;
    }

    /**
     * Resolve an exact tie using the frozen tie_break_mode (deterministic — never
     * map/iteration order). Uses scalar yes/no totals already computed by the caller.
     */
    private Outcome resolveTie(Motion motion, UUID motionId) {
        return switch (motion.getTieBreakMode()) {
            case TIE_FAILS -> Outcome.REJECTED;
            case CHAIR_CASTING -> {
                // The chair's ballot choice decides — look it up by the frozen tieBreakVoterId
                String chairId = motion.getTieBreakVoterId();
                Ballot chairBallot = motions.findBallot(motionId, chairId).orElse(null);
                if (chairBallot == null || chairBallot.getChoice() == Choice.ABSTAIN) {
                    // Chair did not vote or abstained — tie fails
                    yield Outcome.REJECTED;
                }
                yield chairBallot.getChoice() == Choice.YES ? Outcome.PASSED : Outcome.REJECTED;
            }
        };
    }

    @Transactional(readOnly = true)
    public Motion getMotion(UUID motionId) {
        return motions.findById(motionId).orElseThrow(QuorumException::notFound);
    }

    @Transactional(readOnly = true)
    public Page<Ballot> getBallots(UUID motionId, String caller, int page, int size) {
        Motion motion = motions.findById(motionId).orElseThrow(QuorumException::notFound);
        // Per-voter ballots are confidential — only the convener reads them; others get an
        // IDOR-safe 404 indistinguishable from a non-existent motion (QR-AUTHZ-002 posture).
        if (!motion.getConvenerId().equals(caller)) {
            throw QuorumException.notConvener();
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return motions.findBallotsPage(motionId, PageRequest.of(safePage, safeSize));
    }

    private void validatePolicy(PolicySnapshot policy) {
        if (policy.thresholdNumerator() < 0 || policy.thresholdDenominator() <= 0) {
            throw QuorumException.policyInvalid("threshold fraction must have numerator>=0 and denominator>0");
        }
        if (policy.quorumNumerator() < 0 || policy.quorumDenominator() <= 0) {
            throw QuorumException.policyInvalid("quorum fraction must have numerator>=0 and denominator>0");
        }
        if (policy.thresholdNumerator() > policy.thresholdDenominator()) {
            throw QuorumException.policyInvalid("threshold numerator cannot exceed denominator");
        }
        if (policy.quorumNumerator() > policy.quorumDenominator()) {
            throw QuorumException.policyInvalid("quorum numerator cannot exceed denominator");
        }
        if (policy.tieBreakMode() == TieBreakMode.CHAIR_CASTING
            && (policy.tieBreakVoterId() == null || policy.tieBreakVoterId().isBlank())) {
            throw QuorumException.policyInvalid("CHAIR_CASTING requires a non-blank tieBreakVoterId");
        }
    }

    /** Value-type for the frozen policy snapshot passed to openMotion. */
    public record PolicySnapshot(RuleType ruleType,
                                  long thresholdNumerator, long thresholdDenominator,
                                  long quorumNumerator, long quorumDenominator,
                                  AbstentionMode abstentionMode,
                                  TieBreakMode tieBreakMode,
                                  String tieBreakVoterId) {}

    /** Value-type for roster entries. */
    public record VoterEntry(String voterId, long weight) {}
}
