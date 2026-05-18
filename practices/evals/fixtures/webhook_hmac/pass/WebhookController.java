package com.example.app;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * FIXTURE: PASS — satisfies PRACTICES-INTEG-001 (webhook-hmac-required).
 *
 * <p>Verifies HMAC-SHA256 signature before processing any webhook payload.
 * Uses constant-time comparison via MessageDigest.isEqual().
 */
@RestController
public class WebhookController {

    private final WebhookReceiver webhookReceiver;

    public WebhookController(WebhookReceiver webhookReceiver) {
        this.webhookReceiver = webhookReceiver;
    }

    @PostMapping("/api/webhooks/github")
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader("X-Hub-Signature-256") String signatureHeader,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody byte[] rawBody) {

        // Step 1: verify HMAC — constant-time comparison, throws 401 on mismatch
        webhookReceiver.verify(signatureHeader, rawBody);

        // Step 2: idempotency check
        if (webhookReceiver.isDuplicate(deliveryId)) {
            return ResponseEntity.ok().build();
        }

        // Step 3: process and mark
        webhookReceiver.markProcessed(deliveryId);
        processEvent(rawBody);
        return ResponseEntity.ok().build();
    }

    private void processEvent(byte[] rawBody) {
        // domain logic here
    }
}
