package com.ax.template.authblueprint.ledgeradmin;

/** Lifecycle of an enqueued audit-export job (report-export L4 pattern, thinned). */
public enum ExportStatus {
    QUEUED,
    RUNNING,
    DONE,
    FAILED
}
