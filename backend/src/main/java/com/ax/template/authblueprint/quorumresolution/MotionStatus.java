package com.ax.template.authblueprint.quorumresolution;

/** Life cycle states of a Motion. Transitions: OPEN → TALLYING → RESOLVED (one-way). */
public enum MotionStatus {
    OPEN,
    TALLYING,
    RESOLVED
}
