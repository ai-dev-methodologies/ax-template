package com.ax.template.authblueprint.idempotency;

import java.util.regex.Pattern;

/**
 * IDEMPOTENCY-KEY-001 — accept an {@code Idempotency-Key} that is an RFC 4122 UUID, a ULID, or a
 * custom string up to 255 chars; reject anything else with 400. The custom-string form is
 * constrained to a safe, unambiguous character set (token characters) so a key cannot smuggle
 * whitespace / control chars / delimiters into the composite store key.
 *
 * <p>Spec: specs/idempotency-l0.yaml#IDEMPOTENCY-KEY-001.
 */
public final class IdempotencyKeyValidator {

    private static final int MAX_LENGTH = 255;
    // token chars: letters, digits, and the URL-safe separators UUID/ULID use. No whitespace/controls.
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._~-]{1,255}");

    private IdempotencyKeyValidator() {}

    public static boolean isValid(String key) {
        return key != null && key.length() <= MAX_LENGTH && VALID.matcher(key).matches();
    }
}
