package com.ax.template.authblueprint.scheduledtask;

/**
 * Outcome of one {@link JobHistory} execution row.
 * <p>
 * Trace: SCHED-EXECUTE-001 — every run records SUCCESS / FAILED.
 * SKIPPED_LOCK rows are written when the distributed lock is held by another
 * node, so observability still records the cycle.
 */
public enum JobOutcome {
    SUCCESS,
    FAILED,
    SKIPPED_LOCK
}
