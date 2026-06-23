package com.ax.template.authblueprint.reproducibility;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * reproducible-procedure-l0 canonical input hash (PROC-DRAW-001 / PROC-CLASS-001). SHA-256 hex of
 * the canonical (caller-stable-sorted) input string — the recorded basis that lets a draw or a
 * classification be re-derived and audited. Deterministic: the same input always hashes the same.
 */
final class Hashing {

    private Hashing() {}

    /** SHA-256 hex of {@code value} (UTF-8). 64 lowercase hex chars — the recorded input basis. */
    static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);   // never on a conformant JRE
        }
    }
}
