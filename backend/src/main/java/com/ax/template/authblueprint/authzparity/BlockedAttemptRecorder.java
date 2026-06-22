package com.ax.template.authblueprint.authzparity;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Records a refused (parity-mismatch) execution attempt in its OWN committed transaction
 * (AUTHZPARITY-EXEC-001). The execute path throws {@code PARITY_MISMATCH} right after detecting
 * the mismatch, which rolls back the caller's transaction — so the blocked-attempt row MUST be
 * written in a {@code REQUIRES_NEW} transaction that commits independently, otherwise the audit
 * record would roll back with the refusal and the violation would be silently lost.
 *
 * <p>This is a SEPARATE bean (not a self-invoked method on the service) deliberately: a
 * {@code REQUIRES_NEW} call to a sibling method on the same proxied bean would bypass the proxy
 * and run in the SAME transaction (the Spring self-invocation trap), defeating the whole point.
 */
@Component
public class BlockedAttemptRecorder {

    private final MemberWriter members;
    private final Clock clock;

    public BlockedAttemptRecorder(MemberWriter members, Clock clock) {
        this.members = members;
        this.clock = clock;
    }

    /** Persist the blocked attempt in an independent transaction that survives the caller's rollback. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID actionId, String offeredHash, String authorizedHash, String attemptedBy) {
        members.persist(new BlockedAttempt(UUID.randomUUID(), actionId, offeredHash, authorizedHash,
            attemptedBy, Instant.now(clock)));
    }
}
