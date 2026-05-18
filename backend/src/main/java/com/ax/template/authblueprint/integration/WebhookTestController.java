package com.ax.template.authblueprint.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test webhook endpoint for WebhookReceiverIT.
 *
 * <p>This controller exists solely to provide a testable endpoint for
 * {@code WebhookReceiverIT}. In production, fork receivers replace this
 * with their domain-specific webhook controllers.
 *
 * <p>Pattern: implements PRACTICES-INTEG-001 (webhook-hmac-required):
 * <ol>
 *   <li>Verify HMAC signature via {@link WebhookReceiver#verify}.
 *   <li>Check idempotency key before processing.
 *   <li>Mark delivery processed after handling.
 * </ol>
 */
@RestController
@RequestMapping("/api/test")
public class WebhookTestController {

    private static final Logger log = LoggerFactory.getLogger(WebhookTestController.class);

    private final WebhookReceiver webhookReceiver;

    public WebhookTestController(WebhookReceiver webhookReceiver) {
        this.webhookReceiver = webhookReceiver;
    }

    /**
     * Test webhook endpoint.
     *
     * <p>Uses {@code @RequestBody byte[]} to receive raw bytes — required for
     * correct HMAC computation (deserialised objects change byte representation).
     */
    @PostMapping("/webhooks")
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody byte[] rawBody) {

        // Step 1: Verify HMAC (throws 401 on failure — implements PRACTICES-INTEG-001)
        webhookReceiver.verify(signatureHeader, rawBody);

        // Step 2: Idempotency check
        String effectiveDeliveryId = deliveryId != null ? deliveryId : "no-delivery-id";
        if (webhookReceiver.isDuplicate(effectiveDeliveryId)) {
            log.debug("Duplicate webhook delivery ignored: id={}", effectiveDeliveryId);
            return ResponseEntity.ok().build();
        }

        // Step 3: Process event (placeholder — replace with domain logic)
        webhookReceiver.markProcessed(effectiveDeliveryId);
        log.info("Webhook processed: deliveryId={} size={}", effectiveDeliveryId, rawBody.length);

        return ResponseEntity.ok().build();
    }
}
