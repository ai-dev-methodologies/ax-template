package com.ax.template.authblueprint.inputplausibility;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Records an implausible DATE submission in its OWN committed transaction — same rationale as
 * {@link RejectedAttemptRecorder}: the submit path's 422 rolls back the caller's transaction, so
 * the rejected-attempt audit row MUST commit independently ({@code REQUIRES_NEW}) or it would be
 * silently lost with the refusal. A separate bean deliberately, to avoid the Spring
 * self-invocation trap.
 */
@Component
public class DateRejectedAttemptRecorder {

    private final MemberWriter members;
    private final Clock clock;

    public DateRejectedAttemptRecorder(MemberWriter members, Clock clock) {
        this.members = members;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID channelId, Instant assertedAt, Instant referenceAt, DateRejectReason reason, String actor) {
        members.persist(new DateRejectedAttempt(UUID.randomUUID(), channelId, assertedAt, referenceAt,
            reason, actor, Instant.now(clock)));
    }
}
