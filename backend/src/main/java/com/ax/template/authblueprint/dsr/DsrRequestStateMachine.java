package com.ax.template.authblueprint.dsr;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Sole mutator of {@link DsrRequest#getStatus()} (and {@code closedAt}).
 *
 * <p>Mirrors {@code ExportJobStateMachine} / {@code ApprovalRequestStateMachine}:
 * an illegal edge throws {@link IllegalStateException}, which {@link DsrController}
 * maps to {@code 409}.
 *
 * <p>Allowed transitions:
 * <pre>
 *   RECEIVED    → IN_PROGRESS, CLOSED
 *   IN_PROGRESS → CLOSED
 *   CLOSED      → ∅ (terminal)
 * </pre>
 */
@Component
public class DsrRequestStateMachine {

    private static final Map<DsrRequestStatus, Set<DsrRequestStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(DsrRequestStatus.class);
        ALLOWED.put(DsrRequestStatus.RECEIVED,
            EnumSet.of(DsrRequestStatus.IN_PROGRESS, DsrRequestStatus.CLOSED));
        ALLOWED.put(DsrRequestStatus.IN_PROGRESS,
            EnumSet.of(DsrRequestStatus.CLOSED));
        ALLOWED.put(DsrRequestStatus.CLOSED,
            EnumSet.noneOf(DsrRequestStatus.class));
    }

    private final Clock clock;

    public DsrRequestStateMachine(Clock clock) {
        this.clock = clock;
    }

    /** RECEIVED → IN_PROGRESS. */
    public void markInProgress(DsrRequest req) {
        assertTransition(req.getStatus(), DsrRequestStatus.IN_PROGRESS);
        req.setStatus(DsrRequestStatus.IN_PROGRESS);
    }

    /** RECEIVED|IN_PROGRESS → CLOSED; stamps {@code closedAt}. */
    public void markClosed(DsrRequest req) {
        assertTransition(req.getStatus(), DsrRequestStatus.CLOSED);
        req.setStatus(DsrRequestStatus.CLOSED);
        req.setClosedAt(Instant.now(clock));
    }

    private static void assertTransition(DsrRequestStatus from, DsrRequestStatus to) {
        Set<DsrRequestStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(DsrRequestStatus.class));
        if (!allowed.contains(to)) {
            throw new IllegalStateException(
                "illegal DSR transition: " + from + " → " + to + "; allowed = " + allowed);
        }
    }
}
