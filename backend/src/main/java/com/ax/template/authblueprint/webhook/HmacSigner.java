package com.ax.template.authblueprint.webhook;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;

/**
 * HMAC-SHA256 signer for outbound webhooks.
 * <p>
 * Cryptographic anchor: RFC 2104 (HMAC) + OWASP ASVS V13.2.6.
 * <p>
 * Trace:
 * <ul>
 *   <li>WEBHOOK-SIGN-001 — {@link #sign(String, long, String)} produces the
 *       {@code sha256=<hex>} header value over {@code timestamp.body}.</li>
 *   <li>WEBHOOK-SIGN-002 — signature input is {@code "<timestamp>.<body>"} so
 *       the receiver can reject replays beyond a clock-skew window.</li>
 * </ul>
 *
 * <p><b>Receiver-side verify helper.</b> {@link #verify(String, long, String, String)}
 * is publicly importable so fork-receivers integrating the inbound handler can
 * reuse the same HMAC construction without re-implementing it. See
 * {@code templates/L4/webhook/README.md} for the contract.
 */
@Component
public class HmacSigner {

    /** Header carrying the signature: {@code sha256=<lowercase-hex>}. */
    public static final String HEADER_SIGNATURE = "X-Webhook-Signature";
    /** Header carrying the unix-seconds timestamp. */
    public static final String HEADER_TIMESTAMP = "X-Webhook-Timestamp";
    /** Header carrying the stable delivery_id (UUID). */
    public static final String HEADER_DELIVERY_ID = "X-Webhook-Delivery-Id";

    /** Hex-prefix in the signature header value. */
    public static final String SIGNATURE_PREFIX = "sha256=";

    private static final String HMAC_ALG = "HmacSHA256";
    private static final HexFormat HEX = HexFormat.of();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Produce the {@code sha256=<hex>} header value.
     *
     * @param secret    the per-endpoint signing secret
     * @param timestamp unix seconds since epoch (captured at send time)
     * @param body      raw JSON body bytes will be UTF-8 encoded
     * @return {@code "sha256=" + hex(HMAC-SHA256(secret, timestamp + "." + body))}
     */
    public String sign(String secret, long timestamp, String body) {
        Objects.requireNonNull(secret, "secret");
        Objects.requireNonNull(body, "body");
        String signedInput = timestamp + "." + body;
        return SIGNATURE_PREFIX + HEX.formatHex(hmac(secret, signedInput));
    }

    /**
     * Receiver-side verify helper — constant-time comparison.
     *
     * @return {@code true} iff {@code candidateHeader} matches the expected
     *     {@code sha256=<hex>} for the given timestamp + body + secret
     */
    public boolean verify(String secret, long timestamp, String body, String candidateHeader) {
        if (candidateHeader == null || !candidateHeader.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }
        byte[] expected = hmac(secret, timestamp + "." + body);
        byte[] candidate;
        try {
            candidate = HEX.parseHex(candidateHeader.substring(SIGNATURE_PREFIX.length()));
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return constantTimeEquals(expected, candidate);
    }

    /** Generate a 256-bit base64url-encoded secret. */
    public String generateSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HEX.formatHex(bytes);
    }

    private static byte[] hmac(String secret, String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALG));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("HmacSHA256 not available", ex);
        } catch (java.security.InvalidKeyException ex) {
            throw new IllegalArgumentException("invalid HMAC key", ex);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
