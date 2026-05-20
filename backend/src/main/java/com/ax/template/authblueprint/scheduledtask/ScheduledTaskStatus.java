package com.ax.template.authblueprint.scheduledtask;

/**
 * Lifecycle state of a registered scheduled task.
 * <p>
 * Trace: SCHED-REGISTER-001 — {@link #REGISTERED} is the initial state.
 */
public enum ScheduledTaskStatus {
    REGISTERED,
    ENABLED,
    DISABLED
}
