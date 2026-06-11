package com.ax.template.authblueprint.obligation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Ordered escalation ladder (OBL-LADDER-001) over the window windowStart→effectiveDeadline:
 * APPROACH at 50% elapsed, IMMINENT at 80%, BREACH at 100% (reference constants). Each rung
 * fires EXACTLY ONCE, in this order, as an appended additive event.
 */
public enum EscalationRung {
    APPROACH(50),
    IMMINENT(80),
    BREACH(100);

    public static final List<EscalationRung> LADDER = List.of(APPROACH, IMMINENT, BREACH);

    private final int percentOfWindow;

    EscalationRung(int percentOfWindow) {
        this.percentOfWindow = percentOfWindow;
    }

    /** The instant this rung becomes due: windowStart + window × percent. */
    public Instant dueAt(Instant windowStart, Instant effectiveDeadline) {
        long windowMillis = Math.max(0, Duration.between(windowStart, effectiveDeadline).toMillis());
        return windowStart.plusMillis(windowMillis * percentOfWindow / 100);
    }
}
