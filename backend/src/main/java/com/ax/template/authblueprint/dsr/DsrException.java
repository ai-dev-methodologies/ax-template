package com.ax.template.authblueprint.dsr;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for the data-subject-rights surface. Each instance carries the
 * stable RFC 9457 {@code code} + the wire {@link HttpStatus} so
 * {@link DsrController}'s single {@code @ExceptionHandler} maps it to a
 * {@code ProblemDetail} without a per-code switch.
 *
 * <p>Codes (specs/data-subject-rights-l0.yaml):
 * <ul>
 *   <li>{@code DSR_ACCESS_IN_FLIGHT} → 409 (DSR-ACCESS-001)</li>
 *   <li>{@code DSR_FIELD_NOT_RECTIFIABLE} → 422 (DSR-RECTIFY-001)</li>
 *   <li>{@code DSR_RECTIFY_STALE} → 409 (DSR-RECTIFY-001)</li>
 *   <li>{@code DSR_PORTABILITY_FORMAT_INVALID} → 400 (DSR-PORTABILITY-001)</li>
 *   <li>{@code DSR_PROCESSING_RESTRICTED} → 423 (DSR-RESTRICT-001)</li>
 * </ul>
 */
public class DsrException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    private DsrException(HttpStatus status, String code, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }

    public static DsrException accessInFlight() {
        return new DsrException(HttpStatus.CONFLICT, "DSR_ACCESS_IN_FLIGHT",
            "A subject-access request is already in flight for this subject.");
    }

    public static DsrException fieldNotRectifiable(String fieldPath) {
        return new DsrException(HttpStatus.UNPROCESSABLE_ENTITY, "DSR_FIELD_NOT_RECTIFIABLE",
            "Field is not subject-rectifiable: " + fieldPath);
    }

    public static DsrException rectifyStale() {
        return new DsrException(HttpStatus.CONFLICT, "DSR_RECTIFY_STALE",
            "Supplied current_value does not match the stored value; re-read and retry.");
    }

    public static DsrException portabilityFormatInvalid(String format) {
        return new DsrException(HttpStatus.BAD_REQUEST, "DSR_PORTABILITY_FORMAT_INVALID",
            "Unsupported portability format: " + format + " (supported: json, csv).");
    }

    public static DsrException processingRestricted() {
        return new DsrException(HttpStatus.LOCKED, "DSR_PROCESSING_RESTRICTED",
            "Processing is restricted for this subject; the operation is blocked until restriction is lifted.");
    }
}
