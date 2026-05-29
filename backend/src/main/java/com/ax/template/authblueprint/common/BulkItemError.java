package com.ax.template.authblueprint.common;

/**
 * RFC 9457-aligned per-item error for a bulk operation — ships REAL reusable code
 * for the {@code bulk-operation-l0} catalog spec (specs/bulk-operation-l0.yaml),
 * specifically the per-item {@code error} object mandated by BULK-PARTIAL-001.
 *
 * <p>The spec requires that, on a per-item failure inside a 207 Multi-Status
 * response, each result carry "an embedded RFC 9457 {@code error} object
 * ({@code type}, {@code title}, {@code detail})". The IDW2 dogfood found all three
 * personas hand-rolled this same two-field shape inline, drifting on member names
 * (some used {@code code}/{@code reason}, others {@code error}/{@code message}).
 * This record fixes one canonical shape so the per-item error in every bulk
 * endpoint is byte-comparable.
 *
 * <p>Minimal-by-design: it carries {@code code} (a stable machine-readable token,
 * mapping to the RFC 9457 {@code type} URI tail) and {@code message} (the
 * human-readable {@code detail}). Callers that need the full RFC 9457 envelope
 * (a quoted {@code type} URI, {@code title}, {@code status}) map this two-field
 * core into a {@link org.springframework.http.ProblemDetail} at the controller
 * boundary — see {@link GlobalProblemDetailAdvice}. Keeping the per-<em>item</em>
 * error two-field avoids repeating the batch-level {@code status}/{@code type} on
 * every one of up to {@code bulk_max_batch_size} entries.
 *
 * <p>Framework-clean: no Spring, no JPA — a pure value record so the result model
 * can be unit-tested without a web context.
 *
 * @param code    stable machine-readable error token (e.g. {@code "VALIDATION_FAILED"});
 *                must be non-null/non-blank so a client can branch on it
 * @param message human-readable detail of what failed for this item
 */
public record BulkItemError(String code, String message) {

    public BulkItemError {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("BulkItemError.code must be non-blank");
        }
    }
}
