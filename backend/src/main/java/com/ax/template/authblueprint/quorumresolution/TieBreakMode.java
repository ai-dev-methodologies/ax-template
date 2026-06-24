package com.ax.template.authblueprint.quorumresolution;

/**
 * Deterministic tie-break policy, frozen at motion-open.
 * TIE_FAILS: a tie produces REJECTED.
 * CHAIR_CASTING: the frozen tie_break_voter_id's ballot choice decides.
 */
public enum TieBreakMode {
    TIE_FAILS,
    CHAIR_CASTING
}
