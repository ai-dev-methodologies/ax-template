package com.ax.template.authblueprint.idempotency;

/**
 * The request body is valid JSON but TRIPS the streaming constraints pinned on
 * {@link RequestFingerprint}'s mapper (token / document / string bound) — an abusive payload that
 * would otherwise force materialization of millions of nodes. Rejected (never accepted with a
 * degraded raw-body hash) and mapped to 413 {@code REQUEST_BODY_TOO_LARGE} by {@link IdempotencyAdvice}.
 */
public class RequestBodyConstraintViolationException extends RuntimeException {
    public static final String CODE = "REQUEST_BODY_TOO_LARGE";

    public RequestBodyConstraintViolationException(Throwable cause) {
        super("The request body exceeds the maximum allowed JSON streaming constraints.", cause);
    }
}
