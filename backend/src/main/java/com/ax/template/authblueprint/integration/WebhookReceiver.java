package com.ax.template.authblueprint.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inbound webhook verifier.
 *
 * <p>Implements PRACTICES-INTEG-001: every webhook endpoint must verify the
 * HMAC-SHA256 signature before processing the event.
 *
 * <p>Verification uses constant-time comparison ({@link MessageDigest#isEqual})
 * to prevent timing attacks. The shared secret is loaded from
 * {@code ax.webhook.secret} — store it in Vault or secrets manager.
 *
 * @see com.ax.template.authblueprint.integration.WebhookTestController
 */
@Component
public class WebhookReceiver {

    private static final Logger log = LoggerFactory.getLogger(WebhookReceiver.class);

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    static final Duration DEDUP_WINDOW = Duration.ofHours(24);

    private final byte[] secretBytes;
    private final Map<String, Instant> processedDeliveries = new ConcurrentHashMap<>();

    public WebhookReceiver(
            @Value("${ax.webhook.secret:ax-template-dev-secret}") String webhookSecret) {
        this.secretBytes = webhookSecret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Verifies that the inbound signature matches HMAC-SHA256(rawBody, secret).
     *
     * @throws ResponseStatusException 401 when signature is absent, malformed, or invalid
     */
    public void verify(String signatureHeader, byte[] rawBody) {
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            log.warn("Webhook rejected: missing or malformed signature header");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing HMAC signature");
        }

        String receivedHex = signatureHeader.substring(SIGNATURE_PREFIX.length());
        byte[] receivedDigest;
        try {
            receivedDigest = HexFormat.of().parseHex(receivedHex);
        } catch (IllegalArgumentException ex) {
            log.warn("Webhook rejected: invalid hex encoding in signature");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid signature encoding");
        }

        byte[] expectedDigest = computeHmac(rawBody);

        if (!MessageDigest.isEqual(expectedDigest, receivedDigest)) {
            log.warn("Webhook rejected: HMAC mismatch");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid signature");
        }
    }

    public boolean isDuplicate(String deliveryId) {
        evictExpiredDeliveries();
        return processedDeliveries.containsKey(deliveryId);
    }

    public void markProcessed(String deliveryId) {
        processedDeliveries.put(deliveryId, Instant.now());
    }

    private byte[] computeHmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC-SHA256 initialisation failed", ex);
        }
    }

    private void evictExpiredDeliveries() {
        Instant cutoff = Instant.now().minus(DEDUP_WINDOW);
        processedDeliveries.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
    }
}
