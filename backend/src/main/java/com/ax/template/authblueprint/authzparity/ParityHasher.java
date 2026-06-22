package com.ax.template.authblueprint.authzparity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * authorization-parity-l0 canonical parity hash (AUTHZPARITY-ENVELOPE/EXEC-001). The hash is
 * the load-bearing primitive: a SHA-256 over the authorized parameters serialized in a
 * CANONICAL form (keys sorted, each {@code key=value} joined by {@code '\n'}) so the same
 * parameter map always yields the same digest regardless of insertion order. Executed-matches-
 * authorized is then a single structural comparison — a substituted/escalated parameter changes
 * the digest, which the wire cannot fake.
 *
 * <p>The reference canonicalization is deliberately simple (sorted key=value join); a fork-receiver
 * may swap canonical JSON / JCS (RFC 8785) behind this same seam without touching the governance
 * contract (envelope-bind / four-eyes / positive-gates).
 */
final class ParityHasher {

    private ParityHasher() {}

    /** Canonical serialization: keys sorted ascending, {@code key=value} lines joined by '\n'. */
    static String canonicalize(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (!first) {
                sb.append('\n');
            }
            sb.append(e.getKey()).append('=').append(e.getValue() == null ? "" : e.getValue());
            first = false;
        }
        return sb.toString();
    }

    /** Lowercase hex SHA-256 of the canonical serialization of {@code params}. */
    static String hash(Map<String, String> params) {
        byte[] digest = sha256(canonicalize(params).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16));
            hex.append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
