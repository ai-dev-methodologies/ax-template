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

        // BACKLOG P3-56(c) — the declared graph is checked HERE, once, at class init.
        // This replaces a bare Java `assert` inside transition(), which was a no-op under a
        // production JVM (-da is the default): a FORWARD edge added later that WIDENS the
        // mutable-set would have produced no signal at all. See validateForwardEdgesMonotone.
        validateForwardEdgesMonotone(ALLOWED, REOPEN_EDGES);
    }

    /**
     * STATEMUTATION-MONOTONE-001, checked over the DECLARED graph rather than per call.
     *
     * <p>Every non-REOPEN (i.e. FORWARD) edge must tighten or keep the mutable-set
     * ({@link StateFieldPolicy#isMonotoneForward}); widening is legitimate only through a
     * REOPEN, which the catalog requires be recorded with a reason. Validating the tables at
     * class-initialisation is strictly stronger than the per-call check it replaces: it
     * covers EVERY declared edge, including ones no request and no test ever exercises, and
     * it fails at startup rather than on whichever unlucky request first walks the bad edge.
     * A widening FORWARD edge therefore cannot reach {@link #transition}.
     *
     * <p>Package-private and parameterised on the tables so the invariant is directly
     * testable against a deliberately-broken graph — an inline check over the private
     * constants could only ever be exercised on the (correct) real graph, which is how the
     * original `assert` came to be untested as well as disabled.
     *
     * @throws IllegalStateException naming the offending edge — a declaration defect in this
     *         class, not a client error, so it is deliberately not a
     *         {@link StateMutationException} (nothing a caller sends can cause it).
     */
    static void validateForwardEdgesMonotone(Map<FormState, Set<FormState>> allowed,
                                             Map<FormState, Set<FormState>> reopenEdges) {
        for (Map.Entry<FormState, Set<FormState>> edges : allowed.entrySet()) {
            FormState from = edges.getKey();
            Set<FormState> reopen = reopenEdges.getOrDefault(from, EnumSet.noneOf(FormState.class));
            for (FormState to : edges.getValue()) {
                if (reopen.contains(to)) {
                    continue;   // a recorded REOPEN is allowed to widen
                }
                if (!StateFieldPolicy.isMonotoneForward(from, to)) {
                    throw new IllegalStateException(
                        "FORWARD edge " + from + " → " + to + " widens the mutable-set from "
                        + StateFieldPolicy.mutableFields(from) + " to "
                        + StateFieldPolicy.mutableFields(to)
                        + " — declare it in REOPEN_EDGES (a recorded re-open) or remove it "
                        + "(STATEMUTATION-MONOTONE-001)");
                }
            }
        }
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
            // A FORWARD edge MUST tighten (or keep) the mutable-set — never widen
            // (STATEMUTATION-MONOTONE-001). Enforced over the whole declared graph by
            // validateForwardEdgesMonotone at class init (P3-56(c)), so a widening FORWARD
            // edge cannot reach this line: it is rejected before the first instance exists.
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
