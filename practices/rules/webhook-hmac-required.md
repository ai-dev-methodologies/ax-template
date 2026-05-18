---
title: Inbound webhook endpoints must verify HMAC-SHA256 signatures before processing
impact: HIGH
impactDescription: "Webhook endpoints without signature verification accept forged payloads from any attacker who knows the endpoint URL"
tags:
  - integration
  - security
  - hmac
  - webhook
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-INTEG-001"
verification:
  gradle_task: testIntegration
  tag: INTEGRATION
failing_fixture_path: "practices/evals/fixtures/webhook_hmac/fail_no_hmac"
passing_fixture_path: "practices/evals/fixtures/webhook_hmac/pass"
evidence:
  - source_type: external
    citation: "GitHub Docs — Validating webhook deliveries: use MessageDigest.isEqual() for constant-time comparison to prevent timing attacks; compare sha256= prefix and hex digest"
    url: "https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries"
  - source_type: external
    citation: "OWASP ASVS V13.2.6 — Verify that webhook payloads are verified with an HMAC signature or equivalent mechanism before processing to ensure authenticity and integrity"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
  - source_type: external
    citation: "RFC 2104 — HMAC: Keyed-Hashing for Message Authentication. Section 2: HMAC-SHA256 requires constant-time comparison to prevent timing side channels"
    url: "https://www.rfc-editor.org/rfc/rfc2104"
---

## Inbound webhook endpoints must verify HMAC-SHA256 signatures before processing

**Impact: HIGH — Webhook endpoints without signature verification accept forged payloads from any attacker who knows the endpoint URL**

External webhook providers (GitHub, Stripe, Twilio, etc.) sign each outbound event with an HMAC-SHA256 digest of the raw request body using a shared secret. The receiver must verify this signature **before** deserialising or acting on the payload. Skipping verification allows an attacker to POST any payload — triggering deployments, marking orders as paid, or injecting arbitrary events — without possessing the shared secret.

Critical implementation details:
1. **Raw bytes, not parsed JSON** — use `@RequestBody byte[]`, never `@RequestBody String` or a DTO, because JSON parsers normalise whitespace and key ordering, which alters the byte representation and breaks HMAC verification.
2. **Constant-time comparison** — use `MessageDigest.isEqual(expected, received)`, never `Arrays.equals` or `String.equals`. The latter short-circuit on the first mismatch and leak the valid prefix length to a timing attacker.
3. **`sha256=` prefix** — the industry convention (GitHub, Stripe) is `sha256=<hexdigest>`; verify the prefix before hex-decoding.
4. **Store the secret in Vault / Secrets Manager** — never hardcode in source.

**Incorrect — processes payload without any signature check:**

```java
@PostMapping("/api/webhooks/github")
public ResponseEntity<Void> receiveWebhook(@RequestBody String payload) {
    // VIOLATION: no HMAC verification — any request is accepted
    processEvent(payload);
    return ResponseEntity.ok().build();
}
```

**Correct — constant-time HMAC verification before processing:**

```java
@PostMapping("/api/webhooks/github")
public ResponseEntity<Void> receiveWebhook(
        @RequestHeader("X-Hub-Signature-256") String signatureHeader,
        @RequestBody byte[] rawBody) {

    // Step 1: verify HMAC (throws 401 on failure)
    webhookReceiver.verify(signatureHeader, rawBody);

    // Step 2: idempotency check
    webhookReceiver.markProcessed(deliveryId);

    // Step 3: process
    processEvent(rawBody);
    return ResponseEntity.ok().build();
}
```

See `templates/backend/integration/WebhookReceiver.java` for the reference implementation.

Reference: [GitHub Docs — Validating webhook deliveries](https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries)

Reference: [OWASP ASVS V13.2.6 — Webhook payload verification](https://owasp.org/www-project-application-security-verification-standard/)

Reference: [RFC 2104 — HMAC: Keyed-Hashing for Message Authentication](https://www.rfc-editor.org/rfc/rfc2104)
