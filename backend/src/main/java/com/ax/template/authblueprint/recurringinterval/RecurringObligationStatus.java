package com.ax.template.authblueprint.recurringinterval;

/**
 * completion-reset-recurring-interval-l0 lifecycle. OPEN = the obligation recurs; completing an
 * occurrence advances the window and the obligation stays OPEN on the NEXT window — there is
 * deliberately NO terminal/done state (CRI-RESET-001/CRI-ONCE-001: it never auto-expires and is
 * never finished, it just keeps resetting from each completion). The status is intentionally a
 * single value to make the no-terminal posture explicit and to forbid any "completed" terminal a
 * sweep could write.
 */
public enum RecurringObligationStatus {
    OPEN
}
