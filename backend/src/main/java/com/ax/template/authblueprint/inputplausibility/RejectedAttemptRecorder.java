package com.ax.template.authblueprint.inputplausibility;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Records an implausible (range- or rate-rejected) submission in its OWN committed transaction
 * (PLAUSIBILITY-REJECT-001). The submit path throws a 422 right after recording the rejection,
 * which rolls back the caller's transaction — so the rejected-attempt row MUST be written in a
 * {@code REQUIRES_NEW} transaction that commits independently, otherwise the audit record would
 * roll back with the refusal and the implausible attempt (a fraud/calibration signal) would be
 * silently lost — defeating the "rejected AND recorded" contract.
 *
 * <p>This is a SEPARATE bean (not a self-invoked method on {@link PlausibilityService}) deliberately:
 * a {@code REQUIRES_NEW} call to a sibling method on the same proxied bean would bypass the proxy
 * and run in the SAME transaction (the Spring self-invocation trap), defeating the whole point.
 */
@Component
public class RejectedAttemptRecorder {

    private final MemberWriter members;
    private final Clock clock;

    public RejectedAttemptRecorder(MemberWriter members, Clock clock) {
        this.members = members;
        this.clock = clock;
    }

    /** Persist the rejected attempt in an independent transaction that survives the caller's rollback. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID channelId, BigDecimal reportedValue, RejectReason reason,
                       BigDecimal priorValue, long elapsedSeconds, BigDecimal computedRate, String actor) {
        members.persist(new RejectedAttempt(UUID.randomUUID(), channelId, reportedValue, reason,
            priorValue, elapsedSeconds, computedRate, actor, Instant.now(clock)));
    }
}
