package com.ax.template.authblueprint.idempotency;

/**
 * IDEMPOTENCY-SCOPE-001 — an {@code Idempotency-Key} was sent on an inherently-idempotent method
 * (GET/HEAD/OPTIONS, RFC 7231 §4.2.2), where it is meaningless. Mapped to 400
 * {@code IDEMPOTENCY_KEY_NOT_ALLOWED} by {@link IdempotencyAdvice}.
 */
public class IdempotencyKeyNotAllowedException extends RuntimeException {
    public static final String CODE = "IDEMPOTENCY_KEY_NOT_ALLOWED";

    public IdempotencyKeyNotAllowedException() {
        super("Idempotency-Key is not permitted on inherently-idempotent methods (GET/HEAD/OPTIONS).");
    }
}
