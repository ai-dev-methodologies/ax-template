package com.ax.template.authblueprint.filestorage;

/**
 * Outcome of a virus scan. Maps directly to a terminal {@link FileStatus}.
 * <p>
 * Trace: FILE-SCAN-001 — only CLEAN allows transition to READY; INFECTED moves
 * the file to QUARANTINED.
 */
public enum FileScanResult {
    CLEAN,
    INFECTED
}
