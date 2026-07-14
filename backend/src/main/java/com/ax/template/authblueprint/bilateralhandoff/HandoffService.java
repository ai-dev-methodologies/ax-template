package com.ax.template.authblueprint.bilateralhandoff;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * bilateral-handoff-l0 sole orchestrator. A handoff is PROPOSED between two named parties and
 * completes ONLY when BOTH have independently confirmed (BHO-FSM-001); either declining VOIDS it
 * terminally, discarding any prior partial confirmation (BHO-VOID-001). The confirming/declining
 * caller MUST be a named party (BHO-BIND-001, 403 fail-closed — a role mismatch, not an existence
 * leak); confirming a second time is idempotent. The custody-flip effect applies EXACTLY ONCE,
 * atomically, at the SECOND confirmation, under the handoff row's PESSIMISTIC_WRITE lock
 * (BHO-ATOMIC-001, CWE-362) — the second locker to run always observes the first locker's
 * committed confirmation before deciding whether both are now present.
 */
@Service
public class HandoffService {

    private final HandoffRepository handoffs;
    private final HandoffStateMachine sm;
    private final HandoffMetrics metrics;
    private final Clock clock;

    public HandoffService(HandoffRepository handoffs, HandoffStateMachine sm,
                          HandoffMetrics metrics, Clock clock) {
        this.handoffs = handoffs;
        this.sm = sm;
        this.metrics = metrics;
        this.clock = clock;
    }

    /** BHO-FSM-001 — propose a handoff between two named parties; custody starts at the releasor. */
    @Transactional
    public Handoff propose(String releasor, String receiver) {
        Handoff h = handoffs.save(new Handoff(UUID.randomUUID(), releasor, receiver, Instant.now(clock)));
        metrics.record("propose", "ok");
        return h;
    }

    /**
     * BHO-BIND/ATOMIC-001 — the caller confirms. Idempotent per-party; on the SECOND independent
     * confirmation, completes atomically (status → COMPLETED, custody → receiver) under the row lock.
     */
    @Transactional
    public Handoff confirm(UUID handoffId, String caller) {
        Handoff h = handoffs.findByIdForUpdate(handoffId).orElseThrow(HandoffException::notFound);
        if (!h.isParty(caller)) {
            metrics.record("confirm", "not_a_party");
            throw HandoffException.notAParty();                       // 403 — BHO-BIND-001
        }
        if (h.getStatus() == HandoffStatus.VOIDED) {
            metrics.record("confirm", "voided_late");
            throw HandoffException.voided();                          // 409 — BHO-VOID-001
        }
        if (h.getStatus() == HandoffStatus.COMPLETED) {
            metrics.record("confirm", "idempotent");                  // both already confirmed — no-op
            return h;
        }
        Instant now = Instant.now(clock);
        boolean isReleasor = caller.equals(h.getReleasorParty());
        if (isReleasor) {
            h.markReleasorConfirmed(now);                             // per-party idempotent
        } else {
            h.markReceiverConfirmed(now);
        }
        if (h.bothConfirmed()) {
            sm.complete(h);                                           // BHO-ATOMIC-001 — exactly once
            metrics.record("confirm", "completed");
        } else {
            metrics.record("confirm", "ok");
        }
        return h;
    }

    /** BHO-VOID-001 — either named party declines a PROPOSED handoff, voiding it terminally. */
    @Transactional
    public Handoff decline(UUID handoffId, String caller) {
        Handoff h = handoffs.findByIdForUpdate(handoffId).orElseThrow(HandoffException::notFound);
        if (!h.isParty(caller)) {
            metrics.record("decline", "not_a_party");
            throw HandoffException.notAParty();                       // 403 — BHO-BIND-001
        }
        if (h.getStatus() == HandoffStatus.VOIDED) {
            metrics.record("decline", "voided_late");
            throw HandoffException.voided();                          // 409 — already terminal
        }
        if (h.getStatus() == HandoffStatus.COMPLETED) {
            metrics.record("decline", "not_open");
            throw HandoffException.notOpen(h.getStatus().name());     // 409 — cannot decline a completed handoff
        }
        sm.voidHandoff(h);                                             // custody NEVER flips
        metrics.record("decline", "voided");
        return h;
    }

    @Transactional(readOnly = true)
    public Handoff get(UUID handoffId) {
        return handoffs.findById(handoffId).orElseThrow(HandoffException::notFound);
    }
}
