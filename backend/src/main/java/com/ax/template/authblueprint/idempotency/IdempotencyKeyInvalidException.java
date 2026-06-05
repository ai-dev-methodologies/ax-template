package com.ax.template.authblueprint.idempotency;

/**
 * IDEMPOTENCY-KEY-001 — the supplied {@code Idempotency-Key} is not a valid UUID / ULID / token
 * string ≤255 chars. Mapped to 400 {@code IDEMPOTENCY_KEY_INVALID} by {@link IdempotencyAdvice}.
 */
public class IdempotencyKeyInvalidException extends RuntimeException {
    public static final String CODE = "IDEMPOTENCY_KEY_INVALID";

    public IdempotencyKeyInvalidException() {
        super("Idempotency-Key is not a valid UUID, ULID, or token string (max 255 chars).");
    }
}
