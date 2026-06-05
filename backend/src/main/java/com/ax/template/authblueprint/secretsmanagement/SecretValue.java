package com.ax.template.authblueprint.secretsmanagement;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;

/**
 * SECRET-NO-LOG-001 keystone — a wrapper around a plaintext secret whose {@link #toString()} and
 * JSON serialization emit a CONSTANT mask, so an accidental string interpolation, a log line, an
 * exception message, or a serialized response can never leak the underlying value.
 *
 * <p>The plaintext is reachable ONLY through the explicit {@link #reveal()} call (used by the
 * crypto + verification paths in-memory), which names the leak risk at the call site. Every other
 * egress path — {@code log.info("secret={}", secretValue)}, {@code "..." + secretValue},
 * {@code objectMapper.writeValueAsString(secretValue)} — yields {@link #MASK}.
 *
 * <p>Spec: specs/secrets-management-l0.yaml#SECRET-NO-LOG-001.
 */
@JsonSerialize(using = SecretValueSerializer.class)
public final class SecretValue {

    /** The single masked form every non-reveal egress path emits. */
    public static final String MASK = "****";

    private final String plaintext;

    private SecretValue(String plaintext) {
        this.plaintext = Objects.requireNonNull(plaintext, "plaintext");
    }

    public static SecretValue of(String plaintext) {
        return new SecretValue(plaintext);
    }

    /**
     * Reveal the plaintext. The ONLY method that returns it — deliberately named so every leak-risk
     * site is greppable. Callers are the crypto seal/open + the rotation/verification comparison.
     */
    public String reveal() {
        return plaintext;
    }

    /** SECRET-NO-LOG-001 — string interpolation / log args / stack traces get the mask, never the value. */
    @Override
    public String toString() {
        return MASK;
    }

    /** Equality is over the plaintext (so a presented credential can be compared) but NEVER prints it. */
    @Override
    public boolean equals(Object o) {
        return o instanceof SecretValue other && plaintext.equals(other.plaintext);
    }

    @Override
    public int hashCode() {
        return plaintext.hashCode();
    }
}
