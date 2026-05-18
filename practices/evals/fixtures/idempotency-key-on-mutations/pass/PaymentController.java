/**
 * FIXTURE: idempotency-key-on-mutations/pass
 * Demonstrates CORRECT pattern: POST mutation handlers annotated with
 * @RequireIdempotencyKey so the filter layer deduplicates retried requests.
 */
package com.example.fixture.idempotency_key;

import com.example.idempotency.RequireIdempotencyKey;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    // CORRECT: @RequireIdempotencyKey triggers the IdempotencyFilter which
    // returns the cached response for duplicate Idempotency-Key headers.
    @RequireIdempotencyKey
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody CreatePaymentRequest request) {
        // ... process payment ...
        return ResponseEntity.ok(new PaymentResponse("PAY-001"));
    }

    @RequireIdempotencyKey
    @PostMapping("/notify")
    public ResponseEntity<Void> sendNotification(@RequestBody NotifyRequest req) {
        // ... send ...
        return ResponseEntity.accepted().build();
    }

    record CreatePaymentRequest(String amount, String currency) {}
    record PaymentResponse(String paymentId) {}
    record NotifyRequest(String userId, String message) {}
}
