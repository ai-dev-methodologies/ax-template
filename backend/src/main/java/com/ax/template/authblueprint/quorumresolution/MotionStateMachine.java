package com.ax.template.authblueprint.quorumresolution;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Sole mutator of Motion.status. The ALLOWED transition graph is encoded as an EnumMap
 * so that illegal edges are mechanically rejected (no RESOLVED → OPEN is possible).
 * The motionStateMachine holds no state itself — it is a pure function over the
 * current status and the target status.
 */
final class MotionStateMachine {

    private static final Map<MotionStatus, Set<MotionStatus>> ALLOWED =
        new EnumMap<>(MotionStatus.class);

    static {
        ALLOWED.put(MotionStatus.OPEN, EnumSet.of(MotionStatus.TALLYING));
        ALLOWED.put(MotionStatus.TALLYING, EnumSet.of(MotionStatus.RESOLVED));
        ALLOWED.put(MotionStatus.RESOLVED, EnumSet.noneOf(MotionStatus.class));
    }

    private MotionStateMachine() {}

    /**
     * Validates the transition and applies it to the Motion. Throws QuorumException (409)
     * if the edge is not in the ALLOWED graph.
     */
    static void transition(Motion motion, MotionStatus target) {
        MotionStatus current = motion.getStatus();
        if (!ALLOWED.getOrDefault(current, EnumSet.noneOf(MotionStatus.class)).contains(target)) {
            throw QuorumException.illegalTransition(current, target);
        }
        motion.setStatus(target);
    }
}
