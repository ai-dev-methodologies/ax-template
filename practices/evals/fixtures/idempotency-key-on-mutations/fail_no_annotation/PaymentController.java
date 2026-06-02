/**
 * FIXTURE: idempotency-key-on-mutations/fail_no_annotation
 * Demonstrates WRONG pattern: POST mutation handler with no Idempotency-Key header check.
 * Guard must catch: POST endpoint in payment/notification/email-outbox domain
 * that does not take a required Idempotency-Key @RequestHeader (rejecting null/blank
 * with 400) and dedup via IdempotencyKeyStore — network retries will double-charge.
 * Violates idempotency-key-on-mutations rule.
 */
package com.example.fixture.idempotency_key;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    // VIOLATION: POST mutation that creates a payment has no Idempotency-Key header check.
    // A network retry on this endpoint creates a duplicate charge.
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody CreatePaymentRequest request) {
        // ... process payment ...
        return ResponseEntity.ok(new PaymentResponse("PAY-001"));
    }

    // VIOLATION: notification send also missing annotation
    @PostMapping("/notify")
    public ResponseEntity<Void> sendNotification(@RequestBody NotifyRequest req) {
        // ... send ...
        return ResponseEntity.accepted().build();
    }

    record CreatePaymentRequest(String amount, String currency) {}
    record PaymentResponse(String paymentId) {}
    record NotifyRequest(String userId, String message) {}
}
