package com.ax.template.authblueprint.dunning;

/**
 * dunning-collections-l0 one-way dunning ladder (DUNNING-LADDER-001). REMINDER → NOTICE →
 * FINAL_NOTICE → SUSPENDED: additive only — never skip a rung, never reverse, never advance
 * past the terminal SUSPENDED. {@link #next()} is the SINGLE legal forward step.
 */
public enum DunningStage {
    REMINDER,
    NOTICE,
    FINAL_NOTICE,
    SUSPENDED;

    /** The single legal forward step; {@code null} from the terminal SUSPENDED. */
    public DunningStage next() {
        return switch (this) {
            case REMINDER -> NOTICE;
            case NOTICE -> FINAL_NOTICE;
            case FINAL_NOTICE -> SUSPENDED;
            case SUSPENDED -> null;
        };
    }
}
