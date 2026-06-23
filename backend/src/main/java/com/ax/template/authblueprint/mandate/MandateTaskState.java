package com.ax.template.authblueprint.mandate;

/**
 * mandate-fanout-l0 child-task lifecycle (MANDATE-FANOUT/DEEMED-001). A task starts PENDING and
 * reaches a TERMINAL state EXACTLY ONCE: DONE / DECLINED via an explicit response, or DEEMED via
 * the deemed-default sweep on silence past the deadline (MANDATE-DEEMED-001). The mandate's
 * completion is the DERIVED recall over these terminal states — never a stored flag.
 */
public enum MandateTaskState {
    PENDING,
    DONE,
    DECLINED,
    DEEMED;

    /** Whether this state is terminal — counted by the conserved completion recall (Σ terminal == N). */
    public boolean isTerminal() {
        return this != PENDING;
    }
}
