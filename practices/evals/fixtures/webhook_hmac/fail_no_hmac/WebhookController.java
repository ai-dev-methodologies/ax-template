package com.example.app;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * FIXTURE: FAIL — violates PRACTICES-INTEG-001 (webhook-hmac-required).
 *
 * <p>Processes webhook payload with no HMAC signature verification.
 * Any caller who knows the endpoint URL can inject arbitrary events.
 */
@RestController
public class WebhookController {

    @PostMapping("/api/webhooks/github")
    public ResponseEntity<Void> receiveWebhook(
            @RequestBody String payload) {
        // VIOLATION: no HMAC verification before processing
        processEvent(payload);
        return ResponseEntity.ok().build();
    }

    private void processEvent(String payload) {
        // domain logic here
    }
}
