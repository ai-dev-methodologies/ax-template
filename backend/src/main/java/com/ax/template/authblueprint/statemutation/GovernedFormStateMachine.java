package com.ax.template.authblueprint.statemutation;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sole mutator of {@link GovernedForm#getState()} + {@link GovernedForm#getLockedAt()}
 * (STATEMUTATION-MONOTONE-001). The declared legal-edge graph:
 * <pre>
 *   DRAFT     → SUBMITTED            (FORWARD — tightens)
 *   SUBMITTED → APPROVED             (FORWARD — tightens)
 *   APPROVED  → LOCKED               (FORWARD — tightens, terminal)
 *   SUBMITTED → DRAFT                (REOPEN  — widens, RECORDED with a reason)
 *   APPROVED  → DRAFT                (REOPEN  — widens, RECORDED with a reason)
 *   LOCKED    → (nothing)            terminal — no re-open
 * </pre>
 * Every transition appends an immutable {@link FormTransition} (root-JPQL reads,
 * {@link MemberWriter} writes). A FORWARD edge is asserted to be monotone-tightening against
 * {@link StateFieldPolicy}; a widening is legitimate ONLY through a REOPEN, which the catalog
 * requires be recorded — never a silent unlock. Mirrors the {@code ApprovalRequestStateMachine}
 * sole-mutator pattern adopted across the catalog.
 */
@Component
public class GovernedFormStateMachine {

    static final String KIND_FORWARD = "FORWARD";
    static final String KIND_REOPEN = "REOPEN";

    /** The full declared edge graph (forward + reopen) used to reject an illegal edge. */
    private static final Map<FormState, Set<FormState>> ALLOWED;
    /** The reopen (widening) edges — every edge here is a recorded REOPEN, the rest are FORWARD. */
    private static final Map<FormState, Set<FormState>> REOPEN_EDGES;
    static {
        ALLOWED = new EnumMap<>(FormState.class);
        ALLOWED.put(FormState.DRAFT, EnumSet.of(FormState.SUBMITTED));
        ALLOWED.put(FormState.SUBMITTED, EnumSet.of(FormState.APPROVED, FormState.DRAFT));
        ALLOWED.put(FormState.APPROVED, EnumSet.of(FormState.LOCKED, FormState.DRAFT));
        ALLOWED.put(FormState.LOCKED, EnumSet.noneOf(FormState.class));   // terminal

        REOPEN_EDGES = new EnumMap<>(FormState.class);
        REOPEN_EDGES.put(FormState.SUBMITTED, EnumSet.of(FormState.DRAFT));
        REOPEN_EDGES.put(FormState.APPROVED, EnumSet.of(FormState.DRAFT));
    }

    private final MemberWriter members;
    private final Clock clock;

    public GovernedFormStateMachine(MemberWriter members, Clock clock) {
        this.members = members;
        this.clock = clock;
    }

    /** Apply a transition to {@code to}, recording an immutable {@link FormTransition} at {@code seq}.
     *  @param reason mandatory for a REOPEN (widening); ignored for a FORWARD edge.
     *  Throws {@link StateMutationException#illegalTransition} for an undeclared edge,
     *  {@link StateMutationException#reopenReasonRequired} for a blank re-open reason. */
    public void transition(GovernedForm form, FormState to, String reason, String actor, long seq) {
        FormState from = form.getState();
        Set<FormState> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(FormState.class));
        if (!allowed.contains(to)) {
            throw StateMutationException.illegalTransition(from, to);
        }
        boolean reopen = REOPEN_EDGES.getOrDefault(from, EnumSet.noneOf(FormState.class)).contains(to);
        String kind;
        if (reopen) {
            if (reason == null || reason.isBlank()) {
                throw StateMutationException.reopenReasonRequired();
            }
            kind = KIND_REOPEN;
        } else {
            // A FORWARD edge MUST tighten (or keep) the mutable-set — never widen (STATEMUTATION-MONOTONE-001).
            assert StateFieldPolicy.isMonotoneForward(from, to)
                : "FORWARD edge " + from + " → " + to + " must not widen the mutable-set";
            kind = KIND_FORWARD;
        }
        Instant now = Instant.now(clock);
        members.persistAndFlush(new FormTransition(UUID.randomUUID(), form.getId(), seq,
            from, to, kind, reopen ? reason : null, actor, now));
        form.setState(to);
        if (to == FormState.LOCKED) {
            form.markLocked(now);
        }
    }
}
