package com.ax.template.authblueprint.secretsmanagement;

import org.springframework.http.HttpStatus;

/**
 * The closed set of secret-handling failures, each carrying the HTTP status + stable {@code code}
 * the spec names. The {@code reason} (when present) feeds {@link SecretMetrics#resolutionFailure}.
 * The message is ALWAYS neutral — it never carries the secret value or the plaintext (NO-LOG-001).
 *
 * <p>Spec: specs/secrets-management-l0.yaml (ACCESS / ROTATION / LIFECYCLE / SOURCE / ENCRYPTION).
 */
public class SecretException extends RuntimeException {

    public enum Kind {
        ACCESS_DENIED(HttpStatus.FORBIDDEN, "SECRET_ACCESS_DENIED", null),
        VERSION_RETIRED(HttpStatus.UNAUTHORIZED, "SECRET_VERSION_RETIRED", "expired"),
        REVOKED(HttpStatus.UNAUTHORIZED, "SECRET_REVOKED", "revoked"),
        EXPIRED(HttpStatus.UNAUTHORIZED, "SECRET_EXPIRED", "expired"),
        NOT_FOUND(HttpStatus.NOT_FOUND, "SECRET_NOT_FOUND", "not_found"),
        NON_TLS(HttpStatus.BAD_REQUEST, "SECRET_TLS_REQUIRED", null),
        LITERAL_IN_CONFIG(HttpStatus.INTERNAL_SERVER_ERROR, "SECRET_CONFIG_LITERAL", null);

        final HttpStatus status;
        final String code;
        /** Mapped {@link SecretMetrics} resolution-failure reason, or null when not a resolution failure. */
        final String metricReason;

        Kind(HttpStatus status, String code, String metricReason) {
            this.status = status;
            this.code = code;
            this.metricReason = metricReason;
        }
    }

    private final transient Kind kind;

    public SecretException(Kind kind, String neutralMessage) {
        super(neutralMessage);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
