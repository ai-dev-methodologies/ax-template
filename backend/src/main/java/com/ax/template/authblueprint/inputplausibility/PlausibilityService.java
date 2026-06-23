package com.ax.template.authblueprint.inputplausibility;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * self-reported-input-plausibility-l0 sole orchestrator. A self-reported value the server cannot
 * verify is neither blindly trusted nor silently dropped: each submission takes the channel's
 * PESSIMISTIC_WRITE row lock (PLAUSIBILITY-CONCURRENT-001), runs the deterministic RANGE gate
 * (PLAUSIBILITY-RANGE-001) and — when a prior accepted reading exists — the RATE-OF-CHANGE gate
 * (PLAUSIBILITY-RATE-001). A passing submission is appended as a {@link PlausibilityReading} marked
 * SELF_REPORTED_UNVERIFIED with its full basis (PLAUSIBILITY-PROVENANCE-001) and advances the
 * channel's prior pointer. A failing submission is recorded as an immutable {@link RejectedAttempt}
 * BEFORE the 422 is returned (PLAUSIBILITY-REJECT-001) and never touches the accepted state. The
 * bounds and rate ceiling are a fork-receiver swap behind the governance contract. Reading and
 * attempt rows are members: {@link MemberWriter} writes, root-JPQL reads.
 */
@Service
public class PlausibilityService {

    static final String CHECKS_RANGE = "RANGE";
    static final String CHECKS_RANGE_RATE = "RANGE,RATE";
    /** Rate-computation precision — generous enough that the gate decision is never precision-bound. */
    private static final MathContext RATE_MC = new MathContext(34, RoundingMode.HALF_UP);

    private final PlausibilityChannelRepository channels;
    private final MemberWriter members;
    private final RejectedAttemptRecorder rejections;
    private final PlausibilityMetrics metrics;
    private final Clock clock;

    public PlausibilityService(PlausibilityChannelRepository channels, MemberWriter members,
                               RejectedAttemptRecorder rejections, PlausibilityMetrics metrics, Clock clock) {
        this.channels = channels;
        this.members = members;
        this.rejections = rejections;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public PlausibilityChannel define(String subjectRef, BigDecimal minValue, BigDecimal maxValue,
                                      BigDecimal maxDeltaPerSecond) {
        if (subjectRef == null || subjectRef.isBlank()) {
            metrics.record("define", "invalid");
            throw PlausibilityException.invalidChannel("subjectRef must not be blank");
        }
        if (minValue.compareTo(maxValue) > 0) {
            metrics.record("define", "invalid");
            throw PlausibilityException.invalidChannel("minValue must be <= maxValue");
        }
        if (maxDeltaPerSecond.signum() < 0) {
            metrics.record("define", "invalid");
            throw PlausibilityException.invalidChannel("maxDeltaPerSecond must be >= 0");
        }
        PlausibilityChannel c = new PlausibilityChannel(UUID.randomUUID(), subjectRef, minValue,
            maxValue, maxDeltaPerSecond, Instant.now(clock));
        PlausibilityChannel saved = channels.save(c);
        metrics.record("define", "ok");
        return saved;
    }

    /**
     * PLAUSIBILITY-RANGE/RATE/PROVENANCE/REJECT/CONCURRENT-001 — plausibility-gate one self-reported
     * submission. Under the channel's row lock: the RANGE gate runs first; the RATE gate runs only
     * when a prior accepted reading exists. A failing gate records a {@link RejectedAttempt} then
     * throws 422 — the channel's accepted state is untouched. A passing submission appends a
     * SELF_REPORTED_UNVERIFIED reading with its basis and advances the prior pointer.
     */
    @Transactional
    public PlausibilityReading submit(UUID channelId, BigDecimal reportedValue, String actor) {
        PlausibilityChannel c = channels.findByIdForUpdate(channelId)
            .orElseThrow(PlausibilityException::notFound);
        Instant now = Instant.now(clock);

        boolean hadPrior = c.hasPrior();
        long elapsedSeconds = hadPrior ? elapsedSeconds(c.getPriorAt(), now) : 0L;

        // ── RANGE gate (always) ──
        if (!c.inRange(reportedValue)) {
            rejections.record(c.getId(), reportedValue, RejectReason.IMPLAUSIBLE_RANGE,
                hadPrior ? c.getPriorValue() : null, elapsedSeconds, null, actor);
            metrics.record("submit", "implausible_range");
            throw PlausibilityException.implausibleRange();
        }

        // ── RATE gate (only with a prior basis) ──
        BigDecimal computedRate = null;
        String checksRan = CHECKS_RANGE;
        if (hadPrior) {
            checksRan = CHECKS_RANGE_RATE;
            BigDecimal delta = reportedValue.subtract(c.getPriorValue()).abs();
            boolean rateExceeded = exceedsRate(delta, elapsedSeconds, c.getMaxDeltaPerSecond());
            // computedRate is null only for an infinite (zero-elapsed, non-zero-delta) jump.
            computedRate = (elapsedSeconds == 0L)
                ? null
                : delta.divide(BigDecimal.valueOf(elapsedSeconds), RATE_MC);
            if (rateExceeded) {
                rejections.record(c.getId(), reportedValue, RejectReason.IMPLAUSIBLE_RATE,
                    c.getPriorValue(), elapsedSeconds, computedRate, actor);
                metrics.record("submit", "implausible_rate");
                throw PlausibilityException.implausibleRate();
            }
        }

        // ── accepted — append the unverified reading + advance the prior pointer ──
        PlausibilityReading reading = members.persist(new PlausibilityReading(UUID.randomUUID(),
            c.getId(), reportedValue, checksRan, hadPrior, hadPrior ? c.getPriorValue() : null,
            elapsedSeconds, computedRate, actor, now));
        c.recordAccepted(reportedValue, now);
        metrics.record("submit", "accepted");
        return reading;
    }

    @Transactional(readOnly = true)
    public PlausibilityChannel get(UUID channelId) {
        return channels.findById(channelId).orElseThrow(PlausibilityException::notFound);
    }

    @Transactional(readOnly = true)
    public List<PlausibilityReading> readings(UUID channelId) {
        get(channelId);                                  // 404 before an empty list
        return channels.findReadings(channelId);
    }

    @Transactional(readOnly = true)
    public List<RejectedAttempt> rejectedAttempts(UUID channelId) {
        get(channelId);
        return channels.findRejectedAttempts(channelId);
    }

    /** PLAUSIBILITY-RATE-001 — is |delta| / elapsed greater than the configured max delta-per-second?
     *  A zero-elapsed jump with a non-zero delta is an INFINITE rate (always exceeds). A zero delta
     *  is never exceeding. Compared without division (delta vs max*elapsed) to avoid divide-by-zero. */
    private static boolean exceedsRate(BigDecimal delta, long elapsedSeconds, BigDecimal maxDeltaPerSecond) {
        if (delta.signum() == 0) {
            return false;                                // no movement — always plausible
        }
        if (elapsedSeconds == 0L) {
            return true;                                 // instantaneous change of a non-zero amount
        }
        BigDecimal allowed = maxDeltaPerSecond.multiply(BigDecimal.valueOf(elapsedSeconds));
        return delta.compareTo(allowed) > 0;             // |delta| > max * elapsed  ⇔  rate > max
    }

    /** Whole seconds from the prior reading's instant to now (never negative — Clock is monotone in test). */
    private static long elapsedSeconds(Instant priorAt, Instant now) {
        long secs = Duration.between(priorAt, now).getSeconds();
        return Math.max(secs, 0L);
    }
}
