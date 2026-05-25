package com.ax.template.authblueprint.identityverification;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for identity-verification failures.
 *
 * <p>R54 — surfaces as RFC 7807 ProblemDetail at controller boundary.
 * IDV-PROVIDER-002: unknown provider → 400.
 * IDV-CALLBACK-001: invalid HMAC → 401.
 * EXTRACTION_FAIL (per IDV-AUDIT-001): 422.
 */
public class IdentityVerificationException extends RuntimeException {

    public enum Reason {
        UNKNOWN_PROVIDER(HttpStatus.BAD_REQUEST),
        HMAC_FAIL(HttpStatus.UNAUTHORIZED),
        EXTRACTION_FAIL(HttpStatus.UNPROCESSABLE_ENTITY);

        private final HttpStatus status;

        Reason(HttpStatus status) { this.status = status; }

        public HttpStatus status() { return status; }
    }

    private final Reason reason;

    public IdentityVerificationException(Reason reason, String detail) {
        super(detail);
        this.reason = reason;
    }

    public IdentityVerificationException(Reason reason, String detail, Throwable cause) {
        super(detail, cause);
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
