package com.ax.template.authblueprint.filestorage;

/**
 * Lifecycle status of a stored file.
 * <p>
 * Trace:
 * <ul>
 *   <li>FILE-SCAN-001 — PENDING → READY (clean) or PENDING → QUARANTINED (infected)</li>
 *   <li>FILE-SCAN-002 — download of PENDING returns 202 + Retry-After</li>
 *   <li>FILE-QUOTA-001 — only {@code PENDING} and {@code READY} count toward quota</li>
 * </ul>
 * Manifest: {@code blueprints/file-storage-manifest.yaml#virus_scan} +
 * {@code #quota.count_statuses}.
 */
public enum FileStatus {
    /** Upload accepted; virus scan not yet completed. */
    PENDING,
    /** Scan complete, file is safe and downloadable. */
    READY,
    /** Scan flagged the file as infected; downloads return 422. */
    QUARANTINED,
    /** Soft-deleted by the owner; excluded from list/get. */
    DELETED
}
