package com.ax.template.authblueprint.ledgeradmin;

/** Thrown when an admin polls an export-job id that does not exist. */
public class LedgerJobNotFoundException extends RuntimeException {

    public LedgerJobNotFoundException(Long jobId) {
        super("export job not found: " + jobId);
    }
}
