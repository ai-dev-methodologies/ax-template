package com.ax.template.authblueprint.bilateralhandoff;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Sole mutator of {@link Handoff#getStatus()} (BHO-FSM-001). PROPOSED is the only non-terminal
 * state; COMPLETED/VOIDED are both terminal (zero outgoing edges) — {@link HandoffService} never
 * calls {@link Handoff#complete()} / {@link Handoff#voidHandoff()} directly.
 */
@Component
public class HandoffStateMachine {

    private static final Map<HandoffStatus, Set<HandoffStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(HandoffStatus.class);
        ALLOWED.put(HandoffStatus.PROPOSED, EnumSet.of(HandoffStatus.COMPLETED, HandoffStatus.VOIDED));
        ALLOWED.put(HandoffStatus.COMPLETED, EnumSet.noneOf(HandoffStatus.class));
        ALLOWED.put(HandoffStatus.VOIDED, EnumSet.noneOf(HandoffStatus.class));
    }

    /** BHO-ATOMIC-001 — status → COMPLETED, custody flips, in the same write. Caller must have
     *  already verified {@link Handoff#bothConfirmed()}. */
    public void complete(Handoff h) {
        assertTransition(h.getStatus(), HandoffStatus.COMPLETED);
        h.complete();
    }

    /** BHO-VOID-001 — status → VOIDED, terminally. */
    public void voidHandoff(Handoff h) {
        assertTransition(h.getStatus(), HandoffStatus.VOIDED);
        h.voidHandoff();
    }

    private static void assertTransition(HandoffStatus from, HandoffStatus to) {
        Set<HandoffStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(HandoffStatus.class));
        if (!allowed.contains(to)) {
            throw HandoffException.notOpen(from.name());
        }
    }
}
