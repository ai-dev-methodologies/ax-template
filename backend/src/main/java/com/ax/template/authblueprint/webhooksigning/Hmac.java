package com.ax.template.authblueprint.webhooksigning;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * WHSIGN-HMAC-001 — HMAC-SHA256 over the canonical {@code signed_input = timestamp + '.' + raw_body}
 * (RFC 2104), plus the constant-time verify primitive (WHSIGN-VERIFY-001).
 *
 * <p>JDK crypto only ({@link javax.crypto.Mac} {@code HmacSHA256} +
 * {@link java.security.MessageDigest#isEqual}). Distinct simple name {@code Hmac} so it does not clash
 * with the OUTBOUND {@code webhook.HmacSigner} bean. Spec: specs/webhook-signing-l0.yaml#WHSIGN-HMAC-001.
 */
final class Hmac {

    private static final String ALGORITHM = "HmacSHA256";

    private Hmac() {}

    /** Canonical signed input: the wire timestamp, a literal '.', then the EXACT raw body bytes. */
    static byte[] signedInput(long timestamp, byte[] rawBody) {
        byte[] prefix = (timestamp + ".").getBytes(StandardCharsets.US_ASCII);
        byte[] out = new byte[prefix.length + rawBody.length];
        System.arraycopy(prefix, 0, out, 0, prefix.length);
        System.arraycopy(rawBody, 0, out, prefix.length, rawBody.length);
        return out;
    }

    /** HMAC-SHA256(secret, signedInput) → raw MAC bytes. */
    static byte[] compute(byte[] secret, byte[] signedInput) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return mac.doFinal(signedInput);
        } catch (GeneralSecurityException e) {
            // HmacSHA256 is a guaranteed JDK algorithm; an init failure here is non-recoverable.
            throw new IllegalStateException("HMAC-SHA256 is unavailable in this JVM", e);
        }
    }

    /**
     * WHSIGN-HMAC-001 / WHSIGN-VERIFY-001 — constant-time comparison via
     * {@link MessageDigest#isEqual(byte[], byte[])}. NEVER {@code String.equals} on the hex (its
     * early-exit on the first mismatched char is a timing oracle).
     */
    static boolean constantTimeEquals(byte[] expected, byte[] actual) {
        return MessageDigest.isEqual(expected, actual);
    }
}
