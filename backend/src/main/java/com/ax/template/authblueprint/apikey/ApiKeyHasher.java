package com.ax.template.authblueprint.apikey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Hashing + plaintext generation for API keys.
 *
 * <p>Trace: KEY-STORAGE-001 — SHA-256 lowercase hex + constant-time comparison
 * via {@link MessageDigest#isEqual(byte[], byte[])} (CWE-208 — timing oracle defense).
 *
 * <p>Plaintext shape: {@code ak_<base64url 43 chars>} (256 bits of entropy from
 * {@link SecureRandom}). The {@code ak_} prefix is a Stripe-style discriminator
 * that lets log redactors find and mask the value.
 */
public final class ApiKeyHasher {

    /** Customer-visible prefix on the plaintext value. */
    public static final String VALUE_PREFIX = "ak_";

    /** Length of the {@code hashPrefix} stored alongside the hash for fast lookup. */
    public static final int HASH_PREFIX_LENGTH = 8;

    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final HexFormat HEX = HexFormat.of();

    private ApiKeyHasher() {}

    /** Generate a fresh plaintext value. 256 bits of entropy, base64url-encoded. */
    public static String newPlaintext() {
        byte[] random = new byte[32];
        RNG.nextBytes(random);
        return VALUE_PREFIX + URL_ENCODER.encodeToString(random);
    }

    /** Extract the {@link #HASH_PREFIX_LENGTH}-char prefix used as a lookup key. */
    public static String prefixOf(String plaintext) {
        if (plaintext == null || plaintext.length() < HASH_PREFIX_LENGTH) {
            throw new IllegalArgumentException("plaintext too short for prefix");
        }
        return plaintext.substring(0, HASH_PREFIX_LENGTH);
    }

    /** SHA-256 lowercase hex digest of the UTF-8 bytes of {@code plaintext}. */
    public static String hash(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("plaintext is required");
        }
        return HEX.formatHex(sha256(plaintext));
    }

    /**
     * Constant-time match. Both sides are reduced to byte arrays of identical length
     * before delegating to {@link MessageDigest#isEqual} so the work is O(hashlen)
     * regardless of where in the string the first divergence is.
     */
    public static boolean matches(String plaintext, String storedHashHex) {
        if (plaintext == null || storedHashHex == null) {
            return false;
        }
        byte[] candidate;
        byte[] expected;
        try {
            candidate = sha256(plaintext);
            expected = HEX.parseHex(storedHashHex);
        } catch (IllegalArgumentException ex) {
            // Stored hex was malformed — treat as no match rather than throwing
            // so the auth filter does not leak a different timing/exception signal.
            return false;
        }
        return MessageDigest.isEqual(candidate, expected);
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
