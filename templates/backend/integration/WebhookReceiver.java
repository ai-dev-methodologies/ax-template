/**
 * @ax-template-meta
 * template_id: backend/integration/WebhookReceiver
 * layer: backend-infrastructure
 * domain: integration
 * anchors_rule: webhook-hmac-required.md (PRACTICES-INTEG-001)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "GitHub Docs — Validating webhook deliveries: use MessageDigest.isEqual() for constant-time comparison to prevent timing attacks; compare sha256= prefix + hex digest"
 *     url: "https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries"
 *   - source_type: external
 *     citation: "OWASP ASVS V13.2.6 — Verify that webhook payloads are verified with an HMAC signature or equivalent mechanism before processing"
 *     url: "https://owasp.org/www-project-application-security-verification-standard/"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Set ax.webhook.secret in application.yml (or Vault/secrets manager).
 *   Call verify(signatureHeader, rawBody) BEFORE any event processing.
 *   Use @RequestBody byte[] (not String or parsed JSON) to preserve HMAC integrity.
 */
package com.example.app.integration;

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
 * Inbound webhook verifier — implements PRACTICES-INTEG-001.
 *
 * <p>Verify HMAC-SHA256 signatures and deduplicate delivery IDs.
 * Uses constant-time comparison to prevent timing attacks.
 */
@Component
public class WebhookReceiver {

    private static final Logger log = LoggerFactory.getLogger(WebhookReceiver.class);

    private static final String HMAC_ALGORITHM   = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    /** Delivery IDs older than this are evicted from the dedup map. */
    static final Duration DEDUP_WINDOW = Duration.ofHours(24);

    private final byte[] secretBytes;
    private final Map<String, Instant> processedDeliveries = new ConcurrentHashMap<>();

    public WebhookReceiver(
            @Value("${ax.webhook.secret}") String webhookSecret) {
        this.secretBytes = webhookSecret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Verifies HMAC-SHA256 signature.
     *
     * @throws ResponseStatusException HTTP 401 on absent, malformed, or invalid signature
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
            log.warn("Webhook rejected: invalid hex in signature");
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
            throw new IllegalStateException("HMAC-SHA256 init failed", ex);
        }
    }

    private void evictExpiredDeliveries() {
        Instant cutoff = Instant.now().minus(DEDUP_WINDOW);
        processedDeliveries.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
    }
}
