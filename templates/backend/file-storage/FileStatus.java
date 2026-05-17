// @ax-template-meta: template_id=backend/file-storage/FileStatus layer=backend domain=file-storage
// evidence: FILE-SCAN-001 (PENDING→READY|QUARANTINED lifecycle), FILE-SCAN-002 (PENDING download 202)
package com.ax.template.authblueprint.filestorage;

/**
 * FileStatus — lifecycle states for an uploaded file.
 *
 * <pre>
 * PENDING      — uploaded; virus scan queued or in progress
 *    ↓ scan clean       ↓ scan infected
 * READY       QUARANTINED
 *    ↓ delete           ↓ cleanup
 * DELETED     DELETED
 * </pre>
 *
 * <p>Download semantics by status:
 * <ul>
 *   <li>PENDING: 202 Accepted + Retry-After (FILE-SCAN-002)</li>
 *   <li>READY: 302 redirect to presigned URL (FILE-SEC-001)</li>
 *   <li>QUARANTINED: 422 Unprocessable Entity (FILE-SCAN-001)</li>
 *   <li>DELETED: 404 Not Found</li>
 * </ul>
 */
public enum FileStatus {

    /** Upload received; virus scan has not yet completed. */
    PENDING,

    /** Virus scan passed; file is available for download. */
    READY,

    /** Virus scan flagged the file as malware; download is blocked. */
    QUARANTINED,

    /** Soft-deleted; excluded from list queries by default. Hard deletion is async. */
    DELETED
}
