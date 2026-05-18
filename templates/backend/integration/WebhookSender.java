/**
 * @ax-template-meta
 * template_id: backend/integration/WebhookSender
 * layer: backend-infrastructure
 * domain: integration
 * anchors_rule: webhook-hmac-required.md (PRACTICES-INTEG-001)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Transactional Outbox Pattern (microservices.io) — persist the webhook event in the same transaction as the domain change; a background worker delivers it; guarantees at-least-once delivery without a message broker"
 *     url: "https://microservices.io/patterns/data/transactional-outbox.html"
 *   - source_type: external
 *     citation: "GitHub Docs — Validating webhook deliveries: sign each outbound payload with HMAC-SHA256 and include the sha256= prefix so receivers can verify authenticity"
 *     url: "https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Call enqueue() inside the same @Transactional boundary as the domain write.
 *   processQueue() runs @Scheduled — adjust fixedDelay as needed.
 *   Implement WebhookOutboxRepository backed by a webhook_outbox table.
 */
package com.example.app.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Outbound webhook sender using the Transactional Outbox pattern.
 *
 * <p>Mirrors email-outbox: {@link #enqueue} persists a PENDING outbox row in
 * the caller's transaction; {@link #processQueue} delivers pending rows with
 * HMAC-SHA256 signing, exponential back-off, and MAX_RETRIES cap.
 */
@Component
public class WebhookSender {

    private static final Logger log = LoggerFactory.getLogger(WebhookSender.class);

    private static final String HMAC_ALGORITHM   = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final int    MAX_RETRIES      = 3;
    /** Exponential back-off: 2^retryCount × BASE_DELAY_MS. */
    private static final long   BASE_DELAY_MS    = 30_000L;

    private final RestClient restClient;
    private final WebhookOutboxRepository outboxRepository;
    private final byte[] secretBytes;

    public WebhookSender(RestClient integrationRestClient,
                         WebhookOutboxRepository outboxRepository,
                         @Value("${ax.webhook.secret}") String webhookSecret) {
        this.restClient        = integrationRestClient;
        this.outboxRepository  = outboxRepository;
        this.secretBytes       = webhookSecret.getBytes(StandardCharsets.UTF_8);
    }

    // ── Enqueue (call inside domain transaction) ───────────────────────────────

    /**
     * Persists a webhook event in the outbox — no HTTP call yet.
     * Call inside an active {@code @Transactional} boundary.
     */
    @Transactional
    public void enqueue(String targetUrl, String eventType, String payload) {
        WebhookOutbox entry = new WebhookOutbox(targetUrl, eventType, payload);
        outboxRepository.save(entry);
        log.debug("Webhook enqueued: event={} url={}", eventType, targetUrl);
    }

    // ── Delivery loop ──────────────────────────────────────────────────────────

    @Scheduled(fixedDelay = 5_000)
    @Transactional
    public void processQueue() {
        List<WebhookOutbox> pending = outboxRepository.findPendingWithRetryBudget(MAX_RETRIES);
        for (WebhookOutbox entry : pending) {
            deliver(entry);
        }
    }

    // ── Delivery internals ─────────────────────────────────────────────────────

    private void deliver(WebhookOutbox entry) {
        String signature = SIGNATURE_PREFIX + sign(entry.payload());
        try {
            restClient.post()
                    .uri(entry.targetUrl())
                    .header("X-Hub-Signature-256", signature)
                    .body(entry.payload())
                    .retrieve()
                    .toBodilessEntity();

            outboxRepository.markDone(entry.id());
            log.info("Webhook delivered: id={} event={}", entry.id(), entry.eventType());
        } catch (Exception ex) {
            int nextRetry = entry.retryCount() + 1;
            if (nextRetry >= MAX_RETRIES) {
                outboxRepository.markFailed(entry.id());
                log.error("Webhook permanently failed after {} retries: id={}", MAX_RETRIES, entry.id());
            } else {
                long delayMs = (long) Math.pow(2, nextRetry) * BASE_DELAY_MS;
                outboxRepository.scheduleRetry(entry.id(), nextRetry, delayMs);
                log.warn("Webhook delivery failed (retry {} in {}ms): id={}", nextRetry, delayMs, entry.id());
            }
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC-SHA256 init failed", ex);
        }
    }
}
